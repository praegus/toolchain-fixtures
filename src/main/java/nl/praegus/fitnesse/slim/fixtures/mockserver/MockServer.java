package nl.praegus.fitnesse.slim.fixtures.mockserver;

import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpForward;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

import java.util.Map;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpForward.forward;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockServer extends SlimFixture {

    private static ClientAndServer mock;

    public MockServer(int port) {
        mock = startClientAndServer(port);
    }

    public void setResponseForTo(String path, String body) {
        mock.when(request().withPath(path)).respond(response().withBody(body));
    }

    public void setResponseForTo(Map<String, Object> requestMatching, Map<String, Object> responseDefinition) {
        mock.when(httpRequestMatching(requestMatching)).respond(createResponse(responseDefinition));
    }

    public void forwardRequestsOnTo(String path, String target) {
        String host = target;
        int port = 80;
        if (target.contains(":")) {
            host = target.split(":")[0];
            port = Integer.parseInt(target.split(":")[1]);
        }
        createForwardRule(path, host, port, HttpForward.Scheme.HTTP);
    }

    public void forwardRequestsOnToHttps(String path, String target) {
        String host = target;
        int port = 443;
        if (target.contains(":")) {
            host = target.split(":")[0];
            port = Integer.parseInt(target.split(":")[1]);
        }
        createForwardRule(path, host, port, HttpForward.Scheme.HTTPS);

    }

    private void createForwardRule(String path, String host, int port, HttpForward.Scheme scheme) {
        mock.when(request().withPath(path))
                .forward(forward()
                        .withHost(host)
                        .withPort(port)
                        .withScheme(scheme));
    }

    public void openMockServerUi() {
        mock.openUI();
    }

    public void stopMockServer() {
        mock.stop();
    }

    private HttpResponse createResponse(Map<String, Object> responseProperties) {
        HttpResponse resp = response();
        for (String prop : responseProperties.keySet()) {
            switch (prop.toLowerCase()) {
                case "body":
                    resp = resp.withBody(responseProperties.get(prop).toString());
                    break;
                case "status":
                    resp = resp.withStatusCode(Integer.parseInt(responseProperties.get(prop).toString()));
                    break;
                case "headers":
                    if (!(responseProperties.get(prop) instanceof Map)) {
                        throw new SlimFixtureException("Headers to return should be defined as a map containing key/value pairs.");
                    }
                    for (Map.Entry header : ((Map<?, ?>) responseProperties.get(prop)).entrySet()) {
                        resp = resp.withHeader(header.getKey().toString(), header.getValue().toString());
                    }
                    break;
                case "content-type":
                    String[] contentType = responseProperties.get(prop).toString().split("/");
                    if (contentType.length < 2) {
                        throw new SlimFixtureException("Content-Type should be defined as type/subtype. E.g. application/json");
                    }
                    resp = resp.withContentType(new MediaType(contentType[0], contentType[1]));
                    break;
                case "cookies":
                    if (!(responseProperties.get(prop) instanceof Map)) {
                        throw new SlimFixtureException("Cookies to return should be defined as a map containing key/value pairs.");
                    }
                    for (Map.Entry cookie : ((Map<?, ?>) responseProperties.get(prop)).entrySet()) {
                        resp = resp.withCookie(cookie.getKey().toString(), cookie.getValue().toString());
                    }
                    break;
                default:
                    throw new SlimFixtureException("Unknown reponse property: " + prop);
            }
        }
        return resp;
    }

    /**
     * * Support method, path, cookie, querystring, content type, header
     *
     * @param rules
     * @return
     */
    private HttpRequest httpRequestMatching(Map<String, Object> rules) {
        HttpRequest req = request();
        for (String rule : rules.keySet()) {
            switch (rule.toLowerCase()) {
                case "method":
                    req = req.withMethod(rules.get(rule).toString());
                    break;
                case "path":
                    req = req.withPath(rules.get(rule).toString());
                    break;
                case "content-type":
                    String[] contentType = rules.get(rule).toString().split("/");
                    if (contentType.length < 2) {
                        throw new SlimFixtureException("Content-Type should be defined as type/subtype. E.g. application/json");
                    }
                    req = req.withContentType(new MediaType(contentType[0], contentType[1]));
                    break;
                case "cookies":
                    if (!(rules.get(rule) instanceof Map)) {
                        throw new SlimFixtureException("Cookies to filter requests on should be defined as a map containing key/value pairs.");
                    }
                    for (Map.Entry cookie : ((Map<?, ?>) rules.get(rule)).entrySet()) {
                        req = req.withCookie(cookie.getKey().toString(), cookie.getValue().toString());
                    }
                    break;
                case "querystring":
                    if (!(rules.get(rule) instanceof Map)) {
                        throw new SlimFixtureException("QueryString parameters to filter requests on should be defined as a map containing key/value pairs.");
                    }
                    for (Map.Entry param : ((Map<?, ?>) rules.get(rule)).entrySet()) {
                        req = req.withQueryStringParameter(param.getKey().toString(), param.getValue().toString());
                    }
                    break;
                case "headers":
                    if (!(rules.get(rule) instanceof Map)) {
                        throw new SlimFixtureException("Headers to filter requests on should be defined as a map containing key/value pairs.");
                    }
                    for (Map.Entry header : ((Map<?, ?>) rules.get(rule)).entrySet()) {
                        req = req.withHeader(header.getKey().toString(), header.getValue().toString());
                    }
                    break;
                default:
                    throw new SlimFixtureException("Unknown rule: " + rule);
            }
        }
        return req;
    }


}
