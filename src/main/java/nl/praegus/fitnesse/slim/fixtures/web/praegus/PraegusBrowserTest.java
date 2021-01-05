package nl.praegus.fitnesse.slim.fixtures.web.praegus;

import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import nl.hsac.fitnesse.fixture.slim.StopTestException;
import nl.hsac.fitnesse.slim.interaction.ReflectionHelper;
import nl.hsac.fitnesse.fixture.slim.web.BrowserTest;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Method;

/**
 * Custom Praegus fixture class
 */

public class PraegusBrowserTest<T extends WebElement> extends BrowserTest<T> {

    private boolean blurAfterSendingText = false;
    private boolean abortOnException = false;

    public void blurAfterSendingText(boolean blurAfterSendingText) {
        this.blurAfterSendingText = blurAfterSendingText;
    }

    public boolean blurAfterSendingText() {
        return blurAfterSendingText;
    }

    public void abortOnException(boolean abort) {
        abortOnException = abort;
    }

    private String progressIndicator = "id=mask-img";
    private final static Set<String> METHODS_NO_WAIT;
    static {
        METHODS_NO_WAIT = ReflectionHelper.validateMethodNames(PraegusBrowserTest.class, "open", "takeScreenshot",
                "location", "back", "forward", "refresh", "alertText", "confirmAlert", "dismissAlert", "openInNewTab",
                "ensureActiveTabIsNotClosed", "currentTabIndex", "tabCount", "ensureOnlyOneTab", "closeTab",
                "switchToNextTab", "switchToPreviousTab", "switchToDefaultContent", "switchToFrame",
                "switchToParentFrame", "secondsBeforeTimeout", "secondsBeforePageLoadTimeout", "waitForPage",
                "waitForTagWithText", "waitForClassWithText", "waitForClass", "waitForVisible", "waitSeconds",
                "waitMilliseconds", "waitMilliSecondAfterScroll", "screenshotBaseDirectory", "screenshotShowHeight",
                "setBrowserWidth", "setBrowserHeight", "setBrowserSizeToBy", "setBrowserSizeToMaximum",
                "setGlobalValueTo", "setSendCommandForControlOnMacTo", "sendCommandForControlOnMac",
                "isImplicitWaitForAngularEnabled", "setImplicitWaitForAngularTo", "globalValue", "clearSearchContext",
                "executeScript", "secondsBeforeTimeout", "secondsBeforePageLoadTimeout", "trimOnNormalize",
                "setImplicitFindInFramesTo", "setTrimOnNormalize", "setRepeatIntervalToMilliseconds",
                "repeatAtMostTimes", "repeatAtMostTimes", "timeSpentRepeating");
    }

    /**
     * Override beforeInvoke to always wait for any spinner to be gone if it's there
     */
    @Override
    protected void beforeInvoke(Method method, Object[] arguments) {
        super.beforeInvoke(method, arguments);
        if (!METHODS_NO_WAIT.contains(method.getName())) {
            // waitMilliseconds(inputDelay);
            try {
                List<String> fullPath = new ArrayList<>(getCurrentSearchContextPath());
                clearSearchContext();
                waitUntil(webDriver -> isNotVisibleOnPage(progressIndicator));
                refreshSearchContext(fullPath, Math.min(fullPath.size(), minStaleContextRefreshCount));
            } catch (SlimFixtureException e) {
                String msg = e.getMessage();
                if (msg.startsWith("message:<<") && msg.endsWith(">>")) {
                    msg = msg.substring(10, msg.length() - 2);
                    msg = msg.replaceAll("Timed-out waiting ", "Timed out waiting for progress indicator to disappear ");
                }
                throw new SlimFixtureException(false, msg);
            }
        }
    }

    public String progressIndicator() {
        return progressIndicator;
    }

    public void setprogressIndicator(String progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    @Override
    protected void sendValue(WebElement element, String value) {
        super.sendValue(element, value);
        if (blurAfterSendingText) {
            pressTab();
        }
    }

    @Override
    public void setBrowserSizeToMaximum() {
        try {
            super.setBrowserSizeToMaximum();
        } catch (WebDriverException e) {
            try {
                setBrowserHeight(900);
                setBrowserWidth(1800);
                System.out.println("Tried to maximize a browser that didn't allow that, tried setting 1800x900 now");
            } catch (WebDriverException f) {
                System.out.println("Resizing not allowed, mobile device?");
            }
        }
    }

    @Override
    protected Throwable handleException(Method method, Object[] arguments, Throwable t) {
        Throwable result = super.handleException(method, arguments, t);
        if (abortOnException) {
            String msg = result.getMessage();
            if (msg.startsWith("message:<<") && msg.endsWith(">>")) {
                msg = msg.substring(10, msg.length() - 2);
            }
            result = new StopTestException(false, msg);
        }
        return result;
    }
}
