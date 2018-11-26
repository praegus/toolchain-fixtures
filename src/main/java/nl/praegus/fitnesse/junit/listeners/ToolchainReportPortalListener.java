package nl.praegus.fitnesse.junit.listeners;

import com.epam.reportportal.junit.JUnitInjectorProvider;
import com.epam.reportportal.listeners.Statuses;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Report portal custom event listener. This listener is intended to be used with FitNesse's
 * FitNesseRunner (to run FitNesse pages as Junit tests
 *
 * @author Tom Heintzberger - Based on parallel junit runlistener by Aliaksei_Makayed
 */

public class ToolchainReportPortalListener extends RunListener {
    private int testCount = 0;
    private ToolchainRunHandler handler = JUnitInjectorProvider.getInstance().getBean(ToolchainRunHandler.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void testStarted(Description description) {
        if (testCount == 0) { handler.startLaunch(); }
        handler.startSuiteIfRequired(description);
        handler.startTestMethod(description);
        testCount++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFinished(Description description) {
            handler.attachWikiPage(description);
            handler.stopTestMethod(description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testFailure(Failure failure) {
        handler.clearRunningItemId();
        handler.sendReportPortalMsg(failure);
        handler.markCurrentTestMethod(failure.getDescription(), Statuses.FAILED);
        handler.handleTestSkip(failure.getDescription());
    }

    public void testAssumptionFailure(Failure failure) {
        this.testFailure(failure);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testIgnored(Description description) {
        handler.addToFinishedMethods(description);
    }

     public void testRunFinished(Result result) {
        handler.stopAllSuites();
        handler.stopLaunch();
    }


}
