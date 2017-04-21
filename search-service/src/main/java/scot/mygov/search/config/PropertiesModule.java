package scot.mygov.search.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

class PropertiesModule extends SimpleModule {

    public PropertiesModule(char delimiter) {
        addSerializer(Map.class, new ConfigurationSerializer(delimiter));
        addDeserializer(Map.class, new ConfigurationDeserializer(delimiter));
    }

    /**
     * Serialize a map of configuration values into a Jackson object.
     */
    static class ConfigurationSerializer extends JsonSerializer<Map> {

        private String split;

        public ConfigurationSerializer(char delimiter) {
            if (".$|()[{^?*+\\".indexOf(delimiter) == -1) {
                split = String.valueOf(delimiter);
            } else {
                split = "\\" + delimiter;
            }
        }

        @Override
        public void serialize(Map map, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();

            // Build and traverse TreeMap to ensure keys are visited in sort order
            Map<String, String> treeMap = new TreeMap<>(map);
            List<String> prev = new ArrayList<>();
            for (Map.Entry<String, String> entry : treeMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                List<String> segments = asList(key.split(split));
                if (segments.isEmpty()) {
                    continue;
                }
                List<String> curr = segments.subList(0, segments.size() - 1);

                int prefix = commonPrefixLength(prev, curr);
                for (int i = 0; i < prev.size() - prefix; i++) {
                    gen.writeEndObject();
                }
                for (int i = 0; i < curr.size() - prefix; i++) {
                    String fieldName = curr.get(prefix + i);
                    gen.writeObjectFieldStart(fieldName);
                }

                String name = segments.get(segments.size() - 1);
                gen.writeStringField(name, String.valueOf(value));
                prev = curr;
            }
            for (int i = 0; i < prev.size(); i++) {
                gen.writeEndObject();
            }
            gen.writeEndObject();
        }

        private <T> int commonPrefixLength(List<T> prev, List<T> curr) {
            int max = Math.min(curr.size(), prev.size());
            int prefix = 0;
            while (prefix < max && curr.get(prefix).equals(prev.get(prefix))) {
                prefix++;
            }
            return prefix;
        }

    }

    /**
     * Deserialize a configuration object into a map of configuration values.
     */
    static class ConfigurationDeserializer extends JsonDeserializer {

        private final String delimiter;

        public ConfigurationDeserializer(char delimiter) {
            this.delimiter = String.valueOf(delimiter);
        }

        @Override
        public Object deserialize(JsonParser parser, DeserializationContext ctxt)
                throws IOException {
            Map<String, String> map = new LinkedHashMap<>();
            List<String> path = new ArrayList<>();
            for (JsonToken token = parser.nextToken(); token != null; token = parser.nextToken()) {
                if (token.equals(JsonToken.FIELD_NAME)) {
                    path.add(parser.getCurrentName());
                } else if (token.isScalarValue()) {
                    String string = Objects.toString(parser.getValueAsString(), "");
                    map.put(path.stream().collect(joining(delimiter)), string);
                    path.remove(path.size() - 1);
                } else if (token.isStructEnd() && !path.isEmpty()) {
                    path.remove(path.size() - 1);
                }
            }
            return map;
        }

    }

}
