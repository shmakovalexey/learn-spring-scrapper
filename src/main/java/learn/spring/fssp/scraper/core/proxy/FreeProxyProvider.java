package learn.spring.fssp.scraper.core.proxy;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import learn.spring.fssp.scraper.core.dao.Proxy;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FreeProxyProvider implements ProxyListProvider {

    private static final String HOST_FIELD_NAME = "IP Address";
    private static final String PORT_FIELD_NAME = "Port";
    private static final String LOCATION_FIELD_NAME = "Country";
    private static final String HTTPS_FIELD_NAME = "Https";

    protected Integer hostFieldNum = null;
    protected Integer portFieldNum = null;
    protected Integer locationFieldNum = null;
    protected Integer httpsFieldNum = null;

    protected boolean httpsFilter = true;

    public FreeProxyProvider(){
        this(true);
    }

    public FreeProxyProvider(boolean httpsFilter){
        this.httpsFilter = httpsFilter;
    }

    @Override
    public String providerUrl() {
        return "https://free-proxy-list.net/";
    }

    @Override
    public List<Proxy> parseAnswer(String answer) {
        Element table =  Jsoup.parseBodyFragment(answer)
                .getElementsByTag("table").first();
        extractHeaderNums(table.getElementsByTag("th"));
        return extractProxyList(table.getElementsByTag("tr"));
    }

    protected void extractHeaderNums(Elements headers){
        for(int i = 0; i < headers.size(); i++){
            Element td = headers.get(i);
            if (HOST_FIELD_NAME.equals(td.html())) hostFieldNum = i;
            else if (PORT_FIELD_NAME.equals(td.html())) portFieldNum = i;
            else if (LOCATION_FIELD_NAME.equals(td.html())) locationFieldNum = i;
            else if (HTTPS_FIELD_NAME.equals(td.html())) httpsFieldNum = i;
        }
    }

    protected List<Proxy> extractProxyList(Elements rows){
        List<Proxy> result = new ArrayList<>();
        for( var row : rows){
            try{
                if (!httpsFilter || "yes".equalsIgnoreCase(row.child(httpsFieldNum).html())){
                    Proxy proxy = new Proxy();
                    proxy.setHost(row.child(hostFieldNum).html());
                    proxy.setPort(Integer.valueOf(row.child(portFieldNum).html()));
                    if (locationFieldNum != null) proxy.setLocation(row.child(locationFieldNum).html());
                    result.add(proxy);
                }
            } catch (Exception e){
                log.warn("cannot parse proxy row: " + e);
            }
        }
        return result;
    }
}
