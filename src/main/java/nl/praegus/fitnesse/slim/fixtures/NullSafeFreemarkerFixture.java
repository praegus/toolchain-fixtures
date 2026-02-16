package nl.praegus.fitnesse.slim.fixtures;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonWriter;
import freemarker.template.Template;
import nl.hsac.fitnesse.fixture.slim.FreemarkerFixture;
import nl.hsac.fitnesse.fixture.util.FreeMarkerHelper;
import nl.hsac.fitnesse.fixture.util.JsonFormatter;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NullSafeFreemarkerFixture extends FreemarkerFixture {
    private final NullSafeFreeMarkerHelper fmHelper = new NullSafeFreeMarkerHelper();
    private final NullSerializingJsonFormatter jsonFormatter = new NullSerializingJsonFormatter();

    public NullSafeFreemarkerFixture() {
        super();
    }

    public NullSafeFreemarkerFixture(String defaultTemplate) {
        super(defaultTemplate);
    }

    public Map<String, Object> copyMap() {
        return new LinkedHashMap<>(getCurrentValues());
    }

    /**
     * Stores null value.
     * @param name name to use this value for.
     */
    public void setNullValueFor(String name) {
        setValueFor(null, name);
    }

    /**
     * Stores null value in map.
     * @param name name to use this value for.
     * @param map map to store value in.
     */
    public void setNullValueForIn(String name, Map<String, Object> map) {
        getMapHelper().setValueForIn(null, name, map);
    }

    @Override
    public String applyTemplate(String aTemplate) {
        String result = fmHelper.processTemplate(
                getEnvironment().getTemplate(aTemplate),
                getCurrentValues());
        result = postProcess(result);
        result = formatResult(aTemplate, result);
        return result;
    }

    @Override
    protected String formatResult(String aTemplate, String result) {
        if (aTemplate.contains(".json")) {
            try {
                String formatted = jsonFormatter.format(result);
                if (formatted != null) {
                    result = "<pre>" + org.apache.commons.text.StringEscapeUtils.escapeHtml4(formatted) + "</pre>";
                }
            } catch (Exception e) {
                // can not be formatted, return raw result
            }
        } else {
            result = super.formatResult(aTemplate, result);
        }
        return result;
    }

    /**
     * FreeMarkerHelper that replaces null values with a sentinel before template processing.
     * FreeMarker's DefaultObjectWrapper makes null map values invisible,
     * so nulls are replaced with a sentinel to preserve them in output.
     */
    private static class NullSafeFreeMarkerHelper extends FreeMarkerHelper {
        /**
         * Sentinel value used to represent null in FreeMarker templates.
         */
        public static final String NULL_VALUE = "__FMNull__";

        @Override
        public String processTemplate(Template t, Object model) {
            Object processedModel = replaceNulls(model);
            return super.processTemplate(t, processedModel);
        }

        @SuppressWarnings("unchecked")
        private Object replaceNulls(Object value) {
            if (value instanceof Map) {
                Map<String, Object> original = (Map<String, Object>) value;
                Map<String, Object> result = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : original.entrySet()) {
                    Object v = entry.getValue();
                    result.put(entry.getKey(), v == null ? NULL_VALUE : replaceNulls(v));
                }
                return result;
            }
            if (value instanceof List) {
                List<Object> original = (List<Object>) value;
                List<Object> result = new ArrayList<>(original.size());
                for (Object item : original) {
                    result.add(item == null ? NULL_VALUE : replaceNulls(item));
                }
                return result;
            }
            return value;
        }
    }

    /**
     * JsonFormatter that serializes null values in JSON output.
     */
    private static class NullSerializingJsonFormatter extends JsonFormatter {
        private final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

        @Override
        protected void toJson(JsonElement jsonElement, Writer writer) throws JsonIOException {
            try {
                JsonWriter jsonWriter = this.newJsonWriter(writer);
                gson.toJson(jsonElement, jsonWriter);
            } catch (IOException e) {
                throw new JsonIOException(e);
            }
        }
    }
}
