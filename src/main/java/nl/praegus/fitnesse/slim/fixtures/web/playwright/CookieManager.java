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

    public void setCookie(Map<String, String> cookieMap, BrowserContext browserContext) {
        List<Cookie> cookies = new ArrayList<>();
        var cookie = new Cookie(cookieMap.get("name"), cookieMap.get("value"));

        if (cookieMap.get("domain") != null) {
            cookie.setDomain(cookieMap.get("domain"))
                    .setPath(cookieMap.getOrDefault("path", "/"));
        } else  {
            cookie.setUrl(cookieMap.get("url"));
        }

        cookie.setExpires(timestampToEpoch(cookieMap.getOrDefault("expiry", "2080-11-15 21:12")))
                .setSecure(Boolean.parseBoolean(cookieMap.getOrDefault("secure", "false")))
                .setHttpOnly(Boolean.parseBoolean(cookieMap.getOrDefault("httpOnly", "false")))
                .setSameSite(SameSiteAttribute.valueOf(cookieMap.getOrDefault("sameSite", "NONE")));
        cookies.add(cookie);
        browserContext.addCookies(cookies);
    }

    public void setCookies(List<Map<String, String>> cookiesList, BrowserContext browserContext) {
        cookiesList.stream().forEach(cookie -> setCookie(cookie, browserContext));
    }

    Map<String, String> getCookies(BrowserContext browserContext) {
        Map<String, String> cookieStrings = new HashMap<>();
        browserContext.cookies().forEach(cookie -> cookieStrings.put(cookie.name, this.formatCookieString(cookie)));
        return cookieStrings;
    }

    public void deleteCookies(BrowserContext browserContext) {
        browserContext.clearCookies();
    }

    private double timestampToEpoch(String timestamp) {
        return LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).toEpochSecond(ZoneOffset.UTC);
    }

    private String formatCookieString(Cookie cookie) {
        return String.format("%s;%s;%s;%s;%s", cookie.value, cookie.expires, cookie.secure, cookie.httpOnly, cookie.sameSite);
    }
}
