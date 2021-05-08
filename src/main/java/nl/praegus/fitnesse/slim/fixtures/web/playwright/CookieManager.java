package nl.praegus.fitnesse.slim.fixtures.web.playwright;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.SameSiteAttribute;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CookieManager {
    private BrowserContext browserContext;

    public CookieManager(BrowserContext browserContext) {
        this.browserContext = browserContext;
    }

    void setCookie(Map<String, String> cookieMap) {
        List<Cookie> cookies = new ArrayList<>();
        var cookie = new Cookie(cookieMap.get("name"), cookieMap.get("value"));
        cookie.setUrl(cookieMap.get("url"))
                .setExpires(timestampToEpoch(cookieMap.getOrDefault("expiry", "2080-11-15 21:12")))
                .setSecure(Boolean.parseBoolean(cookieMap.getOrDefault("secure", "false")))
                .setHttpOnly(Boolean.parseBoolean(cookieMap.getOrDefault("httpOnly", "false")))
                .setSameSite(SameSiteAttribute.valueOf(cookieMap.getOrDefault("sameSite", "NONE")));
        cookies.add(cookie);
        browserContext.addCookies(cookies);
    }

    void setCookies(List<Map<String, String>> cookiesList) {
        cookiesList.stream().forEach(this::setCookie);
    }

    Map<String, String> getCookies() {
        Map<String, String> cookieStrings = new HashMap<>();
        browserContext.cookies().forEach(cookie -> cookieStrings.put(cookie.name, this.formatCookieString(cookie)));
        return cookieStrings;
    }

    void deleteCookies() {
        browserContext.clearCookies();
    }

    double timestampToEpoch(String timestamp) {
        return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).toEpochSecond(ZoneOffset.UTC);
    }

    String formatCookieString(Cookie cookie) {
        return String.format("%s;%s;%s;%s;%s", cookie.value, cookie.expires, cookie.secure, cookie.httpOnly, cookie.sameSite);
    }
}
