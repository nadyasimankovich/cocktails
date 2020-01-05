package db

import java.util.concurrent.{Executor, Executors}

import com.codahale.metrics.jmx.JmxReporter
import com.datastax.driver.core.{Cluster, HostDistance, PoolingOptions, Session, SocketOptions}
import com.twitter.util.Future
import core.FutureUtils._

class CassandraConnector() {
  private val poolingOptions: PoolingOptions = new PoolingOptions()
    .setConnectionsPerHost(HostDistance.LOCAL, 1, 2)
    .setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
    .setCoreConnectionsPerHost(HostDistance.LOCAL, 2)

  private val socketOptions: SocketOptions = new SocketOptions()
    .setConnectTimeoutMillis(1000)
    .setReadTimeoutMillis(3000)

  private val cluster: Cluster = Cluster.builder
    .withClusterName("MyCassandra")
    .withoutJMXReporting()
    .addContactPoint("127.0.0.1")
    .withPoolingOptions(poolingOptions)
    .withSocketOptions(socketOptions)
    .build

  implicit val executor: Executor = Executors.newFixedThreadPool(10)

  private[db] val session: Future[Session] = cluster.connectAsync("cocktails").asScala

  private val reporter: JmxReporter = JmxReporter.forRegistry(cluster.getMetrics.getRegistry)
      .inDomain(cluster.getClusterName)
      .build()

  reporter.start()
}