package scot.mygov.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/health")
public class Healthcheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(Healthcheck.class);

    @Inject
    Provider<WebTarget> targetProvider;

    @Inject
    Healthcheck() {
        // Default constructor
    }

    @GET
    public void health(@Suspended final AsyncResponse response) {
        targetProvider.get()
                .path("_search")
                .queryParam("size", 0)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .buildGet()
                .submit(new HealthcheckCallback(response));
    }


    static class HealthcheckCallback implements InvocationCallback<JsonNode> {

        private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;

        /** Name of property that summarises the state of this service. */
        private static final String OK = "ok";

        /** Name of property that indicates the availability of Elasticsearch. */
        private static final String ELASTICSEARCH = "elasticsearch";

        /** Name of property that indicates the availability of the index. */
        private static final String INDEX = "index";

        /** Name of property that indicates the number of documents in the index. */
        private static final String DOCUMENTS = "documents";

        /** Name of property that describes the status of the Elasticsearch index shards. */
        private static final String SHARDS = "shards";

        /** Name of property that describes the status message. */
        private static final String MESSAGE = "message";

        private final AsyncResponse asyncResponse;

        public HealthcheckCallback(AsyncResponse response) {
            this.asyncResponse = response;
        }

        @Override
        public void completed(JsonNode search) {
            int documents = search.path("hits").path("total").asInt(0);
            JsonNode shards = search.path("_shards");
            int totalShards = shards.path("total").asInt(-1);
            int successful = shards.path("successful").asInt(-1);
            int failed = shards.path("failed").asInt(-1);
            boolean shardsOk = failed == 0;
            boolean ok = documents > 0 && shardsOk;
            String message = String.format("%d documents (%d/%d shards ok)",
                    documents,
                    successful,
                    totalShards);
            ObjectNode result = FACTORY.objectNode()
                    .put(ELASTICSEARCH, true)
                    .put(INDEX, true)
                    .put(SHARDS, shardsOk)
                    .put(MESSAGE, message)
                    .put(DOCUMENTS, documents);
            Status status = ok ? Status.OK : Status.SERVICE_UNAVAILABLE;
            resume(status, result);
        }

        @Override
        public void failed(Throwable throwable) {
            Throwable t = throwable.getCause() != null ? throwable.getCause() : throwable;
            String message = formatMessage(t);
            LOGGER.error("{} {}", t.getClass().getName(), t.getMessage());
            boolean elasticsearch = t instanceof WebApplicationException;
            ObjectNode result = FACTORY.objectNode()
                    .put(ELASTICSEARCH, elasticsearch)
                    .put(INDEX, false)
                    .put(MESSAGE, message);
            resume(Status.SERVICE_UNAVAILABLE, result);
        }

        void resume(Status status, ObjectNode result) {
            JsonNode entity = FACTORY.objectNode()
                    .put(OK, status.equals(Status.OK))
                    .setAll(result);
            Response response = Response.status(status)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(entity).build();
            asyncResponse.resume(response);
        }

        /**
         * If the Elasticsearch healthcheck fails, the reason should returned in the response.
         * This can be presented on a monitoring dashboard.
         * However, if Elasticsearch returns a 40x error, then it should be obvious that it
         * was in response to the Elasticsearch request, and not the response from this healthcheck.
         * Therefore, if Elasticsearch returns a 40x error, augment the message to clarify that it
         * is from Elasticsearch.
         */
        private String formatMessage(Throwable throwable) {
            String message;
            if (throwable instanceof ClientErrorException) {
                message = "Elasticsearch returned: " + throwable.getMessage();
            } else {
                message = String.format("%s: %s",
                        throwable.getClass().getName(),
                        throwable.getMessage());
            }
            return message;
        }

    }

}
