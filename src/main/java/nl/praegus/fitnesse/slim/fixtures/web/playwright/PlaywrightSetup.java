package nl.praegus.fitnesse.slim.fixtures.web.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;

public final class PlaywrightSetup extends SlimFixture {
    private static final Playwright playwright = Playwright.create();
    private static Browser browser;
    private static BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();

    public void setHeadless(Boolean headless) {
        launchOptions.setHeadless(headless);
    }

    public static void startBrowser(String browserName) {
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
                throw new SlimFixtureException(false, "Unsupported browser name. Use Chromium, Firefox or Webkit!");
        }
    }

    public static Browser getBrowser() {
        return browser;
    }

}