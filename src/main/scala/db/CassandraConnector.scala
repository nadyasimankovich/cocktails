package db

import java.nio.ByteBuffer
import java.util.concurrent.{Executor, Executors}

import com.datastax.driver.core.{BoundStatement, Cluster, PreparedStatement, Session}
import com.twitter.util.Future
import core.FutureUtils._

class CassandraConnector() {
  private val cluster: Cluster = Cluster.builder
    .addContactPoint("127.0.0.1")
    .build

  private implicit val executor: Executor = Executors.newFixedThreadPool(10)

  private[db] val session: Future[Session] = cluster.connectAsync("cocktails").asScala

  private val insertQuery =
    """
      |insert into cocktails.catalog (name, ingredients, recipe, image, ts)
      |values (?, ?, ? , ?, ?)
      |""".stripMargin

  private val updateQuery =
    """
      |update cocktails.catalog
      |set ingredients = ?, recipe = ?, image = ?, ts = ?
      |where name = ?
      |""".stripMargin

  private val getQuery =
    """
      |select name, ingredients, recipe, image, ts
      |from cocktails.catalog
      |where name = ?
      |""".stripMargin

  private val insertStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(insertQuery).asScala)
  private val updateStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(updateQuery).asScala)
  private val getStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(getQuery).asScala)

  def upsert(cocktail: CocktailImage): Future[Unit] = {
    def insert: Future[BoundStatement] = {
      for {
        statement <- insertStatement
      } yield {
        statement.bind()
          .setString("name", cocktail.name)
          .setString("ingredients", cocktail.ingredients)
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
          .setString("ingredients", cocktail.ingredients)
          .setString("recipe", cocktail.recipe)
          .setBytes("image", ByteBuffer.wrap(cocktail.image))
          .setLong("ts", cocktail.ts)
          .setString("name", cocktail.name)
      }
    }

    for {
      result <- get(cocktail.name)
      bounded <- if (result.isDefined) update else insert
      _ <- session.flatMap(_.executeAsync(bounded).asScala)
    } yield ()
  }

  def get(name: String): Future[Option[CocktailImage]] = {
    println(s"CassandraConnector: $name")

    for {
      statement <- getStatement
      row <- session.flatMap(_.executeAsync(statement.bind(name)).asScala).map(_.one())
    } yield {
      if (row == null) None
      else Some(CocktailImage(
        name = row.getString("name"),
        ingredients = row.getString("ingredients"),
        recipe = row.getString("recipe"),
        image = row.getBytes("image").array(),
        ts = row.getLong("ts")
      ))
    }
  }
}