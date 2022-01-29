package learn.spring.fssp.scraper.core.proxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import learn.spring.fssp.scraper.core.dao.Proxy;
import learn.spring.fssp.scraper.core.dao.ProxyRepository;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class ProxyLoader {

    private WebClient client ;

    private ProxyListProvider proxyProvider;

    @Autowired
    ProxyRepository proxyRepository;

    @Autowired
    public void setProxyProvider(ProxyListProvider proxyProvider) {
        this.proxyProvider = proxyProvider;
        client = WebClient.create();
    }

    final static AtomicBoolean proxyMutex = new AtomicBoolean(false);

    public void loadProxies(){
        if (proxyRepository.countByEnabledTrue() > 1 || !proxyMutex.compareAndSet(false, true)) return;
        proxies().subscribe(list->{
            log.debug("proxy list size is {}", list.size());
            if (!list.isEmpty()){
                proxyRepository.saveAll(list);
            }
            proxyMutex.set(false);
        });
    }

    public Mono<List<Proxy>> proxies(){
        return Mono.just("")
                .flatMap(mono->{
                    if (proxyProvider.providerUrl() == null){
                        log.debug("proxy url is null");
                        return Mono.just("null");
                    }
                    log.debug("loading proxy list from {}", proxyProvider.providerUrl());
                    return client
                            .get().uri(proxyProvider.providerUrl())
                            .retrieve()
                            .bodyToMono(String.class)
                            .doOnError(fault->log.warn("cannot load proxy", fault))
                            .onErrorReturn("");
                            //.onErrorStop();
                })
                .map(answer->{
                    log.debug("proxy list loaded");
                    List<Proxy> result = Collections.emptyList();
                    if (StringUtils.hasText(answer)) {
                        result = proxyProvider.parseAnswer(answer);
                    }
                    return result;
                })
                .doOnError(fault->log.warn("cannot parse answer", fault))
                .onErrorReturn(Collections.emptyList());
//        String htmlData = client.get().retrieve().bodyToMono(String.class).block();
    }


}
