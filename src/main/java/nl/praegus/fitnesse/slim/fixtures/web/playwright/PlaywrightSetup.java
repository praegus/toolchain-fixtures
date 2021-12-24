package nl.praegus.fitnesse.slim.fixtures.web.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ColorScheme;
import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;

import java.util.Arrays;
import java.util.Map;

public final class PlaywrightSetup extends SlimFixture {
    private static final Playwright playwright = Playwright.create();
    private static Browser browser;
    private static BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
    private static Browser.NewContextOptions newContextOptions = new Browser.NewContextOptions();

    public void setHeadless(Boolean headless) {
        launchOptions.setHeadless(headless);
    }

    public static void startBrowser(String browserName) {
        switch (browserName.toLowerCase()) {
            case "chromium":
                browser = playwright.chromium().launch(launchOptions.setArgs(Arrays.asList("--disable-dev-shm-usage")));
                break;
            case "firefox":
                browser = playwright.firefox().launch(launchOptions);
                break;
            case "webkit":
                browser = playwright.webkit().launch(launchOptions);
                break;
            default:
                throw new SlimFixtureException(false, "Unsupported browser name. Use Chromium, Firefox or Webkit!");
        }
    }

    public void setAcceptDownloads(Boolean acceptDownloads){
        newContextOptions.setAcceptDownloads(acceptDownloads);
    }

    public void setBypassCSP(Boolean bypassCSP){
        newContextOptions.setBypassCSP(bypassCSP);
    }

    public void setColorScheme(String colorScheme){
        newContextOptions.setColorScheme(ColorScheme.valueOf(colorScheme.toUpperCase()));
    }

    public void setExtraHTTPHeaders(Map<String, String> extraHTTPHeaders) {
        newContextOptions.setExtraHTTPHeaders(extraHTTPHeaders);
    }

    public void setBaseUrl(String baseUrl){
        newContextOptions.setBaseURL(baseUrl);
    }

    public static void setDeviceScaleFactor(int scaleFactor){
        newContextOptions.setDeviceScaleFactor(scaleFactor);
    }

    public static void setViewportWidthAndHeight(int width, int height){
        newContextOptions.setViewportSize(width,height);
    }

    public static void setStrict(Boolean isStrict){
        newContextOptions.setStrictSelectors(isStrict);
    }

    public static Browser.NewContextOptions getNewContextOptions() {
        return newContextOptions;
    }

    public static Browser getBrowser() {
        return browser;
    }

    public void closeBrowser() {
        browser.close();
    }

    public void closePlaywright() {
        playwright.close();
    }
}
