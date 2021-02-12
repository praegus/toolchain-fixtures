package nl.praegus.fitnesse.slim.fixtures.mockserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpForward;
import org.mockserver.model.HttpOverrideForwardedRequest;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.LogEventRequestAndResponse;
import org.mockserver.model.MediaType;
import org.mockserver.model.ObjectWithJsonToString;
import org.mockserver.model.SocketAddress;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.HttpForward.forward;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockServer extends SlimFixture {

    private final ClientAndServer mock;

    /**
     * Initialize a mock server instance on a given port
     *
     * @param port The port number to use
     */
    public MockServer(int port) {
        ConfigurationProperties.enableCORSForAllResponses(true);
        mock = startClientAndServer(port);
    }

    /**
     * Set the mock response body for any request on the given path
     *
     * @param path The path to set this response for (supports regular expressions)
     * @param body The response body as a String
     */
    public void setResponseBodyForTo(String path, String body) {
        mock.when(request().withPath(path)).respond(response().withBody(getResponseBodyFromFileOrLiteral(body)));
    }

    /**
     * Set a mock response body with a given status code for any request on the given path
     * Usage: | set response body for | [path] | to | [body] | with status | [status] |
     *
     * @param path   The path to set this response for (supports regular expressions)
     * @param body   The response body as a String
     * @param status The status code to respond with
     */
    public void setResponseBodyForToWithStatus(String path, String body, int status) {
        mock.when(request().withPath(path))
                .respond(response()
                        .withBody(getResponseBodyFromFileOrLiteral(body))
                        .withStatusCode(status));
    }

    /**
     * Sets a binary file as the response for any request on the given path
     *
     * @param path The path to set this response for (supports regular expressions)
     * @param file The file to respond with (Can be an absolute path, a path relative to /files or a wiki file path)
     */
    public void setBinaryResponseForTo(String path, String file) {
        mock.when(request()
                .withPath(path))
                .respond(response()
                        .withBody(binary(getBytes(file))));
    }

    /**
     * Sets a response defined in the response definition hashmap for any request that matches the rules defined in the
     * request matching hashmap
     * Usage: | set response for | [map: requestMatching] | to | [map: responseDefinition] |
     *
     * @param requestMatching    a map object containing request filter rules. Valid rules are: method, path, content-type,
     *                           cookies, querystring, headers
     * @param responseDefinition a map object containing the definition of the response. Valid fields are: body, status,
     *                           headers, content-type, cookies
     */
    public void setResponseForTo(Map<String, Object> requestMatching, Map<String, Object> responseDefinition) {
        mock.when(httpRequestMatching(requestMatching))
                .respond(createResponse(responseDefinition));
    }

    /**
     * Forward any request on the given path to the target host/port
     * @param path The path to forward requests for
     * @param target The host/port to forward to (http(s)://host[:port])
     */
    public void forwardRequestsOnTo(String path, String target) {
        String host = target;
        HttpForward.Scheme scheme = HttpForward.Scheme.HTTP;
        int port = 80;

        if (target.split(":")[0].startsWith("https")) {
            port = 443;
            scheme = HttpForward.Scheme.HTTPS;
        }

        host = target.split("://")[1];

        if (host.split(":").length == 2) {
            port = Integer.parseInt(host.split(":")[1]);
            host = host.split(":")[0];
        }
        createForwardRule(path, host, port, scheme);
    }

    /**
     * Forward any request on the given path to the target host/port/path
     * Usage: | forward requests on | [path] | to | [target] | with path | [fwPath] |
     * @param path The path to forward requests for
     * @param target The host/port to forward to (http(s)://host[:port])
     * @param fwPath The path to forward to
     */
    public void forwardRequestsOnToWithPath(String path, String target, String fwPath) {
        String host = target;
        int port = 80;
        SocketAddress.Scheme scheme = SocketAddress.Scheme.HTTP;

        if (target.split(":")[0].startsWith("https")) {
            port = 443;
            scheme = SocketAddress.Scheme.HTTPS;
        }

        host = target.split(":")[1];

        if (target.split(":").length == 3) {
            port = Integer.parseInt(target.split(":")[2]);
        }
        createForwardRuleWithPath(path, host, port, scheme, fwPath);
    }

    private void createForwardRuleWithPath(String path, String host, int port, SocketAddress.Scheme scheme, String fwPath) {
        mock.when(request().withPath(path))
                .forward(HttpOverrideForwardedRequest.forwardOverriddenRequest(
                        request()
                                .withPath(fwPath)
                                .withSocketAddress(host.split("://")[1], port, scheme)));
    }

    private void createForwardRule(String path, String host, int port, HttpForward.Scheme scheme) {
        mock.when(request().withPath(path))
                .forward(forward()
                        .withHost(host)
                        .withPort(port)
                        .withScheme(scheme));
    }

    /**
     * Opens the mockserver UI in the browser. Use only on local wiki test runs!
     */
    public void openMockServerUi() {
        mock.openUI();
    }

    /**
     * Stop the server
     */
    public void stopMockServer() {
        mock.stop();
    }

    private HttpResponse createResponse(Map<String, Object> responseProperties) {
        HttpResponse resp = response();
        for (String prop : responseProperties.keySet()) {
            switch (prop.toLowerCase()) {
                case "body":
                    resp = resp.withBody(getResponseBodyFromFileOrLiteral(responseProperties.get(prop).toString()));
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

    public HashMap<Integer, Object> recordedRequests() {
        return arrayOfJsonObjectsToMap(mock.retrieveRecordedRequests(null));
    }

    public HashMap<Integer, Object> recordedRequestsForPath(String path) {
        return arrayOfJsonObjectsToMap(mock.retrieveRecordedRequests(request().withPath(path)));
    }

    public int numberOfRequestsForPath(String path) {
        return mock.retrieveRecordedRequests(request().withPath(path)).length;
    }

    public HashMap<Integer, Object> recordedRequestsAndResponses() {
        return arrayOfJsonObjectsToMap(mock.retrieveRecordedRequestsAndResponses(null));
    }

    public HashMap<Integer, Object> recordedRequestsAndResponsesForPath(String path) {
        return arrayOfJsonObjectsToMap(mock.retrieveRecordedRequestsAndResponses(request().withPath(path)));
    }

    public List<String> erroredRequests() {
        List<String> result = new ArrayList<>();
        LogEventRequestAndResponse[] requests = mock.retrieveRecordedRequestsAndResponses(null);
        for (LogEventRequestAndResponse r : requests) {
            if (r.getHttpResponse().getStatusCode() >= 400) {
                HttpRequest req = (HttpRequest) r.getHttpRequest();
                result.add(req.getPath() + " " + r.getHttpResponse().getStatusCode());
            }
        }
        return result;
    }

    public HashMap<Integer, Object> arrayOfJsonObjectsToMap(ObjectWithJsonToString[] array) {
        HashMap<Integer, Object> result = new HashMap<>();
        int count = 0;
        ObjectMapper m = new ObjectMapper();
        try {
            for (ObjectWithJsonToString obj : array) {
                result.put(count, m.readValue(obj.toString(), HashMap.class));
                count++;
            }
            return result;
        } catch (Exception e) {
            throw new SlimFixtureException(true, e.getMessage());
        }
    }

    protected String getResponseBodyFromFileOrLiteral(String responseFile) {
        if (responseFile.startsWith("http://files") | responseFile.startsWith("files/")) {
            String responseFilePath = getFilePathFromWikiUrl(responseFile);
            try {
                return readFile(responseFilePath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new SlimFixtureException(true, "Unable to get response file contents from: " + responseFile, e);
            }
        }
        return responseFile;
    }

    protected byte[] getBytes(String responseFile) {
        if (responseFile.startsWith("http://files") | responseFile.startsWith("files/")) {
            String responseFilePath = getFilePathFromWikiUrl(responseFile);
            try {
                return Files.readAllBytes(Paths.get(responseFilePath));
            } catch (IOException e) {
                throw new SlimFixtureException(true, "Unable to get response file contents from: " + responseFile, e);
            }
        }
        return new byte[]{};
    }

    protected static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }


}
