package learn.spring.fssp.scraper.core;

import learn.spring.fssp.scraper.core.dao.ProxyRepository;
import learn.spring.fssp.scraper.core.dao.Task;
import learn.spring.fssp.scraper.core.dao.TaskRepository;
import learn.spring.fssp.scraper.core.dto.Request;
import learn.spring.fssp.scraper.core.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import static learn.spring.fssp.scraper.core.Consts.BIRTHDATE_PATTERN;
import static learn.spring.fssp.scraper.core.Consts.CONTROLLER_PATH;

@Slf4j
@RestController
@RequestMapping(CONTROLLER_PATH)
public class Controller {

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    ProxyRepository proxyRepository;

    @GetMapping("/proxies")
    public ResponseEntity<String> result(){
        var proxies = proxyRepository.findByCountGreaterThan(0);
        if (proxies.isEmpty()) return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        String body = proxies.stream()
                .map(proxy -> proxy.getHost() + ":" + proxy.getPort())
                .collect(Collectors.joining("\n"));
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body);
    }

    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<Result> result(@PathVariable String id){
        log.debug("get response for result {}", id);
        var optional = taskRepository.findById(id);
        if (optional.isEmpty()){
            var result = new Result();
            result.setError("NOT_FOUNDED");
            return ResponseEntity.status(HttpStatus.NO_CONTENT).cacheControl(CacheControl.noCache())
                    .body(result);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Result.fromTask(optional.get()));
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> createTask(
            @RequestParam(required = false) String firstname,
            @RequestParam(required = false) String secondname,
            @RequestParam(required = false) String lastname,
            @RequestParam(required = false) String birthdate,
            @RequestParam(required = false) String region)
    {
        if (!StringUtils.hasText(firstname)) return errorResponse("firstname must be defined");
        if (!StringUtils.hasText(lastname)) return errorResponse("lastname must be defined");
        if (!StringUtils.hasText(birthdate)) return errorResponse("birthdate must be defined");

        Task example = new Task();
        example.setFirstName(firstname);
        example.setSecondName(secondname);
        example.setLastName(lastname);
        try{
            example.setBirthdate(new SimpleDateFormat(BIRTHDATE_PATTERN).parse(birthdate));
        } catch (Exception e){
            return errorResponse("value '" + birthdate + "' incorrect for date");
        }
        if (region != null) try{
            example.setRegion(Integer.parseInt(region));
        } catch (Exception e){
            return errorResponse("value '" + region + "' is not integer");
        }
        return createTask(example);
    }

    ResponseEntity<Map<String, String>> errorResponse(String errorText){
        return ResponseEntity.badRequest().body(Collections.singletonMap("error", errorText));

    }

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, String>> createTask(@RequestBody Request request) {
        return createTask(request.toTaskExample());
    }

    ResponseEntity<Map<String, String>> createTask(Task example){
        var task = taskRepository.findOne(Example.of(example));
        if (task.isPresent()){
            return new ResponseEntity<>(Collections.singletonMap("task", task.get().getId()),
                    HttpStatus.ALREADY_REPORTED);
        }
        example.setStarted(new Date());
        var saved = taskRepository.saveAndFlush(example);
        return new ResponseEntity<>(Collections.singletonMap("task", saved.getId()),
                HttpStatus.CREATED);
    }
}
