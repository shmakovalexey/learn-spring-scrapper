package learn.spring.fssp.scraper.core.proxy;


import lombok.SneakyThrows;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class ClientPool extends GenericObjectPool<ClientWrapper> {

    public ClientPool(ClientPooledFactory factory){
        super(factory);
    }

    @SneakyThrows
    @Override
    public void invalidateObject(ClientWrapper obj) {
        super.invalidateObject(obj);
    }
}
