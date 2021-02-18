package nl.praegus.fitnesse.slim.fixtures.web;

import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import nl.hsac.fitnesse.fixture.slim.StopTestException;
import nl.hsac.fitnesse.fixture.slim.web.BrowserTest;
import nl.hsac.fitnesse.slim.interaction.ReflectionHelper;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * An extension on BrowserTest that houses some power-user features
 */

public class WebBrowser extends BrowserTest<WebElement> {
    private final static Set<String> METHODS_NO_WAIT;
    static {
        METHODS_NO_WAIT = ReflectionHelper.validateMethodNames(BrowserTest.class,
                "open", "takeScreenshot", "location", "back", "forward", "refresh", "alertText", "confirmAlert",
                "dismissAlert", "openInNewTab", "ensureActiveTabIsNotClosed", "currentTabIndex", "tabCount",
                "ensureOnlyOneTab", "closeTab", "switchToNextTab", "switchToPreviousTab", "switchToDefaultContent",
                "switchToFrame", "switchToParentFrame", "secondsBeforeTimeout", "secondsBeforePageLoadTimeout", "waitForPage",
                "waitForTagWithText", "waitForClassWithText", "waitForClass", "waitForVisible", "waitSeconds",
                "waitMilliseconds", "waitMilliSecondAfterScroll", "screenshotBaseDirectory", "screenshotShowHeight",
                "setBrowserWidth", "setBrowserHeight", "setBrowserSizeToBy", "setBrowserSizeToMaximum", "setGlobalValueTo",
                "setSendCommandForControlOnMacTo", "sendCommandForControlOnMac", "isImplicitWaitForAngularEnabled",
                "setImplicitWaitForAngularTo", "globalValue", "clearSearchContext", "executeScript", "secondsBeforeTimeout",
                "secondsBeforePageLoadTimeout", "trimOnNormalize", "setImplicitFindInFramesTo", "setTrimOnNormalize",
                "setRepeatIntervalToMilliseconds", "repeatAtMostTimes", "repeatAtMostTimes", "timeSpentRepeating");
    }

    private int inputDelay = 0;
    private String progressIndicator;
    private boolean abortOnException;
    private boolean dumpConsoleOnException;
    private boolean blurAfterSendingText = false;

    /**
     * Dump the browser's console log (if available) below any exception when it occurs. This may help debugging front-end issues
     *
     * @param dump true to append the console log to the exception message. Defaults to false when not set.
     */
    public void dumpConsoleOnException(boolean dump) {
        dumpConsoleOnException = dump;
    }

    /**
     * Abort the test on any exception.
     *
     * @param abort true to convert any Exception to a StopTestException. Effectively aborting the test on the first timeout. Defaults to false when not set.
     */
    public void abortOnException(boolean abort) {
        abortOnException = abort;
    }

    /**
     * Get the currently configured input delay.
     *
     * @return the current delay in milliseconds.
     */
    public int getInputDelay() {
        return inputDelay;
    }

    /**
     * Set the input delay.
     *
     * @param inputDelay The desired delay in milliseconds. This will wait [inputDelay] milliseconds before each browser interaction or value extraction.
     *                   Effectively slowing your testscript.
     */
    public void setInputDelay(int inputDelay) {
        this.inputDelay = inputDelay;
    }

    /**
     * Returns the progress indicator locator currently set
     *
     * @return The locator String
     */
    public String getProgressIndicator() {
        return progressIndicator;
    }

    /**
     * Configure a progress indicator locator to wait for before any interaction/extraction
     *
     * @param spinner The locator (heuristic/css=/xpath=/id=/name=/etc.) to wait for.
     */
    public void setProgressIndicator(String spinner) {
        this.progressIndicator = spinner;
    }

    /**
     * Press tab after sending input
     *
     * @param blurAfterSendingText true to send a tab keypress after sendValue. Defaults to false when not set.
     */
    public void blurAfterSendingText(boolean blurAfterSendingText) {
        this.blurAfterSendingText = blurAfterSendingText;
    }

    /**
     * Get the current setting.
     *
     * @return true if set, false otherwise
     */
    public boolean blurAfterSendingText() {
        return blurAfterSendingText;
    }

    public WebBrowser() {
    }

    public WebBrowser(int secondsBeforeTimeout) {
        super(secondsBeforeTimeout);
    }

