package learn.spring.fssp.scraper.core.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    @Query(value = "select t from Task t where t.completed is null and (t.called is null or t.called < ?1)", countQuery = "20")
    List<Task> findForProcessing(@NonNull Date date);

    @Modifying
    @Transactional
    @Query(value = "update Task t set t.called = current_timestamp where t.id = ?1")
    void signCalledById(@NonNull String id);

    @Transactional
    default void signCalledByIds(@NonNull List<Task> tasks){
        tasks.forEach(task->signCalledById(task.getId()));
    }

    @Modifying
    @Transactional
    @Query(value = "update Task t set t.completed = current_timestamp where t.id = ?1")
    void signCompletedById(@NonNull String id);

}