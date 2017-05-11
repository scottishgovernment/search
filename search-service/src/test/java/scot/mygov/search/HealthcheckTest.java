package scot.mygov.search;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dagger.Module;
import dagger.Provides;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;
import scot.mygov.search.Healthcheck.HealthcheckCallback;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HealthcheckTest {

    private static final JsonNodeFactory factory = JsonNodeFactory.instance;

    @Inject
    Healthcheck searchResource;

    @Captor
    ArgumentCaptor<Response> response;

    WebTarget target;

    AsyncResponse asyncResponse;

    @Module(injects = HealthcheckTest.class)
    class HealthcheckTestModule {
        @Provides
        WebTarget target() {
            return target;
        }
    }

    @Before
    public void setUp() {
        asyncResponse = mock(AsyncResponse.class);
    }

    @Test
    public void showsHealthyWhenIndexContainsDocuments() {
        when(asyncResponse.resume(response.capture())).thenReturn(true);
        HealthcheckCallback callback = new HealthcheckCallback(asyncResponse);
        ObjectNode node = factory.objectNode();
        node.set("hits", factory.objectNode()
                .put("total", 10));
        node.set("_shards", factory.objectNode()
                .put("total", 5)
                .put("failed", 0)
                .put("successful", 5));
        callback.completed(node);
        Response value = response.getValue();
        assertEquals(200, value.getStatus());
        ObjectNode health = (ObjectNode) value.getEntity();
        assertEquals(true, health.get("ok").asBoolean(false));
        assertEquals(true, health.get("elasticsearch").asBoolean(false));
        assertEquals(true, health.get("index").asBoolean(false));
        assertEquals(10, health.get("documents").asInt());
    }

    @Test
    public void showsUnhealthyWhenIndexIsEmpty() {
        when(asyncResponse.resume(response.capture())).thenReturn(true);
        HealthcheckCallback callback = new HealthcheckCallback(asyncResponse);
        ObjectNode node = factory.objectNode();
        node.set("hits", factory.objectNode()
                .put("total", 0));
        node.set("_shards", factory.objectNode()
                .put("total", 5)
                .put("failed", 0)
                .put("successful", 5));
        callback.completed(node);
        Response value = response.getValue();
        assertEquals(503, value.getStatus());
        ObjectNode health = (ObjectNode) value.getEntity();
        assertEquals(false, health.get("ok").asBoolean(true));
        assertEquals(true, health.get("elasticsearch").asBoolean(false));
        assertEquals(true, health.get("index").asBoolean(false));
        assertEquals(0, health.get("documents").asInt());
    }

    @Test
    public void showsUnhealthyWhenAShardFails() {
        when(asyncResponse.resume(response.capture())).thenReturn(true);
        HealthcheckCallback callback = new HealthcheckCallback(asyncResponse);
        ObjectNode node = factory.objectNode();
        node.set("hits", factory.objectNode()
                .put("total", 10));
        node.set("_shards", factory.objectNode()
                .put("total", 5)
                .put("failed", 1)
                .put("successful", 4));
        callback.completed(node);
        Response value = response.getValue();
        assertEquals(503, value.getStatus());
        ObjectNode health = (ObjectNode) value.getEntity();
        assertEquals(false, health.get("ok").asBoolean(true));
        assertEquals(true, health.get("elasticsearch").asBoolean(false));
        assertEquals(true, health.get("index").asBoolean(false));
        assertEquals(10, health.get("documents").asInt());
    }

    @Test
    public void showsUnhealthyWhenIndexDoesNotExist() {
        when(asyncResponse.resume(response.capture())).thenReturn(true);
        HealthcheckCallback callback = new HealthcheckCallback(asyncResponse);
        callback.failed(new NotFoundException("HTTP 404 Not Found"));
        Response value = response.getValue();
        assertEquals(503, value.getStatus());
        ObjectNode health = (ObjectNode) value.getEntity();
        assertEquals(false, health.get("ok").asBoolean(true));
        assertEquals(true, health.get("elasticsearch").asBoolean(false));
        assertEquals(false, health.get("index").asBoolean(true));
    }

    @Test
    public void showsUnhealthyWhenElasticsearchIsNotAvailable() {
        when(asyncResponse.resume(response.capture())).thenReturn(true);
        HealthcheckCallback callback = new HealthcheckCallback(asyncResponse);
        callback.failed(new ProcessingException(new IOException("Connection failed")));
        Response value = response.getValue();
        assertEquals(503, value.getStatus());
        ObjectNode health = (ObjectNode) value.getEntity();
        assertEquals(false, health.get("ok").asBoolean(true));
        assertEquals(false, health.get("elasticsearch").asBoolean(true));
        assertEquals(false, health.get("index").asBoolean(true));
        assertEquals("java.io.IOException: Connection failed", health.get("message").asText());
    }

}
