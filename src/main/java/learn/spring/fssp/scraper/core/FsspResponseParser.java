package learn.spring.fssp.scraper.core;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import learn.spring.fssp.scraper.core.dao.Production;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
public class FsspResponseParser {
    public static final String ERROR_INPUT_CODE_TXT = "Неверно введен код";
    public static final String CAPTURE_RESPONSE_TXT = "Введите код с картинки";
    public static final String COMPLEX_RESPONSE_TXT = "<div class=\\\"npagination-is\\\">";
    public static final String EMPTY_RESPONSE_TXT = "По вашему запросу ничего не найдено";
    public static final String BLOCK_RESPONSE_TXT = "Вы превысили лимит на количество подключений к сайту";

    private final String body;
    private Integer nextPage;
    private Collection<Production> data = Collections.emptyList();


    public enum ResponseType {CAPTCHA_RESPONSE, EMPTY_RESPONSE, NORMAL_RESPONSE, BLOCKED_RESPONSE}
    ResponseType type;

    public FsspResponseParser(String body){
        this.body = body;
        if (body.contains(CAPTURE_RESPONSE_TXT) || body.contains(ERROR_INPUT_CODE_TXT)) type = ResponseType.CAPTCHA_RESPONSE;
        else if (body.contains(EMPTY_RESPONSE_TXT)) type = ResponseType.EMPTY_RESPONSE;
        else if (body.contains(BLOCK_RESPONSE_TXT)) type = ResponseType.BLOCKED_RESPONSE;
        else if (body.contains(COMPLEX_RESPONSE_TXT)) {
            type = ResponseType.NORMAL_RESPONSE;
            parsePagging();
        }
        else type = ResponseType.NORMAL_RESPONSE;
        if (type==ResponseType.NORMAL_RESPONSE){
            parse();
        }
    }

    public String getBody() {
        return body;
    }
    public Integer getNextPage() {
        return nextPage;
    }
    public ResponseType getType() {
        return type;
    }

    public Collection<Production> getData(){
        return data;
    }

    private static ObjectMapper mapper = new ObjectMapper();
    public static String getHtmlFromJsonResponse(String str) throws IOException {
        JsonNode obj = mapper.readTree(str);
        return obj.get("data").asText();
    }

    public boolean isLast(){
        return nextPage == null;
    }

    public String getCaptchaBase64() throws IOException {
        Document doc = Jsoup.parseBodyFragment(getHtmlFromJsonResponse(body));
        String src = doc.getElementsByTag("img").first().attr("src");
        src = src.substring(src.indexOf(',')+1);
        return src;
    }

    static Pattern patternNextPage = Pattern.compile("&amp;page=([0-9]+?)\\\\\">Следующая</a>");
    void parsePagging(){
        Matcher matcher = patternNextPage.matcher(body);
        if (matcher.find()){
            String strNextPage = matcher.group(1);
            nextPage = Integer.parseInt(strNextPage);
        }
    }


    @SneakyThrows
    void parse() {
        String htmlData = getHtmlFromJsonResponse(body);
        Document doc = Jsoup.parseBodyFragment(htmlData);
        Element table = doc.getElementsByTag("table").first();
        if (table!=null){
            data = doc.getElementsByTag("tr").stream()
                    .map(element-> element.getElementsByTag("td")).filter(elements -> elements.size() == 8)
                    .map(cols->{
                        Production production = new Production();
                        production.setName(cols.get(0).html());
                        production.setExe(cols.get(1).html());
                        production.setDetails(cols.get(2).html());
                        production.setEnd(cols.get(3).html());
                        production.setSubject(cols.get(5).html());
                        production.setDepartment(cols.get(6).html());
                        production.setBailiff(cols.get(7).html());
                        return production;
                    }).collect(Collectors.toList());
        }
    }

}
