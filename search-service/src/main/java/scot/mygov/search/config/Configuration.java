package scot.mygov.search.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

public class Configuration<T> {

    private final T configuration;

    private final Map<String, String> properties;

    private final Set<ConstraintViolation<Object>> violations;

    public Configuration(
            T configuration,
            Map<String, String> properties,
            Set<ConstraintViolation<Object>> violations) {
        this.configuration = configuration;
        this.properties = properties;
        this.violations = violations;
    }

    public T getConfiguration() {
        return configuration;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Set<ConstraintViolation<Object>> getViolations() {
        return violations;
    }

    public String toString() {
        return properties.entrySet().stream()
                .map(Object::toString)
                .collect(joining("\n  ", "Configuration:\n  ", ""));
    }

    public static JsonNode merge(JsonNode left, JsonNode right) {
        Iterator<Map.Entry<String, JsonNode>> fields = right.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String fieldName = entry.getKey();
            JsonNode value = entry.getValue();
            JsonNode node = left.get(fieldName);

            if (node != null && node.isObject()) {
                merge(node, value);
            } else if (left instanceof ObjectNode) {
                ((ObjectNode) left).set(fieldName, value);
            }
        }
        return left;
    }

    private static Map<String, String> environment(Map<String, String> properties, String application) {
        if (application == null) {
            return properties;
        }
        String prefix = application + "_";
        Map<String, String> result = new LinkedHashMap<>(properties);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix)) {
                result.put(key.substring(prefix.length()), entry.getValue());
            }
        }
        return result;
    }

    public static <T> Configuration<T> create(T empty, String prefix) throws IOException {
        return create(empty, prefix, System.getenv());
    }

    public static <T> Configuration<T> load(T empty, String prefix) {
        try {
            return create(empty, prefix, System.getenv());
        } catch (IOException ex) {
            throw new RuntimeException(ex); // NOSONAR
        }
    }

    public Configuration<T> validate() {
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> format("%s: %s", v.getPropertyPath(), v.getMessage()))
                    .collect(joining("\n"));
            throw new IllegalArgumentException("Invalid configuration: \n" + message);
        }
        return this;
    }

    public static <T> Configuration<T> create(T empty, String prefix, Map<String, String> overrides) throws IOException {
        Class<T> clazz = (Class<T>) empty.getClass();

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new PropertiesModule('_'));

        Map<String, String> map = environment(overrides, prefix);
        InputStream is = clazz.getResourceAsStream("/application.yml");
        JsonNode defaults = is != null ? mapper.readTree(is) : JsonNodeFactory.instance.objectNode();
        JsonNode environment = mapper.convertValue(map, JsonNode.class);
        JsonNode emptyNode = mapper.convertValue(empty, JsonNode.class);
        JsonNode merged = asList(defaults, environment).stream().reduce(emptyNode, Configuration::merge);

        T effective = mapper.convertValue(merged, clazz);
        Map<String, String> properties = mapper.convertValue(effective, Map.class);

        Set<ConstraintViolation<Object>> violations = Validation
                .buildDefaultValidatorFactory()
                .getValidator()
                .validate(effective);

        return new Configuration(effective, properties, violations);
    }

}
