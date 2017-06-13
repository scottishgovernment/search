package scot.mygov.search;

import dagger.Module;
import dagger.Provides;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.mygov.config.Configuration;

import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

@Module(injects = Search.class)
public class SearchModule {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SearchConfiguration.class);

    private static final String APP_NAME = "search";

    @Provides
    @Singleton
    SearchConfiguration configuration() {
        Configuration<SearchConfiguration> configuration = Configuration
                .load(new SearchConfiguration(), APP_NAME)
                .validate();
        LOGGER.info("{}", configuration);
        return configuration.getConfiguration();
    }

    @Provides
    @Singleton
    Client client() {
        return new ResteasyClientBuilder()
                .connectionPoolSize(10)
                .build();
    }

    @Provides
    WebTarget target(Client client, SearchConfiguration configuration) {
        return client.target(configuration.getIndex());
    }

}
