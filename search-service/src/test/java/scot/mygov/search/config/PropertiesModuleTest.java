package scot.mygov.search.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import scot.mygov.search.config.TestConfiguration.Inner;
import scot.mygov.search.config.TestConfiguration.Nested;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PropertiesModuleTest {

    private PropertiesModule module = new PropertiesModule('_');

    private TestConfiguration config = new TestConfiguration();

    private Map<String, String> properties = new LinkedHashMap<>();

    @Test
    public void serializeStringProperty() throws IOException {
        properties.put("PATH", "x");
        config = serialize(properties, TestConfiguration.class);
        assertEquals("x", config.getPath());
    }

    @Test
    public void serializeIntProperty() throws IOException {
        properties.put("integer", "1");
        config = serialize(properties, TestConfiguration.class);
        assertEquals(1, config.getInteger());
    }

    @Test
    public void serializeIgnoresMissingProperty() throws IOException {
        config = serialize(properties, TestConfiguration.class);
        assertNull(config.getPath());
    }

    @Test
    public void serializeNestedProperty() throws IOException {
        properties.put("nested_string", "x");
        config = serialize(properties, TestConfiguration.class);
        assertEquals("x", config.getNested().getString());
    }

    @Test
    public void serializeWithDotSeparator() throws IOException {
        module = new PropertiesModule('.');
        properties.put("nested.string", "x");
        config = serialize(properties, TestConfiguration.class);
        assertEquals("x", config.getNested().getString());
    }

    @Test
    public void serializeValuesAndNestedValues() throws IOException {
        module = new PropertiesModule('.');
        properties.put("integer", "1");
        properties.put("nested.integer", "2");
        properties.put("nested.string", "x");
        properties.put("other", "y");
        config = serialize(properties, TestConfiguration.class);
        assertEquals(1, config.getInteger());
        assertEquals(2, config.getNested().getInteger());
        assertEquals("x", config.getNested().getString());
        assertEquals("y", config.getOther());
    }

    private <T> T serialize(Map<String, String> map, Class<T> t) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        return mapper.convertValue(map, t);
    }

    @Test
    public void deserializeStringProperty() throws IOException {
        config.setPath("x");
        properties = deserialize(config);
        assertEquals("x", properties.get("PATH"));
    }

    @Test
    public void deserializeIntProperty() throws IOException {
        config.setInteger(1);
        properties = deserialize(config);
        assertEquals("1", properties.get("integer"));
    }

    @Test
    public void deserializeIgnoresIgnorableNullProperty() throws IOException {
        properties = deserialize(config);
        assertNull(properties.get("PATH"));
    }

    @Test
    public void deserializeNestedProperty() throws IOException {
        config.setNested(new Nested());
        config.getNested().setString("x");
        properties = deserialize(config);
        assertEquals("x", properties.get("nested_string"));
    }

    @Test
    public void deserializeWithDotSeparator() throws IOException {
        module = new PropertiesModule('.');
        config.setNested(new Nested());
        config.getNested().setString("x");
        properties = deserialize(config);
        assertEquals("x", properties.get("nested.string"));
    }

    @Test
    public void deserialiseWithTwoNestedProperties() throws IOException {
        module = new PropertiesModule('.');
        config.setNested(new Nested());
        config.setInner(new Inner());
        config.getNested().setString("x");
        config.getInner().setI(1);
        properties = deserialize(config);
        assertEquals("x", properties.get("nested.string"));
        assertEquals("1", properties.get("inner.i"));
    }

    private Map<String, String> deserialize(Object obj) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        return mapper.convertValue(obj, Map.class);
    }

}
