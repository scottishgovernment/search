package scot.mygov.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SearchResourceTest {

    private static final JsonNodeFactory factory = JsonNodeFactory.instance;

    @Inject
    SearchResource searchResource;

    private Dispatcher dispatcher;

    WebTarget target;

    @Module(injects = SearchResourceTest.class)
    class SearchResourceTestModule {
        @Provides
        WebTarget target() {
            return target;
        }
    }

    @Before
    public void setUp() {
        this.target = mock(WebTarget.class);

        ObjectGraph graph = ObjectGraph.create(new SearchResourceTestModule());
        graph.inject(this);
        dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addSingletonResource(searchResource);
    }

    @Test
    public void queryByTemplatReturns200() throws IOException, URISyntaxException {
        ObjectNode params = factory.objectNode();
        ObjectMapper mapper = new ObjectMapper();

        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(target.path(any())).thenReturn(target);
        when(target.request()).thenReturn(builder);
        when(builder.accept((MediaType) any())).thenReturn(builder);
        when(builder.post(any(), eq(String.class))).thenReturn("{}");

        MockHttpRequest request = MockHttpRequest.post("template")
                .contentType(MediaType.APPLICATION_JSON_TYPE)
                .content(mapper.writeValueAsBytes(params));
        request.accept(MediaType.APPLICATION_JSON_TYPE);
        request.contentType(MediaType.APPLICATION_JSON_TYPE);
        MockHttpResponse response = new MockHttpResponse();

        dispatcher.invoke(request, response);
        assertEquals(200, response.getStatus());
        verify(target).request();
    }

    @Test
    public void parametersToRequestObject() {
         MultivaluedMap<String, String> params = new MultivaluedHashMap<>();
         params.add("p", "a");
         params.add("q", "1");
         params.add("q", "2");

         ObjectNode request = SearchResource.queryObject("template", params);

         assertEquals("template", request.get("id").textValue());
         ObjectNode paramsNode = (ObjectNode) request.get("params");
         assertEquals("a", paramsNode.get("p").textValue());
         assertEquals(2, paramsNode.get("q").size());
         assertEquals("1", paramsNode.get("q").get(0).textValue());
         assertEquals("2", paramsNode.get("q").get(1).textValue());
     }

}

