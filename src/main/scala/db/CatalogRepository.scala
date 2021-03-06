package db

import java.nio.ByteBuffer

import com.datastax.driver.core.{BoundStatement, PreparedStatement}

import scala.concurrent.Future
import core._

class CatalogRepository(cassandraConnector: CassandraConnector)
  extends CassandraBaseRepository[CocktailImage, CocktailImage](cassandraConnector) {

  import cassandraConnector._
  import core.FutureUtils._

  protected val insertQuery: String =
    """
      |insert into cocktails.catalog (name, ingredients, recipe, image, ts)
      |values (?, ?, ? , ?, ?)
      |""".stripMargin

  protected val updateQuery: String =
    """
      |update cocktails.catalog
      |set ingredients = ?, recipe = ?, image = ?, ts = ?
      |where name = ?
      |""".stripMargin

  protected val addImageQuery: String =
    """
      |update cocktails.catalog
      |set image = ?, ts = ?
      |where name = ?
      |""".stripMargin

  protected val getQuery: String =
    """
      |select name, ingredients, recipe, image, ts
      |from cocktails.catalog
      |where name = ?
      |""".stripMargin

  protected val insertStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(insertQuery).asScala)
  protected val updateStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(updateQuery).asScala)
  protected val addImageStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(addImageQuery).asScala)
  protected val getStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(getQuery).asScala)

  override def upsert(cocktail: CocktailImage): Future[Unit] = {
    def insert: Future[BoundStatement] = {
        insertStatement.map {
          _.bind()
            .setString("name", cocktail.name)
            .setString("ingredients", cocktail.ingredients.mkString(","))
            .setString("recipe", cocktail.recipe)
            .setBytes("image", ByteBuffer.wrap(cocktail.image))
            .setLong("ts", cocktail.ts)
        }
      }

    def update: Future[BoundStatement] = {
      updateStatement.map {
        _.bind()
          .setString("ingredients", cocktail.ingredients.mkString(","))
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

  override def get(name: String): Future[Option[CocktailImage]] = {
    println(s"CassandraConnector: $name")

    for {
      statement <- getStatement
      row <- session.flatMap(_.executeAsync(statement.bind(name)).asScala).map(_.one())
    } yield {
      if (row == null) None
      else Some(CocktailImage(
        name = row.getString("name"),
        ingredients = row.getString("ingredients").split(",").toSet,
        recipe = row.getString("recipe"),
        image = row.getBytes("image").array(),
        ts = row.getLong("ts")
      ))
    }
  }

  def addImage(name: String, image: Array[Byte]): Future[Unit] = {
    get(name).flatMap {
      case None => Future.failed(new Throwable(s"cocktail with name = $name not found"))
      case Some(_) =>
        for {
          bounded <- addImageStatement.map {
            _.bind()
              .setBytes("image", ByteBuffer.wrap(image))
              .setLong("ts", currentTs)
              .setString("name", name)
          }
          _ <- session.flatMap(_.executeAsync(bounded).asScala)
        } yield ()
    }
  }
}
