package learn.spring.fssp.scraper.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import learn.spring.fssp.scraper.core.dao.Task;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.TimeZone;

import static learn.spring.fssp.scraper.core.Consts.BIRTHDATE_PATTERN;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Request {
    private String firstName;
    private String secondName;
    private String lastName;

    @JsonFormat (pattern = BIRTHDATE_PATTERN)
    private Date birthdate;
    private Integer region;

    public Task toTaskExample(){
        Task task = new Task();
        task.setFirstName(getFirstName().toUpperCase());
        task.setSecondName(StringUtils.hasText(getSecondName()) ? getSecondName().toUpperCase() : null);
        task.setLastName(getLastName().toUpperCase());
        //Удаляем смещение временного пояса
        if (getBirthdate()!=null) task.setBirthdate(new Date(getBirthdate().getTime() - TimeZone.getDefault().getRawOffset()));
        task.setRegion(getRegion());
        return task;
    }
}
