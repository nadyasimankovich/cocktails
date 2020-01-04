package db

import com.datastax.driver.core.BoundStatement
import com.twitter.util.Future

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

  override def upsert(value: IngredientLink): Future[Unit] = {
    def insert: Future[BoundStatement] = {
      for {
        statement <- insertStatement
      } yield {
        statement.bind()
          .setString("name", value.name)
          .setString("cocktails", value.cocktail)
      }
    }

    def update(result: Ingredient): Future[Option[BoundStatement]] = {
      for {
        statement <- updateStatement
      } yield {
        if (!result.cocktails.contains(value.cocktail)) {
          Some(
            statement.bind()
              .setString("name", value.name)
              .setString("cocktails", (result.cocktails + value.cocktail).mkString(","))
          )
        } else None
      }
    }

    for {
      result <- get(value.name)
      bounded <- if (result.isDefined) update(result.get) else insert.map(Some(_))
      _ <- if (bounded.isDefined) session.flatMap(_.executeAsync(bounded.get).asScala) else Future.value()
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
