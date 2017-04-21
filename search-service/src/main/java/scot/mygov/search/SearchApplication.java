package scot.mygov.search;

import javax.inject.Inject;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class SearchApplication extends Application {

    @Inject
    SearchResource search;

    @Inject
    ErrorHandler errorHandler;

    @Override
    public Set<Object> getSingletons() {
        return new HashSet<>(asList(
                search,
                errorHandler
        ));
    }

}
