package learn.spring.fssp.scraper.core.proxy;

import learn.spring.fssp.scraper.core.dao.Proxy;

import java.util.List;

public interface ProxyListProvider {
    String providerUrl();
    List<Proxy> parseAnswer(String answer);
}
