package nl.praegus.fitnesse.junit.testsystemlisteners.util;

import fitnesse.FitNesse;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class WikitextJSFormatter {

    public static String formatWikiText(String wikiText) {
        final String wikiFormatScript = "fitnesse/resources/javascript/WikiFormatter.js";
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("JavaScript");

        try{
            ClassLoader fitNesseClassLoader = FitNesse.class.getClassLoader();
            InputStreamReader formatterReader = new InputStreamReader(
                    fitNesseClassLoader.getResourceAsStream(wikiFormatScript), StandardCharsets.UTF_8);
            engine.eval(new BufferedReader(formatterReader));
            Object formatter = engine.eval("new WikiFormatter()");

            return ((Invocable) engine).invokeMethod(formatter, "format", wikiText).toString();
        } catch(ScriptException | NoSuchMethodException e) {
            System.err.println(e.getMessage());
        }
    return wikiText;
    }

}
