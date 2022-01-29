package learn.spring.fssp.scraper.core.process;

import learn.spring.fssp.scraper.core.dao.*;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import learn.spring.fssp.scraper.captcha.CaptchaUtils;
import learn.spring.fssp.scraper.core.FsspResponseParser;
import learn.spring.fssp.scraper.core.proxy.ClientPool;
import learn.spring.fssp.scraper.core.proxy.ClientWrapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static learn.spring.fssp.scraper.core.FsspResponseParser.ResponseType.*;

@Slf4j
public class Processor {

    @Value("${save.failure.images}")
    Boolean saveFailureImages;

    @Value("${path.failure.images}")
    String pathFailureImages;

    @Value("${save.successful.images}")
    Boolean saveSuccessfulImages;

    @Value("${path.successful.images}")
    String pathSuccessfulImages;

    @Value("${banned.time}")
    Integer proxyBannedTime;

    @Autowired @Lazy
    Processor self;

    @Autowired
    ClientPool clientPool;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    ProxyRepository proxyRepository;

    @Autowired
    ProductionRepository productionRepository;

    public Flux<Job> processWithClient(List<Task> tasks){
        taskRepository.signCalledByIds(tasks);
        return Flux.fromIterable(tasks)
                .map(Job::new)
                .flatMap(this::processWithoutClient)
                .map(job->{
                    clientPool.returnObject(job.getClient());
                    return job;
                });
    }

    @SneakyThrows
    Job borrowClient(Job job){
        taskRepository.signCalledById(job.getTaskId());
        job.setClient(clientPool.borrowObject());
        return job;
    }

    Mono<Job> processWithoutClient(Job job){
        return Mono.just(Boolean.TRUE)
                .map(obj->borrowClient(job))
                .retryWhen(
                        Retry.fixedDelay(Long.MAX_VALUE, Duration.ofMillis(1000))
                                .doBeforeRetry(signal-> log.warn("error #{} " + signal.failure(), signal.totalRetries()))
                )
                .flatMap(this::processWithClient);
    }

    Mono<Job> processWithClient(Job job){
        return callToFssp(job).flatMap(job::processAnswer);
    }

    Mono<Job> invalidateClientAndProcess(Job job){
        return processWithoutClient(invalidateClientInPool(job));
    }

    Job invalidateClientInPool(Job job){
        clientPool.invalidateObject(job.getClient());
        job.setClient(null);
        return job;
    }

