package scot.mygov.search.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dagger.ObjectGraph;
import io.undertow.Undertow;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.finance.tradukisto.ValueConverters;
import scot.mygov.search.Search;
import scot.mygov.search.SearchApplication;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class SearchIT {

    private static final Logger logger = LoggerFactory.getLogger(SearchIT.class);

    private static final JsonNodeFactory factory = JsonNodeFactory.instance;

    private static final String PROPERTIES = "/project.properties";

    private static final String NUMBERS = "numbers";

    private static Search.Server server;

    private static Client client = ClientBuilder.newClient();

    private static URI elasticsearch;

    private static URI application;

    @BeforeClass
    public static void setUpClass() throws IOException {
        setUpServer();
        setUpIndex();
    }

    public static void setUpServer() throws IOException {
        UriBuilder localhost = UriBuilder.fromUri("http://localhost");

        int elasticPort = elasticsearchPort();
        elasticsearch = localhost.port(elasticPort).build();

        Map<String, String> config = new HashMap<>();
        URI url = UriBuilder.fromUri(elasticsearch).path(NUMBERS).build();
        int port = 8081;
        config.put("index", url.toString());
        ObjectGraph graph = ObjectGraph.create(new TestSearchModule(config));
        SearchApplication app = graph.get(SearchApplication.class);

        server = new Search.Server();
        server.deploy(app);
        server.start(Undertow.builder()
                .addHttpListener(port, localhost.build().getHost()));
        int applicationPort = server.port();
        application = localhost.port(applicationPort).build();

        logger.info("Application port:  {}", applicationPort);
        logger.info("Elasticsearch port: {}", elasticPort);
    }

    private static int elasticsearchPort() throws IOException {
        try (InputStream is = SearchIT.class.getResourceAsStream(PROPERTIES)) {
            Properties props = new Properties();
            props.load(is);
            String portString = props.getProperty("elasticsearch.http.port");
            return Integer.parseInt(portString, 10);
        }
    }

    public static void setUpIndex() throws IOException {
        deleteIndex();
        createIndex();
        indexSomeNumbers();
    }

    private static void deleteIndex() throws IOException {
        Response response = client.target(elasticsearch)
                .path(NUMBERS)
                .request()
                .head();
        response.close();
        if (response.getStatus() == 200) {
            logger.info("Deleting index");
            client.target(elasticsearch)
                    .path(NUMBERS)
                    .request()
                    .delete(Object.class);
        }
    }

    private static void createIndex() throws IOException {
        logger.info("Creating index");
        URL mapping = SearchIT.class.getResource("/mapping.json");
        String json = IOUtils.toString(mapping, "UTF-8");
        client.target(elasticsearch)
                .path(NUMBERS)
                .request()
                .put(Entity.json(json), Object.class);
    }

    private static void indexSomeNumbers() throws IOException {
        logger.info("Indexing test data");
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        ValueConverters converter = ValueConverters.ENGLISH_INTEGER;
        int max = 20;
        for (int i = 1; i <= max; i++) {
            ObjectNode node = factory.objectNode();
            ObjectNode index = node.putObject("index");
            index.put("_id", String.valueOf(i));
            index.put("_index", NUMBERS);
            mapper.writeValue(writer, node);
            writer.append('\n');

            ObjectNode item = factory.objectNode();
            item.put("number", i);
            item.put("words", converter.asWords(i));
            item.set("tags", factory.arrayNode().addAll(
                    fizzBuzzTags(i).stream()
                            .map(factory::textNode)
                            .collect(toList())));
            mapper.writeValue(writer, item);
            writer.append('\n');
        }
        String body = writer.toString();
        client.target(elasticsearch)
                .path("_bulk")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(body, "application/x-ndjson"), Object.class);
        client.target(elasticsearch)
                .path("_refresh")
                .request()
                .post(null, Object.class);
    }

    private static List<String> fizzBuzzTags(int i) {
        if (i % 3 == 0 && i % 5 == 0) {
            return asList("fizz", "buzz");
        } else if (i % 3 == 0) {
            return singletonList("fizz");
        } else if (i % 5 == 0) {
            return singletonList("buzz");
        }
        return emptyList();
    }

    private static void postTemplate(String name, URL templateUrl) throws IOException {
        logger.info("Posting search template {}", name);
        String templateString = IOUtils.toString(templateUrl, "UTF-8");
        ObjectNode body = factory.objectNode();
        ObjectNode script = factory.objectNode();
        script.put("lang", "mustache");
        script.put("source", templateString);
        body.set("script", script);
        client.target(elasticsearch)
                .path("_scripts/{name}")
                .resolveTemplate("name", name)
                .request()
                .post(Entity.json(body), Object.class);
    }

    @Test
    public void queryByTemplate() throws IOException {
        postTemplate("fizzbuzz", SearchIT.class.getResource("/query.json"));
        JsonNode params = factory.objectNode()
                .set("tags", factory.arrayNode().add("fizz").add("buzz"));

        Response response = client.target(application)
                .path("fizzbuzz")
                .request()
                .post(Entity.json(params));

        JsonNode tree = response.readEntity(JsonNode.class);
        response.close();
        assertEquals(200, response.getStatus());
        assertEquals(0, tree.path("_shards").path("failed").asInt(-1));
        assertEquals(15, tree
                .path("hits")
                .path("hits")
                .path(0)
                .path("_source")
                .path("number")
                .asInt());
    }

    @Test
    public void completion() throws IOException {
        postTemplate("completion", SearchIT.class.getResource("/completion.json"));
        Response response = client.target(application)
                .path("completion")
                .queryParam("term", "fo")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        JsonNode tree = response.readEntity(JsonNode.class);
        response.close();

        assertEquals(200, response.getStatus());
        assertEquals(0, tree.get("_shards").get("failed").asInt(-1));
        JsonNode options = tree.path("suggest").path("number").path(0).path("options");
        List<String> suggestions = new ArrayList<>();
        for (JsonNode node : options) {
            String text = node.path("text").asText();
            suggestions.add(text);
        }
        Set<String> expected = new HashSet<>(asList("four", "fourteen"));
        assertEquals(expected, new HashSet<>(suggestions));
    }

    @Test
    public void healthcheckReturns200() throws IOException {
        Response r = client.target(application)
                .path("health")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, r.getStatus());
        r.close();
    }

    @Test
    public void healthcheckResponseContainsExpectedProperties() throws IOException {
        ObjectNode response = client.target(application)
                .path("health")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(ObjectNode.class);
        assertEquals(true, response.get("ok").asBoolean(false));
        assertEquals(true, response.get("elasticsearch").asBoolean(false));
        assertEquals(true, response.get("index").asBoolean(false));
        assertEquals(20, response.get("documents").asInt());
    }

    @AfterClass
    public static void tearDownClass() {
        logger.info("Stopping test server");
        server.stop();
    }

}

