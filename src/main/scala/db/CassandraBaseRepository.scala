package db

import com.datastax.driver.core.PreparedStatement
import scala.concurrent.Future

abstract class CassandraBaseRepository[A, B](cassandraConnector: CassandraConnector) {
  protected val insertQuery: String
  protected val updateQuery: String
  protected val getQuery: String

  protected val insertStatement: Future[PreparedStatement]
  protected val updateStatement: Future[PreparedStatement]
  protected val getStatement: Future[PreparedStatement]

  def upsert(key: A): Future[Unit]
  def get(value: String): Future[Option[B]]
}
