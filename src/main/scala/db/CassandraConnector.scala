package db

import java.nio.ByteBuffer
import java.util.concurrent.{Executor, Executors}

import com.datastax.driver.core.{BoundStatement, Cluster, PreparedStatement, Session}
import com.twitter.util.Future
import core.FutureUtils._

import scala.concurrent.ExecutionContext

class CassandraConnector() {
  private val cluster: Cluster = Cluster.builder
    .addContactPoint("127.0.0.1")
    .build

  private implicit val executor: Executor = Executors.newFixedThreadPool(1)
  private implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  private[db] val session: Future[Session] = cluster.connectAsync("cocktails").asScala.asTwitter

  private lazy val insertQuery =
    """
      |insert into cocktails.catalog (name, recipe, image, ts)
      |values (?, ? , ?, ?)
      |""".stripMargin

  private lazy val updateQuery =
    """
      |update cocktails.catalog
      |set recipe = ?, image = ?, ts = ?
      |where name = ?
      |""".stripMargin

  private lazy val getQuery =
    """
      |select name, recipe, image, ts
      |from cocktails.catalog
      |where name = ?
      |""".stripMargin

  private lazy val insertStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(insertQuery).asScala.asTwitter)
  private lazy val updateStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(updateQuery).asScala.asTwitter)
  private lazy val getStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(getQuery).asScala.asTwitter)

  def upsert(cocktail: CocktailImage): Future[Unit] = {
    def insert: Future[BoundStatement] = {
      for {
        statement <- insertStatement
      } yield {
        statement.bind()
          .setString("name", cocktail.name)
          .setString("recipe", cocktail.recipe)
          .setBytes("image", ByteBuffer.wrap(cocktail.image))
          .setLong("ts", cocktail.ts)
      }
    }

    def update: Future[BoundStatement] = {
      for {
        statement <- updateStatement
      } yield {
        statement.bind()
          .setString("recipe", cocktail.recipe)
          .setBytes("image", ByteBuffer.wrap(cocktail.image))
          .setLong("ts", cocktail.ts)
          .setString("name", cocktail.name)
      }
    }

    for {
      result <- get(cocktail.name)
      bounded <- if (result.isDefined) update else insert
      _ <- session.flatMap(_.executeAsync(bounded).asScala.asTwitter)
    } yield ()
  }

  def get(name: String): Future[Option[CocktailImage]] = {
    println(s"CassandraConnector: $name")

    for {
      statement <- getStatement
      row <- session.flatMap(_.executeAsync(statement.bind(name)).asScala.asTwitter).map(_.one())
    } yield {
      if (row == null) None
      else Some(CocktailImage(
        name = row.getString("name"),
        recipe = row.getString("recipe"),
        image = row.getBytes("image").array(),
        ts = row.getLong("ts")
      ))
    }
  }
}