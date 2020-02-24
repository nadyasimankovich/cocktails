package db

import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import core._

import scala.concurrent.Future

class IngredientsRepository(cassandraConnector: CassandraConnector)
  extends CassandraBaseRepository[IngredientLink, Ingredient](cassandraConnector) {
  import cassandraConnector._
  import core.FutureUtils._

  protected val insertQuery: String =
    """
      |insert into cocktails.ingredients (name, cocktails)
      |values (?, ?)
      |""".stripMargin

  protected val updateQuery: String =
    """
      |update cocktails.ingredients
      |set cocktails = ?
      |where name = ?
      |""".stripMargin

  protected val getQuery: String =
    """
      |select name, cocktails
      |from cocktails.ingredients
      |where name = ?
      |""".stripMargin

  protected val insertStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(insertQuery).asScala)
  protected val updateStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(updateQuery).asScala)
  protected val getStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(getQuery).asScala)

  override def upsert(value: IngredientLink): Future[Unit] = {
    def insert: Future[BoundStatement] = {
      insertStatement.map {
        _.bind()
          .setString("name", value.name)
          .setString("cocktails", value.cocktail)
      }
    }

    def update(result: Ingredient): Future[Option[BoundStatement]] = {
      if (!result.cocktails.contains(value.cocktail)) {
          updateStatement.map {
            _.bind()
              .setString("name", value.name)
              .setString("cocktails", (result.cocktails + value.cocktail).mkString(","))
          }.map(i => Some(i))
      } else Future.successful(None)
    }

    for {
      result <- get(value.name)
      bounded <- if (result.isDefined) update(result.get) else insert.map(Some(_))
      _ <- if (bounded.isDefined) session.flatMap(_.executeAsync(bounded.get).asScala) else Future.successful()
    } yield ()
  }

  override def get(key: String): Future[Option[Ingredient]] = {
    println(s"CassandraConnector: $key")

    for {
      statement <- getStatement
      row <- session.flatMap(_.executeAsync(statement.bind(key)).asScala).map(_.one())
    } yield {
      if (row == null) None
      else Some(Ingredient(
        name = row.getString("name"),
        cocktails = row.getString("cocktails").split(",").toSet
      ))
    }
  }
}
