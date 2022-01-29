package learn.spring.fssp.scraper.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.core.HoverflyMode;
import io.specto.hoverfly.junit5.api.HoverflyConfig;
import io.specto.hoverfly.junit5.api.HoverflyCore;
import learn.spring.fssp.scraper.Main;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebFlux;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import learn.spring.fssp.scraper.core.dao.Proxy;
import learn.spring.fssp.scraper.core.dao.ProxyRepository;
import learn.spring.fssp.scraper.core.dto.Request;
import learn.spring.fssp.scraper.core.dto.Result;
import learn.spring.fssp.scraper.core.process.Processor;
import learn.spring.fssp.scraper.core.proxy.ProxyListProvider;

import io.specto.hoverfly.junit5.HoverflyExtension;
import io.specto.hoverfly.junit.dsl.matchers.HoverflyMatchers;
import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.success;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

@Slf4j
@SpringBootTest(
        classes = Main.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"spring.main.web-application-type=reactive",
        "spring.main.allow-bean-definition-overriding=true"})
@EnableAutoConfiguration
@ExtendWith(SpringExtension.class)

@AutoConfigureWebFlux
@AutoConfigureWebTestClient
@AutoConfigureDataJpa

@HoverflyCore(mode = HoverflyMode.SIMULATE, config = @HoverflyConfig(plainHttpTunneling = true))
@ExtendWith(HoverflyExtension.class)
public class SpringTest {


    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private Processor processor;

    @MockBean
    private ProxyListProvider proxyListProvider;

    @Value("${proxy.port}")
    private int proxyPort;

    public static String responseWithCode = readFile("src/test/resources/messages/responseWithCode.json");
    public static String responseWithIP = readFile("src/test/resources/messages/responseWithIP.json");

    @BeforeEach
    void setUp(Hoverfly hoverfly) throws Exception{
        //init fssp stub
        hoverfly.simulate(dsl(
                service(HoverflyMatchers.any())
                .anyMethod(HoverflyMatchers.any())
                .queryParam("code", "Л8МБК")
                .willReturn(success().body(responseWithIP)),
                service(HoverflyMatchers.any())
                        .anyMethod(HoverflyMatchers.any())
                        .anyQueryParams()
                        .willReturn(success().body(responseWithCode))
                ));

        //override test proxy's value
        var testProxies = Collections.singletonList(
                new Proxy("localhost", hoverfly.getHoverflyConfig().getProxyPort()));
        Mockito.when(proxyListProvider.providerUrl()).thenReturn(null);
        Mockito.when(proxyListProvider.parseAnswer(Mockito.anyString()))
                .thenReturn(testProxies);
    }

    public String createRequest() throws Exception{
        Request request = new Request();
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setBirthdate(new SimpleDateFormat("dd.MM.yyyy").parse("10.11.1999"));

        return webClient.post()
                .uri("/fssp")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(request))
                .exchange()
                .expectAll(responseSpec -> responseSpec.expectStatus().isCreated(),
                        responseSpec -> responseSpec.expectBody().jsonPath("$.task").exists()
                )
                .returnResult(String.class).getResponseBody()
                .map(str->{
                    log.info("request on create task response is {}", str);
                    return JsonPath.parse(str).read("$.task").toString();
                })
                .blockLast(Duration.ofMillis(5000));
    }

    Result getResult(String taskId) throws Exception {
        log.info("getting result for task id {}", taskId);
        Result result = null;
        for (int delay = 10000; delay > 0; delay -= 1000){
            result = webClient.get()
                    .uri("/fssp/" + taskId)
                    .exchange()
                    .expectBody(Result.class)
                    .returnResult()
                    .getResponseBody();
            if (result.getTaskEnd() != null) break;
            Thread.sleep(1000);
        }
        return result;
    }

    @Test
    public void processingTest() throws Exception{
        String taskId = createRequest();
        Result result = getResult(taskId);

        Objects.requireNonNull(result, "result is null");
        Objects.requireNonNull(result.getTaskStart(), "TaskStart is null");
        Objects.requireNonNull(result.getTaskEnd(), "TaskEnd is null");
        Assertions.assertTrue(result.getTaskStart().before(result.getTaskEnd()), "TaskEnd less TaskStart");

        result.setTaskStart(new Date(0));
        result.setTaskEnd(new Date(0));
        String resultStr = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);
        log.info("result is {}", resultStr);
        String expected = readFile("src/test/resources/messages/result.json");
        JsonAssert.assertJsonEquals(expected,resultStr);
    }

    @SneakyThrows
    public static String readFile(String fileName){
        return new String(Files.readAllBytes(Paths.get(fileName)), "UTF-8");
    }
}
