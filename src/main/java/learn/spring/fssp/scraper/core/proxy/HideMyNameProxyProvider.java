package learn.spring.fssp.scraper.core.proxy;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.util.StringUtils;
import learn.spring.fssp.scraper.core.dao.Proxy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class HideMyNameProxyProvider implements ProxyListProvider {

    private static final String PROVIDER_URL = "https://hidemy.name/ru/proxy-list/?type=h";

    private static final String HOST_FIELD_NAME = "IP адрес";
    private static final String PORT_FIELD_NAME = "Порт";
    private static final String LOCATION_FIELD_NAME = "Страна, Город";

    protected Integer hostFieldNum = null;
    protected Integer portFieldNum = null;
    protected Integer locationFieldNum = null;

    @Override
    public String providerUrl() {
        return PROVIDER_URL;
    }

    @Override
    public List<Proxy> parseAnswer(String answer) {
        Iterator<Element> iterator =  Jsoup.parseBodyFragment(answer)
                .getElementsByTag("table").first()
                .getElementsByTag("tr").iterator();
        extractHeaderNums(iterator.next());
        return extractProxyList(iterator);
    }

    protected void extractHeaderNums(Element header){
        for(int i = 0; i < header.children().size(); i++){
            Element td = header.children().get(i);
            if (HOST_FIELD_NAME.equals(td.html())) hostFieldNum = i;
            else if (PORT_FIELD_NAME.equals(td.html())) portFieldNum = i;
            else if (LOCATION_FIELD_NAME.equals(td.html())) locationFieldNum = i;
        }
    }

    protected List<Proxy> extractProxyList(Iterator<Element> iterator){
        List<Proxy> result = new ArrayList<>();
        iterator.forEachRemaining(row->{
            try{
                Proxy proxy = new Proxy();
                proxy.setHost(row.child(hostFieldNum).html());
                proxy.setPort(Integer.valueOf(row.child(portFieldNum).html()));
                if (locationFieldNum != null) proxy.setLocation(extractNodeValue(row.child(locationFieldNum)));
                result.add(proxy);
            } catch (Exception ignored){
                log.warn("cannot parse proxy row: {}", ignored);
            }
        });
        return result;
    }

    String extractNodeValue(Element element){
        return element.children().stream().map(Element::html)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(", "));
    }
}
