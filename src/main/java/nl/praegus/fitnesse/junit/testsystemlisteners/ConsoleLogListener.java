package nl.praegus.fitnesse.junit.testsystemlisteners;

import fitnesse.testsystems.Assertion;
import fitnesse.testsystems.ExceptionResult;
import fitnesse.testsystems.TestPage;
import fitnesse.testsystems.TestResult;
import fitnesse.testsystems.TestSummary;
import fitnesse.testsystems.TestSystem;
import fitnesse.testsystems.TestSystemListener;
import nl.praegus.fitnesse.junit.testsystemlisteners.util.ConsoleOutputChunkParser;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class ConsoleLogListener implements TestSystemListener, Closeable {
    private final String NEWLINE = System.getProperty("line.separator");
    private final ConsoleOutputChunkParser parser = new ConsoleOutputChunkParser();
    private final DateTimeFormatter timeFmt =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneId.systemDefault());

    private final BufferedOutputStream out;

    public ConsoleLogListener() {
        out = new BufferedOutputStream(System.out);
    }

    private void writeln(String line) {
        try {
            out.write(line.getBytes());
            out.write('\n');
        } catch (IOException e) {
            System.out.println(line);
        }
    }

    private void flushTestOutputToConsole() {
        try {
            out.flush();
        } catch (IOException e) {
            System.err.println("Error writing test output: " + e.getMessage());
        }
    }

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

            if (!output.endsWith(NEWLINE)) {
                output += NEWLINE;
            }
            writeln(output);
        }
    }

    @Override
    public void testStarted(TestPage testPage) {
        writeln("\r\n" + "[OUTPUT] for: " + testPage.getFullPath());
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
        writeln(parser.printSummary(testPage.getFullPath(), testSummary.toString()));
        flushTestOutputToConsole();
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
    public void close() {
    }
}
