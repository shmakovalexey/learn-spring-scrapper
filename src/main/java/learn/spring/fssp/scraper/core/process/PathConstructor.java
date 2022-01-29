package learn.spring.fssp.scraper.core.process;

import learn.spring.fssp.scraper.core.dao.Task;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static learn.spring.fssp.scraper.core.Consts.BIRTHDATE_PATTERN;

public class PathConstructor {
    //is[extended]=1&
    static final String URL_SEARCH_PATTERN = "http://is.fssp.gov.ru/ajax_search?system=ip&nocache=1";
    static final String VARIANT_ATTR = "is[variant]";
    static final String LASTNAME_ATTR = "is[last_name]";
    static final String FIRSTNAME_ATTR = "is[first_name]";
    static final String SECONDNAME_ATTR = "is[patronymic]";
    static final String BIRTHDATE_ATTR = "is[date]";
    static final String REGION_ATTR = "is[region_id][0]";



    public static String construct(Task task){
        requireNonNull(task.getLastName(), "Last name must be defined");
        requireNonNull(task.getFirstName(), "First name must be defineded");
        requireNonNull(task.getBirthdate(), "Birthdate must be defined");
        Map<String, String> map = new LinkedHashMap<>();
        map.put(VARIANT_ATTR, "" + 1);
        map.put(LASTNAME_ATTR, task.getLastName());
        map.put(FIRSTNAME_ATTR, task.getFirstName());
        if (task.getSecondName()!=null) map.put(SECONDNAME_ATTR, task.getSecondName());
        map.put(BIRTHDATE_ATTR, new SimpleDateFormat(BIRTHDATE_PATTERN).format(task.getBirthdate()));
        map.put(REGION_ATTR, String.valueOf(task.getRegion() == null ? -1 : task.getRegion()));
        return URL_SEARCH_PATTERN + "&" +
                map.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));
    }

//    static String encode(String str){
//        return URLEncoder.encode(str, StandardCharsets.UTF_8);
//    }
}
