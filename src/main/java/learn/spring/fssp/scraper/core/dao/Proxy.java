package learn.spring.fssp.scraper.core.dao;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import javax.persistence.*;

import java.util.Date;

import static learn.spring.fssp.scraper.core.Consts.PROXIES_TABLE;

@Entity
@Table(name = PROXIES_TABLE,
        uniqueConstraints = @UniqueConstraint(columnNames={"host", "port"})
)
@Getter
@Setter
public class Proxy {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue
    private Long id;

    public Proxy(){}

    public Proxy(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    @Column(nullable = false)
    String host;
    @Column(nullable = false)
    Integer port;
    String location;

    //https://codingexplained.com/coding/java/hibernate/hibernate-mapping-smallint-tinyint-int-column-to-boolean
    @Type(type = "org.hibernate.type.NumericBooleanType")
    Boolean enabled = true;

    @Temporal(TemporalType.TIMESTAMP)
    Date used = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    Date banned;

    int count = 0;

    @Override
    public String toString(){
        return String.format("proxy-%s[%s:%s](%s)", getId(), getHost(), getPort(), getLocation());
    }
}
