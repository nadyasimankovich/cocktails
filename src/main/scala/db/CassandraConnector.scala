package db

import java.util.concurrent.{Executor, Executors}

import com.datastax.driver.core.{Cluster, Session}
import com.twitter.util.Future
import core.FutureUtils._

class CassandraConnector() {
  private val cluster: Cluster = Cluster.builder
    .addContactPoint("127.0.0.1")
    .build

  implicit val executor: Executor = Executors.newFixedThreadPool(10)

  private[db] val session: Future[Session] = cluster.connectAsync("cocktails").asScala
}