    public WebBrowser(int secondsBeforeTimeout, boolean confirmAlertIfAvailable) {
        super(secondsBeforeTimeout, confirmAlertIfAvailable);
    }

    /**
     * Override beforeInvoke to always wait for any spinner to be gone if it's there
     */
    @Override
    protected void beforeInvoke(Method method, Object[] arguments) {
        super.beforeInvoke(method, arguments);
        if (!METHODS_NO_WAIT.contains(method.getName())) {
            try {
                if (inputDelay > 0) {
                    waitMilliseconds(inputDelay);
                }
                if (progressIndicator != null && progressIndicator.trim().length() > 0) {
                    List<String> fullPath = new ArrayList<>(getCurrentSearchContextPath());
                    clearSearchContext();
                    waitUntil(webDriver -> isNotVisibleOnPage(progressIndicator));
                    refreshSearchContext(fullPath, Math.min(fullPath.size(), minStaleContextRefreshCount));
                }
            } catch (SlimFixtureException e) {
                String msg = e.getMessage();
                if (msg.startsWith("message:<<") && msg.endsWith(">>")) {
                    msg = msg.substring(10, msg.length() - 2);
                    msg = msg.replaceAll("Timed-out waiting ", "Timed out waiting for spinner to disappear ");
                }
                throw new SlimFixtureException(false, msg);
            }
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
                System.out.println("Resizing not allowed, mobile device or headless browser?");
            }
        }
    }

    @Override
    protected Throwable handleException(Method method, Object[] arguments, Throwable t) {
        Throwable result = super.handleException(method, arguments, t);
        if (dumpConsoleOnException) {
            result = new SlimFixtureException(false, 
                    result.getMessage().substring(10, result.getMessage().length() - 2) +
                    "<br /><b>Console Log:</b><br /><div style=\"font-size:0.8em; font-family: monospace;\">" +
                    consoleLog() +
                    "</div>",
                    t);
        }
        if (abortOnException) {
            String msg = result.getMessage();
            if (msg.startsWith("message:<<") && msg.endsWith(">>")) {
                msg = msg.substring(10, msg.length() - 2);
            }
            result = new StopTestException(false, msg);
        }
        return result;
    }

    @Override
    protected void sendValue(WebElement element, String value) {
        super.sendValue(element, value);
        if (blurAfterSendingText) {
            pressTab();
        }
    }

    /**
     * Get the vlue of a CSS property on an element
     *
     * @param property The property to return a value for
     * @param place    The element the property is on
     * @return The property value
     */
    public String valueOfCssPropertyOn(String property, String place) {
        WebElement element = getElement(place);
        return element.getCssValue(property);
    }

    /**
     * Get the vlue of a CSS property on an element
     *
     * @param property  The property to return a value for
     * @param place     The element the property is on
     * @param container the container element to limit the serach to
     * @return The property value
     */
    public String valueOfCssPropertyOnIn(String property, String place, String container) {
        WebElement element = getElement(place, container);
        return element.getCssValue(property);
    }

    /**
     * Get and clear the browser's console log
     *
     * @return the current console log, formatted as [Time] [LEVEL] [Message]. Every log on a new line.
     */
    public String consoleLog() {
        StringBuilder console = new StringBuilder();

        for (LogEntry log : getSeleniumHelper().driver().manage().logs().get(LogType.BROWSER)) {
            String instant = LocalDateTime.ofInstant(Instant.ofEpochMilli(log.getTimestamp()),
                    TimeZone.getDefault().toZoneId()).toString();
            console.append(String.format("%s [%s] %s\r\n", instant, log.getLevel().getName(), log.getMessage()));
        }
        return console.toString();
    }

    /**
     * Make sure no errors have occured in the browser's console
     *
     * @return true if the console log holds no messages with level SEVERE
     */
    public boolean noErrorsInBrowserConsole() {
        for (LogEntry log : getSeleniumHelper().driver().manage().logs().get(LogType.BROWSER)) {
            if (log.getLevel().intValue() >= 1000) {
                return false;
            }
        }
        return true;
    }

    /**
     * Make sure no warnings or errors have occured in the browser's console
     *
     * @return true if the console log holds no messages with level SEVERE or WARNING
     */
    public boolean noWarningsOrErrorsInBrowserConsole() {
        for (LogEntry log : getSeleniumHelper().driver().manage().logs().get(LogType.BROWSER)) {
            if (log.getLevel().intValue() >= 900) {
                return false;
            }
        }
        return true;
    }
}
