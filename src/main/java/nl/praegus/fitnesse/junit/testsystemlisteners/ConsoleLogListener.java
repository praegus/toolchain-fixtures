package nl.praegus.fitnesse.junit.testsystemlisteners;

import fitnesse.testsystems.*;
import nl.praegus.fitnesse.junit.testsystemlisteners.util.OutputChunkParser;

import java.io.Closeable;

public class ConsoleLogListener implements TestSystemListener, Closeable {
    private final String NEWLINE = System.getProperty("line.separator");
    private final OutputChunkParser parser = new OutputChunkParser();


    @Override
    public void testSystemStarted(TestSystem testSystem) {
    }

    @Override
    public void testOutputChunk(String output) {
        output = parser.filterCollapsedSections(output);
        if (!output.isEmpty()) {
            output = parser.rewriteHashTables(output);
            output = parser.formatHtmlForConsole(output);
            output = parser.sanitizeRemainingHtml(output);
            output = parser.applyConsoleColoring(output);

            if(!output.endsWith(NEWLINE)) {
                output += NEWLINE;
            }
            System.out.println(output);
        }
    }

    @Override
    public void testStarted(TestPage testPage) {
        System.out.println("\r\nTest Started: " + testPage.getFullPath());
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
        System.out.println(parser.printSummary(testPage.getFullPath(), testSummary.toString()));
    }

    @Override
    public void testSystemStopped(TestSystem testSystem, Throwable cause) {
        //System.setOut(defaultSysOut);
    }

    @Override
    public void testAssertionVerified(Assertion assertion, TestResult testResult) {
    }

    @Override
    public void testExceptionOccurred(Assertion assertion, ExceptionResult exceptionResult) {

    }

    @Override
    public void close() {
    }
}
