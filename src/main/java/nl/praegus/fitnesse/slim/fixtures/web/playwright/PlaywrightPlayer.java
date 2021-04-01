package nl.praegus.fitnesse.slim.fixtures.web.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import nl.hsac.fitnesse.fixture.util.FileUtil;
import nl.praegus.fitnesse.slim.fixtures.web.playwright.environment.PageObjects;

import java.io.File;
import java.lang.reflect.Method;

import static com.microsoft.playwright.Page.ClickOptions;
import static com.microsoft.playwright.Page.DblclickOptions;
import static com.microsoft.playwright.Page.FillOptions;
import static com.microsoft.playwright.Page.ScreenshotOptions;

public class PlaywrightPlayer extends SlimFixture {
    private final PageObjects pageObjects = PageObjects.getInstance();
    private final Playwright playwright = Playwright.create();
    private final File screenshotFolder = new File(getEnvironment().getFitNesseFilesSectionDir(), "screenshots");
    private final File pageSourceFolder = new File(getEnvironment().getFitNesseFilesSectionDir(), "pagesources");
    private final File downloadFolder = new File(getEnvironment().getFitNesseFilesSectionDir(), "downloads");
    private final BrowserContext ctx;
    private Browser browser;
    private Page currentPage;
    private int timeoutInSeconds = 10;

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

    private void startBrowser(String browserName) {
        switch (browserName.toLowerCase()) {
            case "chromium":
                browser = playwright.chromium().launch();
                break;
            case "firefox":
                browser = playwright.firefox().launch();
                break;
            case "webkit":
                browser = playwright.webkit().launch();
                break;
            default:
                throw new SlimFixtureException(false, "Unsupported browser name. Use chromium, firefox or webkit!");
        }
    }

    public void navigateTo(String url) {
        currentPage.navigate(url);
    }

    public void click(String pageObject) {
        ClickOptions cOpts = new ClickOptions().setTimeout(timeoutInSeconds * 1000);
        currentPage.click(getSelector(pageObject), cOpts);
    }

    public void doubleClick(String pageObject) {
        DblclickOptions cOpts = new DblclickOptions().setTimeout(timeoutInSeconds * 1000);
        currentPage.dblclick(getSelector(pageObject), cOpts);
    }

    public void enterAs(String value, String pageObject) {
        FillOptions fOpts = new FillOptions().setTimeout(timeoutInSeconds * 1000);
        currentPage.fill(getSelector(pageObject), value, fOpts);
    }

    public void press(String keyOrChord) {
        currentPage.keyboard().press(keyOrChord);
    }

    public String valueOf(String pageObject) {
        String selector = getSelector(pageObject);
        String tagName = currentPage.evalOnSelector(selector, "e => e.tagName").toString();
        Object result;
        if ("input".equals(tagName.toLowerCase()) ||
                "button".equals(tagName.toLowerCase()) ||
                "option".equals(tagName.toLowerCase()) ||
                "select".equals(tagName.toLowerCase())) {
            result = currentPage.evalOnSelector(selector, "e => e.value");
        } else {
            result = currentPage.innerText(selector);
        }
        return result.toString();
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
        currentPage.onFileChooser(fileChooser -> {
            fileChooser.setFiles(new File(filePath).toPath());
        });
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
        File pageSource = new File(pageSourceFolder, baseName + ".html");
        FileUtil.writeFile(pageSource.getAbsolutePath(), currentPage.content());
        return String.format("<a href=\"%1s\" target=\"_blank\">%2s</a>", getWikiUrl(pageSource.getAbsolutePath()), pageSource.getName());
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

    public void timeoutInSeconds(int timeout) {
        this.timeoutInSeconds = timeout;
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

    @Override
    protected Throwable handleException(Method method, Object[] arguments, Throwable t) {
        if (t instanceof PlaywrightException) {
            String m = t.getMessage();
            t = new SlimFixtureException(false, "<div>" + m + "</div>");
        }
        return t;
    }
}
