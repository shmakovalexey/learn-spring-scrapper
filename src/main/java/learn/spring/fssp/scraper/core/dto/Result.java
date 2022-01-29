package learn.spring.fssp.scraper.core.dto;

import com.fasterxml.jackson.annotation.*;
import learn.spring.fssp.scraper.core.dao.Task;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static learn.spring.fssp.scraper.core.Consts.DATETIME_PATTERN;


@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"error", "request", "task_start", "task_end", "productions" })
public class Result {
    private String error;
    Request request;

    @JsonProperty("task_start")
    @JsonFormat (pattern = DATETIME_PATTERN)
    Date taskStart;

    @JsonProperty("task_end")
    @JsonFormat (pattern = DATETIME_PATTERN)
    Date taskEnd;

    List<ResultProduction> productions;

    public static Result fromTask(Task task){
        Result result = new Result();
        Request request = new Request();
        request.setFirstName(task.getFirstName());
        request.setSecondName(task.getSecondName());
        request.setLastName(task.getLastName());
        request.setBirthdate(task.getBirthdate());
        request.setRegion(task.getRegion());
        result.setRequest(request);
        result.setTaskStart(task.getStarted());
        if (task.getCompleted() != null){
            result.setTaskEnd(task.getCompleted());
            List<ResultProduction> productions = new ArrayList<>(task.getProductions().size());
            task.getProductions().forEach(production -> {
                productions.add(ResultProduction.fromProduction(production));
            });
            result.setProductions(productions);
        }
        return result;
    }

}
