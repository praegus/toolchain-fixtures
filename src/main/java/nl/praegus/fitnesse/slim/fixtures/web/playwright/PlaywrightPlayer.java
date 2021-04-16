package nl.praegus.fitnesse.slim.fixtures.web.playwright;

import com.microsoft.playwright.*;
import nl.hsac.fitnesse.fixture.slim.FileFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import nl.praegus.fitnesse.slim.fixtures.web.playwright.environment.PageObjects;

import java.io.File;
import java.lang.reflect.Method;

import static com.microsoft.playwright.Page.ScreenshotOptions;

public class PlaywrightPlayer extends SlimFixture {
    private final PageObjects pageObjects = PageObjects.getInstance();
    private final Playwright playwright = Playwright.create();
    private Browser browser;
    private BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
    private final BrowserContext ctx;
    private Page currentPage;

    private final File screenshotFolder = new File(getEnvironment().getFitNesseFilesSectionDir(), "screenshots");
    private final String pageSourceFolder = getEnvironment().getFitNesseFilesSectionDir() + "/pagesource/";
    private final File downloadFolder = new File(getEnvironment().getFitNesseFilesSectionDir(), "downloads");
    private final FileFixture fileFixture = new FileFixture();



    public PlaywrightPlayer(String browserName) {
        startBrowser(browserName);
        ctx = browser.newContext(new Browser.NewContextOptions().setAcceptDownloads(true));
        currentPage = ctx.newPage();
    }

    public PlaywrightPlayer(String browserName, String url) {
        startBrowser(browserName);
        ctx = browser.newContext(new Browser.NewContextOptions().setAcceptDownloads(true));
        currentPage = ctx.newPage();
        navigateTo(url);
    }

    public void setHeadless(Boolean setHeadless) {
        this.launchOptions.setHeadless(setHeadless);
    }

    private void startBrowser(String browserName) {
        switch (browserName.toLowerCase()) {
            case "chromium":
                browser = playwright.chromium().launch(launchOptions);
                break;
            case "firefox":
                browser = playwright.firefox().launch(launchOptions);
                break;
            case "webkit":
                browser = playwright.webkit().launch(launchOptions);
                break;
            default:
                throw new SlimFixtureException(false, "Unsupported browser name. Use chromium, firefox or webkit!");
        }
    }

    public void timeoutInSeconds(Integer timeoutInSeconds) {
        currentPage.setDefaultTimeout(toMilliSeconds(timeoutInSeconds));
    }

    public void navigationTimeoutInSeconds(Integer timeoutInSeconds) {
        currentPage.setDefaultNavigationTimeout(toMilliSeconds(timeoutInSeconds));
    }

    public void navigationTimeout(Integer timeoutInSeconds) {
        currentPage.setDefaultNavigationTimeout(toMilliSeconds(timeoutInSeconds));
    }

    public void navigateTo(String url) {
        currentPage.navigate(url);
    }

    public void waitForNavigationAfterClicking(String pageObject) {
        currentPage.waitForNavigation(() -> click(pageObject));
    }

    public void waitForSelector(String pageObject) {
        currentPage.waitForSelector(getSelector(pageObject));
    }

    public void click(String pageObject) {
        currentPage.click(getSelector(pageObject));
    }

    public void doubleClick(String pageObject) {
        currentPage.dblclick(getSelector(pageObject));
    }

    public void enterAs(String value, String pageObject) {
        currentPage.fill(getSelector(pageObject), value);
    }

    public void press(String keyOrChord) {
        currentPage.keyboard().press(keyOrChord);
    }

    public String valueOf(String pageObject) {
        String selector = getSelector(pageObject);
        String tagName = currentPage.evalOnSelector(selector, "e => e.tagName").toString();
        Object result;
        if ("input".equalsIgnoreCase(tagName)
                || "button".equalsIgnoreCase(tagName)
                || "option".equalsIgnoreCase(tagName)
                || "select".equalsIgnoreCase(tagName)) {
            result = currentPage.evalOnSelector(selector, "e => e.value");
        } else {
            result = currentPage.innerText(selector);
        }
        return result.toString();
    }

    public String textValueOf(String pageObject) {
        String selector = getSelector(pageObject);
        return currentPage.textContent(selector);
    }

    public String takeScreenshot(String baseName) {
        File screenshotFile = new File(screenshotFolder, baseName + ".png");
        ScreenshotOptions opts = new ScreenshotOptions().setPath(screenshotFile.toPath());
        currentPage.screenshot(opts);
        return String.format("<a href=\"%1$s\" target=\"_blank\"><img src=\"%1$s\" title=\"%2$s\" height=\"%3$s\"/></a>",
                getWikiUrl(screenshotFile.getAbsolutePath()), baseName, 200);
    }

    public void uploadFileFor(String file, String pageObject) {
        String filePath = getEnvironment().getFilePathFromWikiUrl(file);
        currentPage.onFileChooser(fileChooser ->
                fileChooser.setFiles(new File(filePath).toPath()));
        click(pageObject);
    }

    public String downloadFileByClicking(String pageObject) {
        return saveDownloadedFileAsByClicking("", pageObject);
    }

    public String saveDownloadedFileAsByClicking(String filename, String pageObject) {
        Download dl = currentPage.waitForDownload(() -> currentPage.click(getSelector(pageObject)));

        String dlFilename = filename.length() > 0 ? filename : dl.suggestedFilename();
        File downloadedFile = new File(downloadFolder, dlFilename);

        dl.saveAs(downloadedFile.toPath());
        return String.format("<a href=\"%1s\" target=\"_blank\">%2s</a>", getWikiUrl(downloadedFile.getAbsolutePath()), dlFilename);
    }

    public String savePageSource(String baseName) {
        return fileFixture.createContaining(pageSourceFolder + baseName + ".html", currentPage.content());
    }

    public void switchToNextTab() {
        int index = ctx.pages().indexOf(currentPage);
        if (ctx.pages().size() > index + 1) {
            currentPage = ctx.pages().get(index + 1);
        } else {
            currentPage = ctx.pages().get(0);
        }
        currentPage.bringToFront();
    }

    public void switchToPreviousTab() {
        int index = ctx.pages().indexOf(currentPage);
        if (index > 0) {
            currentPage = ctx.pages().get(index - 1);
        } else {
            currentPage = ctx.pages().get(ctx.pages().size() - 1);
        }
        currentPage.bringToFront();
    }

    public void closeTab() {
        currentPage.close();
        currentPage = ctx.pages().get(0);
        currentPage.bringToFront();
    }

    public void closeBrowser() {
        browser.close();
    }

    private String getSelector(String pageObject) {
        try {
            String pageObjectName = pageObject.split("\\.")[0];
            String element = pageObject.split("\\.")[1];

            Object selector = pageObjects.getPageObject(pageObjectName).get(element);

            if (selector != null) {
                return selector.toString();
            } else {
                return pageObject;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return pageObject;
        }
    }

    private Integer toMilliSeconds(Integer timeoutInSeconds) {
        return timeoutInSeconds * 1000;
    }

    @Override
    protected Throwable handleException(Method method, Object[] arguments, Throwable t) {
        if (t instanceof PlaywrightException) {
            String m = t.getMessage();
            t = new SlimFixtureException(false, "<div>" + m + "</div>");
        }
        return t;
    }


}
