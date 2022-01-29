package learn.spring.fssp.scraper.core;

import learn.spring.fssp.scraper.core.dao.Task;
import learn.spring.fssp.scraper.core.dao.TaskRepository;
import learn.spring.fssp.scraper.core.process.Processor;
import learn.spring.fssp.scraper.core.proxy.ProxyLoader;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static learn.spring.fssp.scraper.core.Consts.SCHEDULER_DELAY;

@Service
@EnableScheduling
@Slf4j
@Setter
public class Scheduler {

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    ProxyLoader proxyLoader;

//    FluxSink<Task> taskSink;
//    Flux<Task> bridge = Flux.create(sink->setTaskSink(sink));

    @Autowired
    Processor processor;

//    Disposable disposable;
//
//    Disposable subscribe() {
//        return bridge.publish(x->processor.mapper(x)).subscribe(s->log.info("subscribed"));
//    }

    @Scheduled(fixedDelayString = SCHEDULER_DELAY,
    initialDelay = 1000)
    public void scheduled() throws Exception{
        log.trace("start scheduling");
        List<Task> tasks = taskRepository.findForProcessing(new Date(System.currentTimeMillis() - 30000L));
        if (tasks.isEmpty()) {
            log.trace("There are not task for processing");
            return;
        }
//        if (disposable == null) disposable = subscribe();
//        tasks.forEach(taskSink::next);
        processor.processWithClient(tasks)
                .subscribe(job->{
                    log.info("Processing completed for task {} with client {}", job.getTaskId(), job.getClient());
                });
        if (tasks.size() == 1) log.info("add task {} to flux - called {}", tasks.get(0).getId(), tasks.get(0).getCalled());
        else log.info("add {} tasks to flux", tasks.size());
    }


    @Scheduled(fixedDelay = 10000, initialDelay = 1000)
    public void scheduledProxy(){
        log.trace("scheduledProxy");
        proxyLoader.loadProxies();
    }

}
