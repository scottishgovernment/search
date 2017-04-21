package scot.mygov.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/")
public class SearchResource {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private static final String ID = "id";

    private static final String PARAMS = "params";

    @Inject
    Provider<WebTarget> targetProvider;

    @GET
    @Path("direct/")
    public Response searchByQueryParam(
            @QueryParam("q") String query,
            @QueryParam("from") @DefaultValue("0") int from,
            @QueryParam("size") @DefaultValue("10") int size)
            throws JsonProcessingException {

        ObjectNode params = JSON.objectNode();
        params.put("term", query);
        params.put("from", from);
        params.put("size", size);

        ObjectNode request = JSON.objectNode();
        request.set(ID, JSON.textNode("site-search"));
        request.set(PARAMS, params);

        WebTarget target = target("_search/template");
        return proxy(target, request);
    }

    @POST
    @Path("{prefix : (template-search/)?}{template}")
    public Response searchAll(
            @PathParam("template") String template,
            JsonNode node)
            throws JsonProcessingException {

        ObjectNode request = JSON.objectNode();
        request.set(ID, JSON.textNode(template));
        request.set(PARAMS, node);

        WebTarget target = target("_search/template");
        return proxy(target, request);
    }

    @POST
    @Path("{prefix : (template-search/)?}{type}/{template}")
    public Response searchByType(
            @PathParam("type") String type,
            @PathParam("template") String template,
            JsonNode node)
            throws JsonProcessingException {

        ObjectNode request = JSON.objectNode();
        request.set(ID, JSON.textNode(template));
        request.set(PARAMS, node);

        WebTarget target = target("{type}/_search/template")
                .resolveTemplate("type", type);
        return proxy(target, request);
    }

    @GET
    @Path("ac")
    public Response autocomplete(@QueryParam("q") String query) {
        ObjectNode request = (ObjectNode) JSON.objectNode()
                .set("autocomplete", JSON.objectNode()
                        .put("text", query.toLowerCase())
                        .set("completion", JSON.objectNode()
                                .put("field", "autocomplete")));

        WebTarget target = target("_suggest");
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
