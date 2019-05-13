package nl.praegus.fitnesse.slim.fixtures.web.mendix;

import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import nl.hsac.fitnesse.fixture.slim.web.BrowserTest;
import nl.hsac.fitnesse.fixture.slim.web.annotation.TimeoutPolicy;
import nl.hsac.fitnesse.fixture.slim.web.annotation.WaitUntil;
import nl.hsac.fitnesse.fixture.util.selenium.SeleniumHelper;
import nl.hsac.fitnesse.slim.interaction.ReflectionHelper;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Fixture class customized to test mendix web apps.
 */

public class MendixBrowserTest extends BrowserTest<WebElement> {
    private final static Set<String> METHODS_NO_WAIT;
    static {
        METHODS_NO_WAIT = ReflectionHelper.validateMethodNames(BrowserTest.class,
                "open",
                "takeScreenshot",
                "location",
                "back",
                "forward",
                "refresh",
                "alertText",
                "confirmAlert",
                "dismissAlert",
                "openInNewTab",
                "ensureActiveTabIsNotClosed",
                "currentTabIndex",
                "tabCount",
                "ensureOnlyOneTab",
                "closeTab",
                "switchToNextTab",
                "switchToPreviousTab",
                "switchToDefaultContent",
                "switchToFrame",
                "switchToParentFrame",
                "secondsBeforeTimeout",
                "secondsBeforePageLoadTimeout",
                "waitForPage",
                "waitForTagWithText",
                "waitForClassWithText",
                "waitForClass",
                "waitForVisible",
                "waitSeconds",
                "waitMilliseconds",
                "waitMilliSecondAfterScroll",
                "screenshotBaseDirectory",
                "screenshotShowHeight",
                "setBrowserWidth",
                "setBrowserHeight",
                "setBrowserSizeToBy",
                "setBrowserSizeToMaximum",
                "setGlobalValueTo",
                "setSendCommandForControlOnMacTo",
                "sendCommandForControlOnMac",
                "isImplicitWaitForAngularEnabled",
                "setImplicitWaitForAngularTo",
                "globalValue",
                "clearSearchContext",
                "executeScript",
                "secondsBeforeTimeout",
                "secondsBeforePageLoadTimeout",
                "trimOnNormalize",
                "setImplicitFindInFramesTo",
                "setTrimOnNormalize",
                "setRepeatIntervalToMilliseconds",
                "repeatAtMostTimes",
                "repeatAtMostTimes",
                "timeSpentRepeating");
    }

    private boolean waitForJquery = false;
    private int jqueryTimeout = 5; //Timeout in seconds
    private SeleniumHelper seleniumHelper = getEnvironment().getSeleniumHelper();
    private int delayBeforeValue = 0;
    private int inputDelay = 0;


