package nl.praegus.fitnesse.slim.fixtures.web;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarNameValuePair;
import net.lightbody.bmp.proxy.CaptureType;
import nl.hsac.fitnesse.fixture.slim.JsonFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class XhrTest extends JsonFixture {
    private static final BrowserMobProxy proxy = new BrowserMobProxyServer();
    private static final ByteArrayOutputStream harOutput = new ByteArrayOutputStream();
    private static String endpoint;

    public XhrTest(String domain, String endpoint) {
        this.endpoint = endpoint; //We should find a better solution for this
        proxy.start(0);
        proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT);
        proxy.newHar(domain);
    }

    public XhrTest() {
    }

    public int getProxyPort() {
        return proxy.getPort();
    }

    private void filterHar(Har har) {
        har.getLog().getEntries().removeIf(x -> !x.getRequest().getUrl().replaceAll("\\?.*", "").endsWith(endpoint));
    }

    public String saveHarTo(String fileName) {
        Har har = proxy.getHar();
        filterHar(har);
        String filepath = getEnvironment().getFilePathFromWikiUrl(fileName);
        try {
            File harFile = new File(filepath);
            har.writeTo(harFile);
            return harFile.getAbsolutePath();
        } catch (IOException e) {
            throw new SlimFixtureException(e);
        }
    }

    public boolean usePostDataFromRequest(int req) {
        Har har = proxy.getHar();
        filterHar(har);
        load(har.getLog().getEntries().get(req).getRequest().getPostData().getText());
        return true;
    }

    public boolean useQueryStringFromRequest(int req) {
        Har har = proxy.getHar();
        filterHar(har);
        JSONObject queryString = new JSONObject();
        for(HarNameValuePair pair : har.getLog().getEntries().get(req).getRequest().getQueryString()) {
            queryString.put(pair.getName(), pair.getValue());
        }
        load(queryString.toString());
        return true;
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
        try {
            harOutput.flush();
        } catch (IOException e) {
            throw new SlimFixtureException(false, "Failed to flush har output");
        }
        proxy.stop();
    }
}