    Mono<String> callToFssp(final Job job){
        taskRepository.signCalledById(job.getTaskId());
        String url = job.getBaseUrl();
        if (job.getNextPage() != null) url += "&page=" + job.getNextPage();
        if (job.getCaptcha()!=null) url += "&code=" + job.getCaptcha();
        log.debug("task {} use proxy {} for calling {}", job.getTaskId(), job.getClient().getProxy(), url);
        Mono<String> result = job.getClient().getClient()
                .get().uri(url)
//                .headers(httpHeaders -> {
//                    log.debug("request headers is {}", httpHeaders);
//                })
                .cookies(cookies->{
                    if (job.getCookies()!=null) {
                        //load cookies from client
                        job.getCookies().forEach((name, value)->{
                            cookies.add(name, value.getValue());
                        });
//                        log.debug("cookies before request {}", cookies);
                    }
                })
                .exchangeToMono(response -> {
                    log.debug("response headers is {}", response.headers().asHttpHeaders());
                    if (!response.cookies().isEmpty()){
                        //save cookies
                        job.setCookies(response.cookies().toSingleValueMap());
                        log.debug("cookies from response {}", job.getCookies());
                    }
                    return response.bodyToMono(String.class);
                });
                if (job.getClient().getProxy().getCount() > 0)
                {
                    //If not first call its not a trashed proxy, let it chance
                    result = result.retryWhen(
                                    Retry.fixedDelay(2, Duration.ofMillis(1000))
                                            .doBeforeRetry(signal-> log.warn(" task {} error #{} " + signal.failure(), job.getTaskId(), signal.totalRetries()))
                            );
                }
                return result.onErrorResume(throwable -> {
                    //Change client and call again
                    log.info("task {} choose another client " + throwable, job.getTaskId());
                    return Mono.just(borrowClient(invalidateClientInPool(job)))
                            .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofMillis(1000)))
                            .flatMap(this::callToFssp);
                });
    }

    @Transactional
    public Task addToTask(@NonNull String id, Collection<Production> productions){
        Task out = taskRepository.findById(id).get();
        productions.forEach(production -> {
            production.setTask(out);
            productionRepository.save(production);
        });
        out.getProductions().addAll(productions);
        taskRepository.save(out);
        return out;
    }

    public static BufferedImage imageFromStrB64(String str) throws IOException {
        try(var bais = new ByteArrayInputStream(java.util.Base64.getDecoder().decode(str))){
            return ImageIO.read(bais);
        }
    }

    @Getter
    @Setter
    public class Job {
        String taskId;
        ClientWrapper client;
        int captchaCount = 0;
        String captcha;
        BufferedImage captchaImage;
        Map<String, ResponseCookie> cookies;
        String baseUrl;
        Integer nextPage;

        public Job(Task task){
            setTaskId(task.getId());
            setNextPage(task.getNextPage());
            setBaseUrl(PathConstructor.construct(task));
        }

        @SneakyThrows
        public Mono<Job> processAnswer(String answer){
            refreshProxy();//successful response - sign proxy as working
            log.debug("task {} received response {}", getTaskId(), answer);
            FsspResponseParser fsspResponseParser = new FsspResponseParser(answer);
            if (fsspResponseParser.getType() == BLOCKED_RESPONSE){
                banProxy();
                return invalidateClientAndProcess(Job.this);
            }
            if (fsspResponseParser.getType() == CAPTCHA_RESPONSE){
                log.debug("task {} is capture response", getTaskId());
                saveFailureImage();
                recognizeCaptcha(fsspResponseParser.getCaptchaBase64());
                return processWithClient(Job.this);//repeat processing with new capctha's value
            }
            saveSuccessfulImage();
            resetCaptcha();

            if (fsspResponseParser.getType() == EMPTY_RESPONSE){
                setNextPage(null);
                taskRepository.signCompletedById(getTaskId());
            }
            else if (fsspResponseParser.getType() == NORMAL_RESPONSE){
                if (fsspResponseParser.getData().isEmpty()) {
                    log.warn("Normal response for {}, but empty results", getTaskId());
                    setNextPage(null);
                    taskRepository.signCompletedById(getTaskId());
                }
                else {
                    Task task = self.addToTask(getTaskId(), fsspResponseParser.getData());
                    task.setNextPage(fsspResponseParser.getNextPage());
                    setNextPage(fsspResponseParser.getNextPage());
                    taskRepository.save(task);
                }
            }
            if (fsspResponseParser.getNextPage() != null)  return processWithClient(Job.this);//repeat processing for next page
            taskRepository.signCompletedById(getTaskId());
            return Mono.just(Job.this);
        }


        void banProxy(){
            getClient().getProxy().setBanned(new Date(System.currentTimeMillis() + proxyBannedTime));
            proxyRepository.save(getClient().getProxy());
            log.debug("{} banned", getClient());
        }

        void saveFailureImage() throws IOException {
            if (getCaptchaImage()!=null && saveFailureImages){
                //Image already set, so previous image recognize failed
                String failuredImagePath = pathFailureImages + getCaptcha() + ".png";
                ImageIO.write(getCaptchaImage(), "png", new File(failuredImagePath));
                setCaptchaCount(getCaptchaCount() + 1);
                log.debug("save failured image#{} as {}", getCaptchaCount(), failuredImagePath);
            }
        }

        void saveSuccessfulImage() throws IOException {
            if (getCaptchaImage()!=null && saveSuccessfulImages){
                String successfulImagePath = pathSuccessfulImages + getCaptcha() + ".png";
                ImageIO.write(getCaptchaImage(), "png", new File(successfulImagePath));
                setCaptchaCount(getCaptchaCount() + 1);
                log.debug("save successful image#{} as {}", getCaptchaCount(), successfulImagePath);
            }
        }

        void recognizeCaptcha(String captcha) throws IOException {
            BufferedImage image = imageFromStrB64(captcha);
            setCaptcha(CaptchaUtils.recognizeCaptcha(image));
            setCaptchaImage(image);
            log.debug("Captcha recognize {}", getCaptcha());
        }

        void resetCaptcha(){
            setCaptcha(null);
            setCaptchaImage(null);
            setCaptchaCount(0);
        }

        void refreshProxy(){
            getClient().getProxy().setBanned(null);
            getClient().getProxy().setUsed(new Date());
            getClient().getProxy().setCount(getClient().getProxy().getCount() + 1);
            proxyRepository.save(getClient().getProxy());
        }
    }

}