    /**
     * Override beforeInvoke to always wait for any spinner to be gone if it's there
     */
    @Override
    protected void beforeInvoke(Method method, Object[] arguments) {
        super.beforeInvoke(method, arguments);
        waitForJqueryIfNeeded();
        if (!METHODS_NO_WAIT.contains(method.getName())) {
            try {
                waitUntil(webDriver -> isNotVisibleOnPage("css=.mx-progress-indicator"));
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

    public void waitForJquery(boolean wait) {
        waitForJquery = wait;
    }

    public void setJqueryTimeout(int timeout) {
        jqueryTimeout = timeout;
    }

    public void setInputDelay(int delay) {
        inputDelay = delay;
    }

    public int getInputDelay() {
        return inputDelay;
    }

    @Override
    public void setBrowserSizeToMaximum() {
        try {
            super.setBrowserSizeToMaximum();
        } catch (WebDriverException e) {
            try{
                setBrowserHeight(900);
                setBrowserWidth(1800);
                System.out.println("Tried to maximize a browser that didn't allow that, tried setting 1800x900 now");
            } catch (WebDriverException f) {
                System.out.println("Resizing not allowed, mobile device?");
            }
        }
    }

    @WaitUntil(TimeoutPolicy.RETURN_NULL)
    public Integer numberOfItems(String xPath) {
        if (xPath.startsWith("xpath=")) {
            xPath = xPath.substring(6);
        }
        return getSeleniumHelper().driver().findElements(By.xpath(xPath)).size();
    }

    @Override
    public boolean selectForIn(String value, String place, String container) {
        clickIn(place, container);
        return super.selectForIn(value, place, container);
    }

    @Override
    public boolean selectFor(String value, String place) {
        click(place);
        return super.selectFor(value, place);
    }


    public boolean clickItems(String listOfItems) {
        String[] items = listOfItems.split(";");
        for (String item : items) {
            if (!click(item.trim())) {
                return false;
            }
        }
        return true;
    }

    public String getCsrfToken() {
        return seleniumHelper.executeJavascript("return window.mx.session.sessionData.csrftoken").toString();
    }

    public void delayValueExtractionByMilliseconds(int millis) {
        delayBeforeValue = millis;
    }

    @WaitUntil(TimeoutPolicy.RETURN_NULL)
    public String valueOfInRowWhereIsInTable(String requestedColumnName, String selectOnColumn, String selectOnValue, String table) {
        //Search in the right table
        String xpathForHeadTable = xpathForTable(table, "head");
        String xpathForBodyTable = xpathForTable(table, "body");

        int selectColumnIndex = columnIndex(xpathForHeadTable, selectOnColumn);
        int requestedColumnIndex = columnIndex(xpathForHeadTable, requestedColumnName);

        String xpath = String.format("%s//td[%s][contains(.,'%s')]/parent::tr/td[%s]", xpathForBodyTable, selectColumnIndex, selectOnValue, requestedColumnIndex);
        WebElement element = seleniumHelper.findByXPath(xpath);
        String result = valueFor(element);
        if (result.length() == 0) {
            result = element.getAttribute("title");
        }
        return result;
    }

    @WaitUntil(TimeoutPolicy.RETURN_NULL)
    public String valueOfInRowWhereIs(String requestedColumnName, String selectOnColumn, String selectOnValue) {

        WebElement element = elementInRowWhereIs(requestedColumnName, selectOnColumn, selectOnValue);
        if(element != null) {
            String result = valueFor(element);
            if (result.length() == 0) {
                result = element.getAttribute("title");
            }
            return result;
        }
        return null;
    }

    @Override
    @WaitUntil(TimeoutPolicy.RETURN_NULL)
    public String valueOfInRowNumber(String requestedColumnName, int rowNumber) {
        String xpathForHeadTable = "//table[contains(@class, 'mx-datagrid-head-table')]";
        String xpathForBodyTable = "//table[contains(@class, 'mx-datagrid-body-table')]";

        int columnIndex = columnIndex(xpathForHeadTable, requestedColumnName);

        String xpath = String.format("%s//tr[%d]/td[%d]", xpathForBodyTable, rowNumber, columnIndex);

        WebElement element = seleniumHelper.findByXPath(xpath);
        if(element != null) {
            String result = valueFor(element);
            if (result.length() == 0) {
                result = element.getAttribute("title");
            }
            return result;
        }
        return null;

    }

    @Override
    public boolean clickInRowWhereIs(String requestedColumnName, String selectOnColumn, String selectOnValue) {
        return clickElement(elementInRowWhereIs(requestedColumnName, selectOnColumn, selectOnValue));
    }

    private WebElement elementInRowWhereIs(String requestedColumnName, String selectOnColumn, String selectOnValue) {
        //Search in the right table
        String xpathForHeadTable = "//table[contains(@class, 'mx-datagrid-head-table')]";
        String xpathForBodyTable = "//table[contains(@class, 'mx-datagrid-body-table')]";

        int selectColumnIndex = columnIndex(xpathForHeadTable, selectOnColumn);
        int requestedColumnIndex = columnIndex(xpathForHeadTable, requestedColumnName);

        String xpath = String.format("%s//td[%s][contains(.,'%s')]/parent::tr/td[%s]", xpathForBodyTable, selectColumnIndex, selectOnValue, requestedColumnIndex);

        return seleniumHelper.findByXPath(xpath);
    }

    private String xpathForTable(String table, String type) {
        int i = 1;
        String xpath = "";
        boolean displayed = false;
        WebElement el;
        while (!displayed) {
            xpath = String.format("(//*[text()='%s']/ancestor::div[@class='row']//table[contains(@class, '%s-table')])[%s]", table, type, String.valueOf(i));
            el = seleniumHelper.findByXPath(xpath);
            if (el == null) {
                xpath = String.format("//*[text()='%s']/ancestor::div[@class='row']//table[contains(@class, '%s-table')]", table, type);
                break;
            } else {
                displayed = el.isDisplayed();
                i++;
            }
        }
        return xpath;
    }

    private int columnIndex(String tableXpath, String columnName) {
        int precedingColumns = findElements(By.xpath(String.format("%s//th[contains(.,'%s')]/preceding-sibling::th", tableXpath, columnName))).size();
        return precedingColumns + 1;
    }

    @Override
    @WaitUntil(TimeoutPolicy.RETURN_NULL)
    public String valueOf(String place) {
        waitMilliseconds(delayBeforeValue);
        return super.valueOf(place);
    }

    @Override
    protected boolean enter(String value, String place, String container, boolean shouldClear) {
        WebElement element = getElementToSendValue(place, container);
        boolean result = element != null && isInteractable(element);

        if (result) {
            if (shouldClear) {
                element.clear();
            }
            waitMilliseconds(inputDelay);
            sendValue(element, value);
        }
        return result;
    }


    private void waitForJqueryIfNeeded() {
        if(waitForJquery) {
            try{
                int originalTimeout = secondsBeforeTimeout();
                secondsBeforeTimeout(jqueryTimeout);
                ExpectedCondition<Object> condition = webDriver -> jQueryIsDone();

                waitUntil(condition);

                secondsBeforeTimeout(originalTimeout);
            } catch (WebDriverException e) {
                // ignore. Probably no jQ.
            }
        }
    }

    private boolean jQueryIsDone() {
        boolean result = (Boolean) executeScript("return window.jQuery.active === 0");
        return result;
    }
}