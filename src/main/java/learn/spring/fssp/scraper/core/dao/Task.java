package learn.spring.fssp.scraper.core.dao;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static learn.spring.fssp.scraper.core.Consts.TASK_TABLE;

@Entity
@Table(name = TASK_TABLE)
@DynamicUpdate
@Getter @Setter
public class Task {
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    private String firstName;
    @Column(nullable = true)
    private String secondName;
    private String lastName;

    @Temporal(TemporalType.DATE)
    private Date birthdate;

    @Column(nullable = true)
    private Integer region;

    @Column(nullable = true)
    Integer nextPage;

    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Temporal(TemporalType.TIMESTAMP)
    private Date started;

    @Temporal(TemporalType.TIMESTAMP)
    private Date called;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updated;

    @Temporal(TemporalType.TIMESTAMP)
    private Date completed;

    @OneToMany(mappedBy = "task", cascade = CascadeType.PERSIST)
    private List<Production> productions;

    @PrePersist
    protected void onCreate() {
        created = new Date();
        id = UUID.randomUUID().toString();
    }

    @PreUpdate
    protected void onUpdate() {
        updated = new Date();
    }
}