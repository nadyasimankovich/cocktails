package db

import com.datastax.driver.core.PreparedStatement
import com.twitter.util.Future

abstract class CassandraBaseRepository[A, B](cassandraConnector: CassandraConnector) {
  import cassandraConnector._
  import core.FutureUtils._

  protected val insertQuery: String
  protected val updateQuery: String
  protected val getQuery: String

  protected val insertStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(insertQuery).asScala)
  protected val updateStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(updateQuery).asScala)
  protected val getStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(getQuery).asScala)

  def upsert(key: A): Future[Unit]
  def get(value: String): Future[Option[B]]
}
