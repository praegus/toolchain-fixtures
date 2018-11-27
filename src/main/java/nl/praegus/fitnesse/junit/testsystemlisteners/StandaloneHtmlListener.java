package nl.praegus.fitnesse.junit.testsystemlisteners;

import com.google.common.io.ByteStreams;
import fitnesse.testsystems.*;
import nl.praegus.fitnesse.junit.testsystemlisteners.util.OutputChunkParser;

import java.io.Closeable;
import java.io.IOException;

/*
 TestSystemListener that keeps a static StringBuilder for the running testcase
 The sb contains all the concatenated output chunks that are returned from the test system.
 The html contained in the sb is standalone. No external resources are required. CSS, JS and
 images (such as screen shots) are embedded in the page.

 To obtain the html, simply call StandaloneHtmlListener.output from your testrunlistener.
 Note: this cass is not usable when doing parallel execution from the same workspace.
 */

public class StandaloneHtmlListener implements TestSystemListener, Closeable {
    public static StringBuilder output = new StringBuilder();
    private OutputChunkParser parser = new OutputChunkParser();

    @Override
    public void testSystemStarted(TestSystem testSystem) {

    }

    @Override
    public void testOutputChunk(String chunk) {
        if(!chunk.isEmpty()) {
            chunk = parser.embedImages(chunk);
        }
        output.append(chunk);
    }

    @Override
    public void testStarted(TestPage testPage) {

        String css = new String(getBytesForResource("/plaincss.css"));
        String js = new String(getBytesForResource("/javascript.js"));
        output = new StringBuilder();
        output.append("<html>\r\n")
                .append("<head>\r\n")
                .append("<style>\r\n")
                .append(css)
                .append("\r\n</style>\r\n")
                .append("</head>\r\n")
                .append("<body onload=\"enableClickHandlers()\">\r\n")
                .append("<script>\r\n")
                .append(js)
                .append("\r\n</script>\r\n")
                .append("<h1>").append(testPage.getFullPath()).append("</h1>\r\n");
    }

    private byte[] getBytesForResource(String resource) {
        byte[] result;
        try {
            result = ByteStreams.toByteArray(getClass().getResourceAsStream(resource));
        } catch (Exception e) {
            e.printStackTrace();
            result = "".getBytes();
        }
        return result;
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
        output.append("</body></html>");
    }

    @Override
    public void testSystemStopped(TestSystem testSystem, Throwable cause) {

    }

    @Override
    public void testAssertionVerified(Assertion assertion, TestResult testResult) {

    }

    @Override
    public void testExceptionOccurred(Assertion assertion, ExceptionResult exceptionResult) {

    }

    @Override
    public void close() throws IOException {

    }
}
