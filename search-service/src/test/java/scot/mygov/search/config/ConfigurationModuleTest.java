package scot.mygov.search.config;

import org.junit.Test;
import scot.mygov.search.config.TestConfiguration.Nested;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigurationModuleTest {

    private String prefix;

    private Map<String, String> overrides = new HashMap<>();

    private TestConfiguration config = new TestConfiguration();

    @Test
    public void usesEnvironmentByDefaultIfNoOverrides() throws IOException {
        config = Configuration.create(config, null).getConfiguration();
        assertTrue(config.getPath().contains(System.getProperty("path.separator")));
    }

    @Test
    public void nullPrefixAllowed() throws IOException {
        overrides.put("other", "o");
        config = create();
        assertEquals("o", config.getOther());
    }

    @Test
    public void useDefaultIfValueNotOverridden() throws IOException {
        config = new TestConfiguration();
        config.setInteger(1);
        config = create();
        assertEquals(1, config.getInteger());
    }

    @Test
    public void useOverrideValue() throws IOException {
        overrides.put("integer", "2");
        config.setInteger(1);
        config = create();
        assertEquals(2, config.getInteger());
    }

    @Test
    public void useOverrideForInnerClass() throws IOException {
        overrides.put("nested_string", "override");
        Nested nested = new Nested();
        nested.setString("default");
        config.setNested(nested);
        config = create();
        assertEquals("override", config.getNested().getString());
    }

    @Test
    public void usePrefixedValue() throws IOException {
        overrides.put("app_integer", "2");
        prefix = "app";
        config.setInteger(1);
        config = create();
        assertEquals(2, config.getInteger());
    }

    @Test
    public void prefixedValueTakesPrecedence() throws IOException {
        overrides.put("app_integer", "2");
        overrides.put("integer", "3");
        prefix = "app";
        config.setInteger(1);
        config = create();
        assertEquals(2, config.getInteger());
    }

    @Test
    public void propertiesReturnsDefaultValues() throws IOException {
        config.setInteger(1);
        Nested nested = new Nested();
        nested.setString("default");
        config.setNested(nested);
        Map<String, String> properties =
                Configuration.create(config, null, overrides).getProperties();
        assertEquals("1", properties.get("integer"));
        assertEquals("default", properties.get("nested_string"));
    }

    @Test
    public void propertiesReturnsOverriddenValues() throws IOException {
        overrides.put("integer", "2");
        config.setInteger(1);
        Map<String, String> properties =
                Configuration.create(config, null, overrides).getProperties();
        assertEquals("2", properties.get("integer"));
    }

    @Test
    public void ignoreIfPropertyIsDelimiter() throws IOException {
        overrides.put("_", "2");
        config = create();
    }

    private TestConfiguration create() throws IOException {
        return Configuration.create(config, prefix, overrides)
                .getConfiguration();
    }

}
