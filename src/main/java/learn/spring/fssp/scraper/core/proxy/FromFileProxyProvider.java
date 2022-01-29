package learn.spring.fssp.scraper.core.proxy;


import lombok.SneakyThrows;
import learn.spring.fssp.scraper.core.dao.Proxy;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

public class FromFileProxyProvider implements ProxyListProvider {
    String fileName = "proxies.txt";

    @Override
    public String providerUrl() {
        return null;
    }

    @Override
    @SneakyThrows
    public List<Proxy> parseAnswer(String answer) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(fileOrResources(fileName)))) {
            return br.lines()
                    .filter(s->!s.trim().startsWith("#"))
                    .map(str->str.split(":"))
                    .filter(array->array!=null && array.length == 2)
                    .map(array->{
                        Proxy proxy = new Proxy();
                        proxy.setHost(array[0]);
                        proxy.setPort(Integer.valueOf(array[1]));
                        return proxy;
                    }).collect(Collectors.toList()) ;
        }
    }

    InputStream fileOrResources(String name){
        try{
            return new FileInputStream(name);
        } catch (Exception e){
            return ClassLoader.getSystemResourceAsStream(name);
        }
    }
}
