package nl.praegus.fitnesse.junit.testsystemlisteners;

import fitnesse.testsystems.*;
import nl.praegus.fitnesse.junit.testsystemlisteners.util.ConsoleOutputChunkParser;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class ConsoleLogListener implements TestSystemListener, Closeable {
    private final String NEWLINE = System.getProperty("line.separator");
    private final ConsoleOutputChunkParser parser = new ConsoleOutputChunkParser();
    private final DateTimeFormatter timeFmt =
            DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
                    .withLocale( Locale.getDefault() )
                    .withZone( ZoneId.systemDefault() );

    private final OutputStream out;

    public ConsoleLogListener() throws UnsupportedEncodingException {
        out = new BufferedOutputStream( System.out );
    }

    private void writeln(String line) {
        try {
            out.write(line.getBytes());
            out.write('\n');
        } catch (IOException e) {
            System.out.println(line);
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

            if(!output.endsWith(NEWLINE)) {
                output += NEWLINE;
            }
            writeln(output);
        }
    }

    @Override
    public void testStarted(TestPage testPage) {
        writeln("\r\n" + timeFmt.format(Instant.now()) + " - Test Started: " + testPage.getFullPath());
    }

    @Override
    public void testComplete(TestPage testPage, TestSummary testSummary) {
        writeln(parser.printSummary(testPage.getFullPath(), testSummary.toString()));
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
