package learn.spring.fssp.scraper.core.proxy;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import learn.spring.fssp.scraper.core.dao.Proxy;

import java.util.Date;

@Slf4j
public class ClientPooledFactory extends BasePooledObjectFactory<ClientWrapper> {

    ClientCreator clientCreator;

    public ClientPooledFactory(ClientCreator clientCreator) {
        this.clientCreator = clientCreator;
    }

    @Override
    public ClientWrapper create() throws Exception {
        return clientCreator.create();
    }

    @Override
    public PooledObject<ClientWrapper> wrap(ClientWrapper obj) {
        return new DefaultPooledObject<ClientWrapper>(obj);
    }

    @Override
    public void destroyObject(final PooledObject<ClientWrapper> p) {
        Proxy proxy = p.getObject().getProxy();
        //if proxy banned don't disable proxy
        if (proxy.getBanned() == null || proxy.getBanned().before(new Date())){
            clientCreator.disableProxy(p.getObject().getProxy());
            log.debug("{} disabled", proxy);
        };
    }

}