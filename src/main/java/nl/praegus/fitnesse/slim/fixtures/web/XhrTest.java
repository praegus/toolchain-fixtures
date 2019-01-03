package nl.praegus.fitnesse.slim.fixtures.web;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.proxy.CaptureType;
import nl.hsac.fitnesse.fixture.slim.JsonFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

public class XhrTest extends JsonFixture {
    private static BrowserMobProxyServer proxy = new BrowserMobProxyServer();
    private String endpoint = "";
    private String customFilter = "";

    public XhrTest(String domain, String endpoint) {
        startProxyOnFor(domain, endpoint);
    }

    public XhrTest() {
        if(!proxy.isStarted()) {
            startProxyOnFor("","");
        }
    }

    public void filterHarBy(String filter) {
        customFilter = filter;
    }

    public void startProxyOnFor(String domain, String endpoint) {
        this.endpoint = endpoint;
        if(proxy.isStarted()) {
            proxy.endHar();
            newHarForDomain(domain);
        } else {
            proxy.start();
            proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT);
            if(cleanupValue(domain).isEmpty()) {
                proxy.newHar();
            } else {
                proxy.newHar(cleanupValue(domain));
            }
        }
    }

    public void newHarForDomain(String domain) {
        proxy.newHar(domain);
    }

    public int getProxyPort() {
        return proxy.getPort();
    }

    private void filterHar(Har har) {
        if(customFilter.isEmpty()) {
            har.getLog().getEntries()
                    .removeIf(x -> !x.getRequest().getUrl().replaceAll("\\?.*", "").endsWith(endpoint));
        } else {
            har.getLog().getEntries()
                    .removeIf(x -> !x.getRequest().getUrl().contains(customFilter));
        }
    }

    public String saveHarTo(String fileName) {
        Har har = proxy.getHar();
        filterHar(har);
        String filepath = getEnvironment().getFilePathFromWikiUrl(fileName);
        try {
            File harFile = new File(filepath);
            har.writeTo(harFile);
            loadFile(harFile.getAbsolutePath());
            return harFile.getAbsolutePath();
        } catch (IOException e) {
            throw new SlimFixtureException(e);
        }
    }

    public void useAllData() throws IOException {
        Har har = proxy.getHar();
        filterHar(har);
        StringWriter jsonStr = new StringWriter();
        har.writeTo(jsonStr);
        load(jsonStr.toString());
    }

    public void usePostDataFromRequest(int req) {
        Har har = proxy.getHar();
        filterHar(har);
        load(har.getLog().getEntries().get(req).getRequest().getPostData().getText());
    }

    public void useQueryStringFromRequest(int req) {
        Har har = proxy.getHar();
        filterHar(har);
        JSONObject queryString = new JSONObject();
        for(HarNameValuePair pair : har.getLog().getEntries().get(req).getRequest().getQueryString()) {
            queryString.put(pair.getName(), pair.getValue());
        }
        load(queryString.toString());
    }

    public Object valueOfJsonPath(String path) {
        return jsonPath(path);
    }

    public boolean useParametersFromJsonPath(String path) {
        load(urlEncodedParametersToJson(valueOfJsonPath(path).toString()).toString());
        return true;
    }

    public String dataInUse() {
        return object();
    }

    private JSONObject urlEncodedParametersToJson(String parameters) {
        JSONObject result = new JSONObject();
        String[] params = parameters.split("&");
        for(String param : params) {
            String[] pair = param.split("=");
            String val;
            if(pair.length > 1) {
                try {
                    val = java.net.URLDecoder.decode(pair[1], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    val = pair[1];
                }
            } else {
                val = "";
            }
            result.put(pair[0], val);
        }
        return result;
    }

    public void stopProxy() {
        proxy.endHar();
        proxy.stop();
        proxy = new BrowserMobProxyServer();
    }
}
