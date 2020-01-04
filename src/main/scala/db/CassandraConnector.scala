package db

import java.util.concurrent.{Executor, Executors}

import com.datastax.driver.core.{Cluster, HostDistance, PoolingOptions, Session}
import com.twitter.util.Future
import core.FutureUtils._

class CassandraConnector() {
  private val poolingOptions: PoolingOptions = new PoolingOptions()
    .setConnectionsPerHost(HostDistance.LOCAL, 1, 2)
    .setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
    .setCoreConnectionsPerHost(HostDistance.LOCAL, 2)

  private val cluster: Cluster = Cluster.builder
    .addContactPoint("127.0.0.1")
    .withPoolingOptions(poolingOptions)
    .build

  implicit val executor: Executor = Executors.newFixedThreadPool(1)

  private[db] val session: Future[Session] = cluster.connectAsync("cocktails").asScala
}