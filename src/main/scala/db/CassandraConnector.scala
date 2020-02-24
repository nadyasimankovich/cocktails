package db

import java.util.concurrent.{Executor, Executors}

import com.codahale.metrics.jmx.JmxReporter
import com.datastax.driver.core.{Cluster, HostDistance, PoolingOptions, Session, SocketOptions}
import core.FutureUtils._

import scala.concurrent.Future

class CassandraConnector() {
  private val poolingOptions: PoolingOptions = new PoolingOptions()
    .setMaxRequestsPerConnection(HostDistance.LOCAL, 1000)
    .setMaxConnectionsPerHost(HostDistance.LOCAL, 10)
    .setMaxQueueSize(100)

  private val cluster: Cluster = Cluster.builder
    .withoutJMXReporting()
    .addContactPoint("127.0.0.1")
    .withPoolingOptions(poolingOptions)
    .build

  implicit val executor: Executor = Executors.newFixedThreadPool(10)

  private[db] val session: Future[Session] = cluster.connectAsync("cocktails").asScala

  private val reporter: JmxReporter = JmxReporter.forRegistry(cluster.getMetrics.getRegistry)
      .inDomain("cassandra.connector")
      .build()

  reporter.start()
}