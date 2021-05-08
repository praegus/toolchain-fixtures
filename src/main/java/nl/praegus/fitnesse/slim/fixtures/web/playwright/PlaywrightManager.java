package nl.praegus.fitnesse.slim.fixtures.web.playwright;

import com.microsoft.playwright.Browser;

public final class PlaywrightManager {

    private Browser browser;

    public void setBrowser(){
        this.browser = browser;
    }

    public Browser getBrowser() {
        return browser;
    }

}
