# Search

This service is used on the informational sites to provide an API for search
queries. It receives search requests from the browser, formats them into
Elasticsearch queries which are then executed, and the results returned to the
browser.

This application provides endpoints for:

* querying an index using a search template;
* querying a type of document in an index using a search template;
* autocompletion of search results.

This service proxies search requests so that Elasticsearch doesn't have to be
exposed directly to the internet. It limits the queries that are forwarded to Elasticsearch, and is intended to limit the attack surface area.

# Configuration

* `port`
 * The port number to listen on for HTTP requests.
 * Type: integer (0-65535)
 * Default: `8082`
* `index`
 * The URL for the Elasticsearch index to be queried.
 * Type: URL
 * Default: `http://localhost:9200/livecontent`

# Integration tests

The tests in the `scot.mygov.search.it` package are integration tests that
depend on a running instance of Elasticsearch. These tests can be run using
`mvn verify`. This starts up an Elasticsearch instance as part of the maven
build, and stops it after running the integration tests.

Using Maven to run tests can be slow, because of the time it takes to start
Elasticsearch. Instead, when developing tests, run the `./run-es` script in the top-level directory to run an Elasticsearch instance. This allows integration
tests to be run from the IDE. Press `Ctrl+C` to stop the instance, before
running Maven again.
