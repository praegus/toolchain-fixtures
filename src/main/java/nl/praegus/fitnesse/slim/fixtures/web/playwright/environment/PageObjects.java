package nl.praegus.fitnesse.slim.fixtures.web.playwright.environment;

import java.util.HashMap;
import java.util.Map;

public class PageObjects {
    private final static PageObjects INSTANCE = new PageObjects();
    private final Map<String, Map<String,Object>> pageObjects = new HashMap<>();
    /**
     * @return singleton instance.
     */
    public static PageObjects getInstance() {
        return INSTANCE;
    }

    public void storePageObject(String name, Map<String, Object> pageObject) {
        pageObjects.put(name, pageObject);
    }

    public Map<String, Object> getPageObject(String name) {
        return pageObjects.get(name);
    }
}
