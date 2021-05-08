package nl.praegus.fitnesse.slim.fixtures.web.playwright;

import nl.hsac.fitnesse.fixture.slim.SlimFixtureWithMap;
import nl.praegus.fitnesse.slim.fixtures.web.playwright.environment.PageObjects;

import java.lang.reflect.Method;

public class PageObject extends SlimFixtureWithMap {
    private final String name;
    private final PageObjects pageObjects = PageObjects.getInstance();

    public PageObject(String name) {
        super();
        this.name = name;
    }

    @Override
    protected Object afterCompletion(Method method, Object[] arguments, Object result) {
        pageObjects.storePageObject(name, getCurrentValues());
        return super.afterCompletion(method, arguments, result);
    }
}
