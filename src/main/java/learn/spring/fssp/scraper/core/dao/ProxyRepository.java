package learn.spring.fssp.scraper.core.dao;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ProxyRepository extends CrudRepository<Proxy, Long> {

    int countByEnabledTrue();

    //@Lock(LockModeType.PESSIMISTIC_READ)
    Optional<Proxy> findTopByEnabledTrueOrderByUsedAsc();

    Proxy findByHostAndPort(String host, Integer port);

    List<Proxy> findByCountGreaterThan(int count);
}