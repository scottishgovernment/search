package scot.mygov.search.it;

import dagger.Module;
import dagger.Provides;
import scot.mygov.search.SearchApplication;
import scot.mygov.search.SearchConfiguration;
import scot.mygov.search.SearchModule;
import scot.mygov.config.Configuration;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;

@Module(
        injects = SearchApplication.class,
        includes = SearchModule.class,
        overrides = true
)
public class TestSearchModule {

    private static final String APP_NAME = "search";

    private final Map<String, String> config;

    public TestSearchModule(Map<String, String> config) {
        this.config = config;
    }

    @Provides
    @Singleton
    SearchConfiguration configuration()  {
        try {
            return Configuration
                    .create(new SearchConfiguration(), APP_NAME, config)
                    .getConfiguration();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
