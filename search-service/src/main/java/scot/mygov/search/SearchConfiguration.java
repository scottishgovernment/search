package scot.mygov.search;

import java.net.URI;

public class SearchConfiguration {

    private int port = 8082;

    private URI index = URI.create("http://localhost:9200/livecontent");

    public int getPort() {
        return port;
    }

    public URI getIndex() {
        return index;
    }

}
