package learn.spring.fssp.scraper.core.proxy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.reactive.function.client.WebClient;
import learn.spring.fssp.scraper.core.dao.Proxy;


@Getter @Setter
public class ClientWrapper {
    Proxy proxy;
    WebClient client;

    public ClientWrapper(WebClient client, Proxy proxy){
        this.client = client;
        this.proxy = proxy;
    }

    @Override
    public String toString(){
        return "client-" + proxy;
    }
}
