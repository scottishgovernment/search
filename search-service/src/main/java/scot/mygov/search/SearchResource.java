package scot.mygov.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Path("/")
public class SearchResource {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private static final String ID = "id";

    private static final String PARAMS = "params";

    private static final String SEARCH_TEMPLATE = "_search/template";

    @Inject
    Provider<WebTarget> targetProvider;

    @GET
    @Path("{template}")
    public Response searchGet(
            @PathParam("template") String template,
            @Context UriInfo uriInfo) {

        ObjectNode request = queryObject(template, uriInfo.getQueryParameters());
        WebTarget target = target(SEARCH_TEMPLATE);
        return proxy(target, request);
    }

    /**
     * Converts parameters (from a query string) into an Elasticsearch template query object.
     * If a parameter is specified once, it
     */
    static ObjectNode queryObject(
            String template,
            MultivaluedMap<String, String> params) {

        ObjectNode paramsNode = JSON.objectNode();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            String param = entry.getKey();
            List<String> values = entry.getValue();
            if (values.size() == 1) {
                paramsNode.put(param, values.get(0));
            } else {
                paramsNode.set(param, JSON.arrayNode()
                        .addAll(values.stream()
                                .map(JSON::textNode)
                                .collect(toList())));
            }

        }
        ObjectNode request = JSON.objectNode();
        request.put(ID, template);
        request.set(PARAMS, paramsNode);
        return request;
    }

    @POST
    @Path("{template}")
    public Response searchAll(
            @PathParam("template") String template,
            JsonNode node)
            throws JsonProcessingException {

        ObjectNode request = JSON.objectNode();
        request.set(ID, JSON.textNode(template));
        request.set(PARAMS, node);

        WebTarget target = target(SEARCH_TEMPLATE);
        return proxy(target, request);
    }


    private WebTarget target(String path) {
        return targetProvider.get().path(path);
    }

    private Response proxy(WebTarget target, JsonNode request) {
        String response = target.request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(request), String.class);
        return Response.status(Status.OK)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(response)
                .build();
    }

}
