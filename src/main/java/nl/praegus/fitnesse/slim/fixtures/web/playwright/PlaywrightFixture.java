package nl.praegus.fitnesse.slim.fixtures.web.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class PlaywrightFixture extends SlimFixture {
    private final Browser browser = PlaywrightSetup.getBrowser();
    private BrowserContext browserContext = browser.newContext(PlaywrightSetup.getNewContextOptions());
    private final CookieManager cookieManager = new CookieManager();
    private Page currentPage = browserContext.newPage();
    private final File screenshotFolder = new File(getEnvironment().getFitNesseFilesSectionDir(), "screenshots");
    private final File tracesFolder = new File(getEnvironment().getFitNesseFilesSectionDir(), "traces");
    private String storageState;
    private Double timeout;

    public BrowserContext getBrowserContext() {
        return browserContext;
    }

    private Locator getLocator(String selector) {
        return currentPage.locator(selector);
    }

    /**
     * Sets the timeout for the current browser context.
     *
     * @param timeoutInMilliseconds
     */
    public void setTimeout(Double timeoutInMilliseconds) {
        timeout = timeoutInMilliseconds;
        browserContext.setDefaultTimeout(timeout);
    }

    //Page management
    public void openNewContext() {
        browserContext = browser.newContext();
    }

    public void closePage() {
        currentPage.close();
    }

    public void closeContext() {
        browserContext.close();
    }

    public void acceptNextDialog() {
        currentPage.onceDialog(Dialog::accept);
    }

    //     Tab management
    public void switchToNextTab() {
        currentPage = browserContext.pages().get(getPageIndex(currentPage) + 1);
        currentPage.bringToFront();
    }

    public void switchToPreviousTab() {
        int currentPageIndex = getPageIndex(currentPage);
        currentPage = currentPageIndex > 0 ? browserContext.pages().get(currentPageIndex - 1) : currentPage;
        currentPage.bringToFront();
    }

    public void closeCurrentTab() {
        var tabToCloseIndex = getPageIndex(currentPage);
        switchToPreviousTab();
        browserContext.pages().get(tabToCloseIndex).close();
    }

    public void closeNextTab() {
        var tabToCloseIndex = (getPageIndex(currentPage) + 1);
        browserContext.pages().get(tabToCloseIndex).close();
    }

    /**
     * Returns index of given Page in Pages list of given BrowserContext. Returns -1 if not found.
     *
     * @param page
     * @return
     */
    private Integer getPageIndex(Page page) {
        return browserContext.pages().indexOf(page);
    }

    //Cookie management
    public void setCookie(Map<String, String> cookieMap) {
        cookieManager.setCookie(cookieMap, browserContext);
    }

    public void setCookies(List<Map<String, String>> cookiesList) {
        cookieManager.setCookies(cookiesList, browserContext);
    }

    public Map<String, String> getCookies() {
        return cookieManager.getCookies(browserContext);
    }

    public void deleteCookies() {
        cookieManager.deleteCookies(browserContext);
    }

    //Navigation
    public void navigateTo(String url) {
        currentPage.navigate(url);
    }

    public void open(String url) {
        this.currentPage = browserContext.newPage();
        navigateTo(url);
    }

    public void goBack() {
        currentPage.goBack();
    }

    public void reloadPage() {
        currentPage.reload();
    }

    //User page interaction
    public void click(String selector) {
        getLocator(selector).click();
    }

    public void clickTimes(int times, String selector) {
        for (int i = 0; i < times; i++) {
            this.click(selector);
        }
    }

    public void clickAndWaitForNavigation(String selector) {
        currentPage.waitForNavigation(() -> this.click(selector));
    }

    public void doubleClick(String selector) {
        getLocator(selector).dblclick();
    }

    public void enterAs(String value, String selector) {
        getLocator(selector).fill(value);
    }

    public void selectIn(String value, String selector) {
        getLocator(selector).selectOption(new SelectOption().setLabel(value));
    }

    public void selectCheckbox(String selector) {
        getLocator(selector).check();
    }

    public void forceSelectCheckbox(String selector) {
        getLocator(selector).check(new Locator.CheckOptions().setForce(true));
    }

    //Keyboard interaction
    public void press(String keyOrChord) {
        currentPage.keyboard().press(keyOrChord);
    }

    public void type(String text) {
        currentPage.keyboard().type(text);
    }

    //Waiting stuff
    public void waitForUrl(String url) {
        currentPage.waitForURL(url);
    }

    public void waitForVisible(String selector) {
        getLocator(selector).waitFor();
    }

    public void waitForHidden(String selector) {
        getLocator(selector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    public void waitForPresentInDom(String selector) {
        getLocator(selector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
    }

    public void waitForNetworkIdle() {
        currentPage.waitForLoadState(LoadState.NETWORKIDLE);
    }

    public void waitForMilliseconds(Double timeout) {
        currentPage.waitForTimeout(timeout);
    }

    public void waitForSeconds(Integer timeout) {
        waitForMilliseconds(toMilliSeconds(timeout));
    }

    public void waitForNavigation() {
        currentPage.waitForNavigation(() -> {
        });
    }

    //Assertions
    public boolean isVisible(String selector) {
        return getLocator(selector).isVisible();
    }

    public boolean isHidden(String selector) {
        return getLocator(selector).isHidden();
    }

    public boolean isEnabled(String selector) {
        return getLocator(selector).isEnabled();
    }

    public boolean isChecked(String selector) {
        return getLocator(selector).isChecked();
    }

    public boolean clickOnOpensTabWithUrl(String selector, String url) {
        return browserContext.waitForPage(() -> getLocator(selector).click(new Locator.ClickOptions())).url().equals(url);
    }

    public boolean clickOnAndWaitOpensTabWithUrl(String selector, String url) {
        browserContext.waitForPage(() -> getLocator(selector).click()).waitForURL(url);
        return true;
    }

    //Value retrieval
    public String valueOf(String selector) {
        String result;
        switch (getLocator(selector).evaluate("e => e.tagName", null, new Locator.EvaluateOptions()).toString().toLowerCase()) {
            case "input":
            case "textarea":
                result = getLocator(selector).inputValue();
                break;
            case "button":
            case "option":
            case "select":
                result = getLocator(selector).evaluate("e => e.value", null, new Locator.EvaluateOptions()).toString();
                break;
            case "text":
                result = getLocator(selector).innerHTML();
                break;
            default:
                result = getLocator(selector).innerText();
        }
        return result;
    }

    public String valueOfAttributeForSelector(String attributeName, String selector) {
        return currentPage.getAttribute(selector, attributeName);
    }

    public String selectedOptionLabel(String selector) {
        var selectedIndex = currentPage.evalOnSelector(selector, "e => e.selectedIndex");
        return currentPage.evalOnSelector(selector, String.format("e => e.options[%s].innerText", selectedIndex)).toString();
    }

    public String normalizedValueOf(String selector) {
        return getNormalizedText(valueOf(selector).trim());
    }

    public String getNormalizedText(String text) {
        return (text != null) ? Pattern.compile("[" + "\u00a0" + "\\s]+").matcher(text).replaceAll(" ") : null;
    }

    public String getUrl() {
        return currentPage.url();
    }

    //Taking screenshots
    public String takeScreenshot(String baseName) {
        var screenshotFile = new File(screenshotFolder, baseName + ".png");
        currentPage.screenshot(new Page.ScreenshotOptions().setPath(screenshotFile.toPath()).setFullPage(true));

        return String.format("<a href=\"%1$s\" target=\"_blank\"><img src=\"%1$s\" title=\"%2$s\" height=\"%3$s\"/></a>",
                getWikiUrl(screenshotFile.getAbsolutePath()), baseName, 200);
    }

    public String takeScreenshot() {
        return takeScreenshot(String.valueOf(Instant.now().toEpochMilli()));
    }

    //Debugging

    /**
     * Calling pause() starts the PlayWright Inspector, but only when NOT running headless!
     * Scripts recorded in the Playwright Inspector can not be used in FitNesse, but the inspector might be useful
     * when finding and debugging selectors.
     */
    public void pause() {
        currentPage.pause();
    }

    public String getCurrentPage() {
        return currentPage.toString();
    }

    public String getPages() {
        return browserContext.pages().toString();
    }

    public String getContexts() {
        return browser.contexts().toString();
    }

    public String getCurrentContext() {
        return browserContext.toString();
    }

    //Manage re-usable state
    public void saveStorageState() {
        storageState = browserContext.storageState();
    }

    public String getStorageState() {
        return storageState;
    }

    public void openNewContextWithSavedStorageState() {
        this.browserContext = browser.newContext(PlaywrightSetup.getNewContextOptions().setStorageState(getStorageState()));
        setTimeout(timeout);
    }

    //Tracing
    public void startTrace() {
        browserContext.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
    }

    public void saveTrace(String name) {
        browserContext.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(tracesFolder + "/" + name + ".zip")));
    }

    public void saveTrace() {
        saveTrace("trace");
    }

    public void openTrace(String name) throws IOException, InterruptedException {
        String[] args = {"show-trace", tracesFolder + "/" + name + ".zip"};
        CLI.main(args);
    }

    public void openTrace() throws IOException, InterruptedException {
        openTrace("trace");
    }

    //Network
    public void clickAndWaitForResponseFromUrl(String selector, String url) {
        currentPage.waitForResponse(url, () -> this.click(selector));
    }

    public void enterAsAndWaitForResponseFromUrl(String value, String selector, String url) {
        currentPage.waitForResponse(url, () -> this.enterAs(value, selector));
    }

    public void waitForResponseFromUrl(String url) {
        currentPage.waitForResponse(url, () -> {
        });
    }

    public void waitForResponseFromUrlMatching(String urlRegex) {
        currentPage.waitForResponse(Pattern.compile(urlRegex), () -> {
        });
    }

    //Utility
    public static Double toMilliSeconds(Integer timeoutInSeconds) {
        return (double) timeoutInSeconds * 1000;
    }

    @Override
    protected Throwable handleException(Method method, Object[] arguments, Throwable t) {
        if (t instanceof PlaywrightException) {
            t = new SlimFixtureException(false, getSlimFixtureExceptionMessageWithScreenshot(t));
        }
        return t;
    }

    protected String getSlimFixtureExceptionMessageWithScreenshot(Throwable t) {
        return String.format("<div>%s</div><div>%s</div>", t.getMessage(), takeScreenshot());
    }
}