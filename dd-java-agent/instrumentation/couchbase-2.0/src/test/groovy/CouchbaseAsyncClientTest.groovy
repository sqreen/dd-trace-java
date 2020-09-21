import com.couchbase.client.java.AsyncCluster
import com.couchbase.client.java.CouchbaseAsyncCluster
import com.couchbase.client.java.document.JsonDocument
import com.couchbase.client.java.document.json.JsonObject
import com.couchbase.client.java.env.CouchbaseEnvironment
import com.couchbase.client.java.query.N1qlQuery
import spock.lang.Retry
import spock.lang.Unroll
import spock.util.concurrent.BlockingVariable
import util.AbstractCouchbaseTest

import java.util.concurrent.TimeUnit

import static datadog.trace.agent.test.utils.TraceUtils.basicSpan
import static datadog.trace.agent.test.utils.TraceUtils.runUnderTrace

@Retry
@Unroll
class CouchbaseAsyncClientTest extends AbstractCouchbaseTest {
  static final int TIMEOUT = 10

  def "test hasBucket #type"() {
    setup:
    def hasBucket = new BlockingVariable<Boolean>(TIMEOUT)

    when:
    cluster.openBucket(bucketSettings.name(), bucketSettings.password()).subscribe({ bkt ->
      manager.hasBucket(bucketSettings.name()).subscribe({ result -> hasBucket.set(result) })
    })

    then:
    assert hasBucket.get()
    assertTraces(1) {
      trace(2) {
        assertCouchbaseCall(it, "ClusterManager.hasBucket", null, span(1))
        assertCouchbaseCall(it, "Cluster.openBucket", null)
      }
    }

    cleanup:
    cluster?.disconnect()?.timeout(TIMEOUT, TimeUnit.SECONDS)?.toBlocking()?.single()
    environment.shutdown()

    where:
    bucketSettings << [bucketCouchbase, bucketMemcache]

    environment = envBuilder(bucketSettings).build()
    cluster = CouchbaseAsyncCluster.create(environment, Arrays.asList("127.0.0.1"))
    manager = cluster.clusterManager(USERNAME, PASSWORD).toBlocking().single()
    type = bucketSettings.type().name()
  }

  def "test upsert #type"() {
    setup:
    JsonObject content = JsonObject.create().put("hello", "world")
    def inserted = new BlockingVariable<JsonDocument>(TIMEOUT)

    when:
    runUnderTrace("someTrace") {
      // Connect to the bucket and open it
      cluster.openBucket(bucketSettings.name(), bucketSettings.password()).subscribe({ bkt ->
        bkt.upsert(JsonDocument.create("helloworld", content)).subscribe({ result -> inserted.set(result) })
      })

      blockUntilChildSpansFinished(2) // Improve span ordering consistency
    }

    then:
    inserted.get().content().getString("hello") == "world"

    assertTraces(1) {
      trace(3) {
        basicSpan(it, "someTrace")
        assertCouchbaseCall(it, "Bucket.upsert", bucketSettings.name(), span(2))
        assertCouchbaseCall(it, "Cluster.openBucket", null, span(0))
      }
    }

    cleanup:
    cluster?.disconnect()?.timeout(TIMEOUT, TimeUnit.SECONDS)?.toBlocking()?.single()
    environment.shutdown()

    where:
    bucketSettings << [bucketCouchbase, bucketMemcache]

    environment = envBuilder(bucketSettings).build()
    cluster = CouchbaseAsyncCluster.create(environment, Arrays.asList("127.0.0.1"))
    type = bucketSettings.type().name()
  }

  def "test upsert and get #type"() {
    setup:
    JsonObject content = JsonObject.create().put("hello", "world")
    def inserted = new BlockingVariable<JsonDocument>(TIMEOUT)
    def found = new BlockingVariable<JsonDocument>(TIMEOUT)

    when:
    runUnderTrace("someTrace") {
      cluster.openBucket(bucketSettings.name(), bucketSettings.password()).subscribe({ bkt ->
        bkt.upsert(JsonDocument.create("helloworld", content))
          .subscribe({ result ->
            inserted.set(result)
            bkt.get("helloworld")
              .subscribe({ searchResult -> found.set(searchResult)
              })
          })
      })

      blockUntilChildSpansFinished(3) // Improve span ordering consistency
    }

    // Create a JSON document and store it with the ID "helloworld"
    then:
    found.get() == inserted.get()
    found.get().content().getString("hello") == "world"

    assertTraces(1) {
      trace(4) {
        basicSpan(it, "someTrace")

        assertCouchbaseCall(it, "Bucket.get", bucketSettings.name(), span(2))
        assertCouchbaseCall(it, "Bucket.upsert", bucketSettings.name(), span(3))
        assertCouchbaseCall(it, "Cluster.openBucket", null, span(0))
      }
    }

    cleanup:
    cluster?.disconnect()?.timeout(TIMEOUT, TimeUnit.SECONDS)?.toBlocking()?.single()
    environment.shutdown()

    where:
    bucketSettings << [bucketCouchbase, bucketMemcache]

    environment = envBuilder(bucketSettings).build()
    cluster = CouchbaseAsyncCluster.create(environment, Arrays.asList("127.0.0.1"))
    type = bucketSettings.type().name()
  }

  def "test query"() {
    setup:
    // Only couchbase buckets support queries.
    CouchbaseEnvironment environment = envBuilder(bucketCouchbase).build()
    AsyncCluster cluster = CouchbaseAsyncCluster.create(environment, Arrays.asList("127.0.0.1"))
    def queryResult = new BlockingVariable<JsonObject>(TIMEOUT)

    when:
    // Mock expects this specific query.
    // See com.couchbase.mock.http.query.QueryServer.handleString.
    runUnderTrace("someTrace") {
      cluster.openBucket(bucketCouchbase.name(), bucketCouchbase.password()).subscribe({
        bkt ->
          bkt.query(N1qlQuery.simple("SELECT mockrow"))
            .flatMap({ query -> query.rows() })
            .single()
            .subscribe({ row -> queryResult.set(row.value()) })
      })

      blockUntilChildSpansFinished(2) // Improve span ordering consistency
    }

    then:
    queryResult.get().get("row") == "value"

    assertTraces(1) {
      trace(3) {
        basicSpan(it, "someTrace")

        assertCouchbaseCall(it, "Bucket.query", bucketCouchbase.name(), span(2))
        assertCouchbaseCall(it, "Cluster.openBucket", null, span(0))
      }
    }

    cleanup:
    cluster?.disconnect()?.timeout(TIMEOUT, TimeUnit.SECONDS)?.toBlocking()?.single()
    environment.shutdown()
  }
}
