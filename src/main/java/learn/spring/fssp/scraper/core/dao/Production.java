package learn.spring.fssp.scraper.core.dao;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

import static learn.spring.fssp.scraper.core.Consts.PRODUCTION_TABLE;

@Entity
@Table(name = PRODUCTION_TABLE)
@Getter
@Setter
public class Production {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Task task;

    @Column(name = "exe_production")
    private String exe;
    private String name;
    private String details;
    private String subject;
    private String department;
    private String bailiff;
    @Column(name = "ip_end")
    private String end;

}
