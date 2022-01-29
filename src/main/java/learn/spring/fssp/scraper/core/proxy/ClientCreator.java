package learn.spring.fssp.scraper.core.proxy;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;
import learn.spring.fssp.scraper.core.dao.Proxy;
import learn.spring.fssp.scraper.core.dao.ProxyRepository;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClientCreator {

    @Value("${connect.timeout}")
    Integer connectTimeout;
    @Value("${socket.timeout}")
    Integer socketTimeout;

    @Autowired
    ProxyRepository proxyRepository;

//    @Lazy @Autowired
//    private ClientCreator self;

    @SneakyThrows
    public ClientWrapper create() {
        var proxyOptional = getProxy();
        Proxy proxy = proxyOptional.orElseThrow(()->new RuntimeException("All proxies disabled"));
        log.debug("creating client for {}", proxy);

        var sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

        HttpClient httpClient = HttpClient.create()
                .secure(t->t.sslContext(sslContext))
                .proxy(options -> options.type(ProxyProvider.Proxy.HTTP)
                        .host(proxy.getHost())
                        .port(proxy.getPort()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
//                .option(ChannelOption.SO_TIMEOUT, socketTimeout)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(socketTimeout, TimeUnit.MILLISECONDS));
                    connection.addHandlerLast(new WriteTimeoutHandler(socketTimeout, TimeUnit.MILLISECONDS));
                })
                .doOnRequest((request, connection) -> {
                    log.debug("on request cookies is {}", request.cookies());
                })
                .doOnResponse((response, connection) -> {
                    log.debug("on response cookies is {}", response.cookies());
                })
                ;

        WebClient client = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        log.info("created client for {}", proxy);
        return new ClientWrapper(client, proxy);
    }

    //@Transactional
    Optional<Proxy> getProxy(){
        var proxy = proxyRepository.findTopByEnabledTrueOrderByUsedAsc();
        proxy.ifPresent(this::updateProxy);
        return proxy;
    }

    public void disableProxy(Proxy proxy){
        proxy.setEnabled(false);
        proxyRepository.save(proxy);
        log.info("{} disabled", proxy);
    }

//    @Transactional
//    public void disableProxy(String host, int port){
//        disableProxy(proxyRepository.findByHostAndPort(host, port));
//    }

    void updateProxy(Proxy proxy){
        proxy.setUsed(new Date());
        proxyRepository.save(proxy);
    }
}
