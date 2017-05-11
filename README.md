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

# Monitoring

The healthcheck endpoint is `GET /health`. The endpoint returns a JSON response
with the properties listed below. The status code is `200` if the service is
healthy, and `503` otherwise.

* `ok`
  * Indicates whether the search index is available and contains content.
  * Type: boolean
* `elasticsearch`
  * Indicates whether Elasticsearch appears to be up. It is considered up if an
    HTTP response is received, even if the status code indicates an error. This
    is used to indicate connectivity to Elasticsearch only. See also: `index`.
  * Type: boolean
* `index`
  * Indicates whether the index is available to the extent that a simple search query returns a 200 response code.
  * Type: boolean
* `shards`
  * Indicates whether all the shards in the index are OK.  
    This property is not present if `index` is false.
  * Type: boolean
* `documents`
  * The number of documents in the index.  
    This property is not present if `index` is false.
  * Type: boolean
* `message`
  * The URL for the Elasticsearch index to be queried.
  * Type: boolean

The check-search script in this repository is a Nagios plugin that queries the
healthcheck endpoint, returning the appropriate status code for Nagios. It also
outputs a summary of the current status. The output information that shows up as
performance data in Nagios, which includes the status of Elasticsearch; the
availability of the index; document count and shard health.

# Integration tests

The tests in the `scot.mygov.search.it` package are integration tests that
depend on a running instance of Elasticsearch. These tests can be run using
`mvn verify`. This starts up an Elasticsearch instance as part of the maven
build, and stops it after running the integration tests.

Using Maven to run tests can be slow, because of the time it takes to start
Elasticsearch. Instead, when developing tests, run the `./run-es` script in the top-level directory to run an Elasticsearch instance. This allows integration
tests to be run from the IDE. Press `Ctrl+C` to stop the instance, before
running Maven again.
