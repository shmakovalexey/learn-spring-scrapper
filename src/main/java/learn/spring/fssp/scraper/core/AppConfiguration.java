package learn.spring.fssp.scraper.core;

import learn.spring.fssp.scraper.core.process.Processor;
import learn.spring.fssp.scraper.core.proxy.*;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.reactive.config.EnableWebFlux;

@EnableScheduling
@EnableWebFlux
@EnableJpaRepositories("learn.spring.fssp.scraper.core.dao")
@EnableTransactionManagement
@org.springframework.context.annotation.Configuration
public class AppConfiguration {

    @Bean
    ProxyListProvider proxyListProvider(){

        return new FreeProxyProvider(false);
    }

//    @Bean
//    public PlatformTransactionManager txManager(SessionFactory sessionFactory) {
//        return new HibernateTransactionManager(sessionFactory);
//    }

//    @Bean
//    ProxyListProvider proxyListProvider(){
//        return new FromFileProxyProvider();
//    }

    @Bean
    ClientCreator clientCreator(){
        return new ClientCreator();
    }

    @Bean
    ClientPooledFactory clientPooledFactory(){
        return new ClientPooledFactory(clientCreator());
    }

    @Bean
    public ClientPool createPool(){
        return new ClientPool(clientPooledFactory());
    }

    @Bean
    public Processor processor(){
        return new Processor();
    }
}
