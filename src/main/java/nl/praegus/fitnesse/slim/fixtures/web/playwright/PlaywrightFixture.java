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

    public void setTimeout(Double timeoutInMilliseconds) {
        timeout = timeoutInMilliseconds;
        browserContext.setDefaultTimeout(timeout);
    }

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

    public void navigateTo(String url) {
        currentPage.navigate(url);
    }

    public void clickAndWaitForNavigation(String selector) {
        currentPage.waitForNavigation(() -> this.click(selector));
    }

    public void waitForSelector(String selector) {
        currentPage.waitForSelector(selector);
    }

    public void waitForSelectorHidden(String selector) {
        currentPage.waitForSelector(selector, new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN));
    }

    public void waitForSelectorPresent(String selector) {
        currentPage.waitForSelector(selector, new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));
    }

    public void waitForUrl(String url) {
        currentPage.waitForURL(url);
    }

    public Boolean isVisible(String selector) {
        return currentPage.isVisible(selector, new Page.IsVisibleOptions());
    }

    public Boolean isHidden(String selector) {
        return currentPage.isHidden(selector, new Page.IsHiddenOptions());
    }

    public void click(String selector) {
        currentPage.click(selector, new Page.ClickOptions());
    }

    public void clickTimes(int times, String selector) {
        for(int i=0; i<times; i++) {
            this.click(selector);
        }
    }
    public void doubleClick(String selector) {
        currentPage.dblclick(selector, new Page.DblclickOptions());
    }

    public void enterAs(String value, String selector) {
        currentPage.fill(selector, value, new Page.FillOptions());
    }

    public void selectIn(String value, String selector) {
        currentPage.selectOption(selector, new SelectOption().setLabel(value), new Page.SelectOptionOptions());
    }

    public boolean isEnabled(String selector) {
        return currentPage.isEnabled(selector, new Page.IsEnabledOptions());
    }

    public void press(String keyOrChord) {
        currentPage.keyboard().press(keyOrChord);
    }

    public void type(String text) {
        currentPage.keyboard().type(text);
    }

    public void goBack() {
        currentPage.goBack();
    }

    public String getUrl() {
        return currentPage.url();
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

    public void reloadPage() {
        currentPage.reload();
    }

    public String valueOf(String selector) {
        String result;
        switch (currentPage.evalOnSelector(selector, "e => e.tagName", null, new Page.EvalOnSelectorOptions()).toString().toLowerCase()) {
            case "input":
            case "textarea":
                result = currentPage.inputValue(selector, new Page.InputValueOptions());
                break;
            case "button":
            case "option":
            case "select":
                result = currentPage.evalOnSelector(selector, "e => e.value", null, new Page.EvalOnSelectorOptions()).toString();
                break;
            case "text":
                result = currentPage.innerHTML(selector, new Page.InnerHTMLOptions());
                break;
            default:
                result = currentPage.innerText(selector, new Page.InnerTextOptions());
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

    public static String getNormalizedText(String text) {
        return (text != null) ? Pattern.compile("[" + "\u00a0" + "\\s]+").matcher(text).replaceAll(" ") : null;
    }

    public Boolean isChecked(String selector) {
        return currentPage.isChecked(selector, new Page.IsCheckedOptions());
    }

    public void selectCheckbox(String selector) {
        currentPage.check(selector, new Page.CheckOptions());
    }

    public void forceSelectCheckbox(String selector) {
        currentPage.check(selector, new Page.CheckOptions().setForce(true));
    }

    public String takeScreenshot(String baseName) {
        var screenshotFile = new File(screenshotFolder, baseName + ".png");
        currentPage.screenshot(new Page.ScreenshotOptions().setPath(screenshotFile.toPath()).setFullPage(true));

        return String.format("<a href=\"%1$s\" target=\"_blank\"><img src=\"%1$s\" title=\"%2$s\" height=\"%3$s\"/></a>",
                getWikiUrl(screenshotFile.getAbsolutePath()), baseName, 200);
    }

    public String takeScreenshot() {
        return takeScreenshot(String.valueOf(Instant.now().toEpochMilli()));
    }

    /**
     * Calling pause() starts the PlayWright Inspector, but only when NOT running headless!
     * Scripts recorded in the Playwright Inspector can not be used in FitNesse, but the inspector might be useful
     * when finding and debugging selectors.
     */
    public void pause() {
        currentPage.pause();
    }

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

    public void openNewContext() {
        browserContext = browser.newContext();
    }

    public void open(String url) {
        this.currentPage = browserContext.newPage();
        navigateTo(url);
    }

    public void closePage() {
        currentPage.close();
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

    public boolean clickOnOpensTabWithUrl(String selector, String url) {
        return browserContext.waitForPage(() -> currentPage.click(selector, new Page.ClickOptions())).url().equals(url);
    }

    public void clickOnOpensTab(String selector) {
        this.currentPage = browserContext.waitForPage(() -> currentPage.click(selector));
    }

    public Boolean clickOnAndWaitOpensTabWithUrl(String selector, String url) {
        browserContext.waitForPage(() -> currentPage.click(selector)).waitForURL(url);
        return true;
    }

    public void closeContext() {
        browserContext.close();
    }

    public void acceptNextDialog() {
        currentPage.onceDialog(Dialog::accept);
    }

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

    public static Double toMilliSeconds(Integer timeoutInSeconds) {
        return (double) timeoutInSeconds * 1000;
    }

    public void openTrace() throws IOException, InterruptedException {
        openTrace("trace");
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
