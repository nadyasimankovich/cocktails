package db

import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.twitter.util.Future

class IngredientsRepository(cassandraConnector: CassandraConnector) {
  import cassandraConnector._
  import core.FutureUtils._

  private val insertQuery =
    """
      |insert into cocktails.ingredients (name, cocktails)
      |values (?, ?)
      |""".stripMargin

  private val updateQuery =
    """
      |update cocktails.ingredients
      |set cocktails = ?
      |where name = ?
      |""".stripMargin

  private val getQuery =
    """
      |select name, cocktails
      |from cocktails.ingredients
      |where name = ?
      |""".stripMargin

  private val insertStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(insertQuery).asScala)
  private val updateStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(updateQuery).asScala)
  private val getStatement: Future[PreparedStatement] = session.flatMap(_.prepareAsync(getQuery).asScala)

  def upsert(ingredient: IngredientLink): Future[Unit] = {
    def insert: Future[BoundStatement] = {
      for {
        statement <- insertStatement
      } yield {
        statement.bind()
          .setString("name", ingredient.name)
          .setString("cocktails", ingredient.cocktail)
      }
    }

    def update(result: Ingredient): Future[Option[BoundStatement]] = {
      for {
        statement <- updateStatement
      } yield {
        if (!result.cocktails.contains(ingredient.cocktail)) {
          Some(
            statement.bind()
              .setString("name", ingredient.name)
              .setString("cocktails", (result.cocktails + ingredient.cocktail).mkString(","))
          )
        } else None
      }
    }

    for {
      result <- get(ingredient.name)
      bounded <- if (result.isDefined) update(result.get) else insert.map(Some(_))
      _ <- if (bounded.isDefined) session.flatMap(_.executeAsync(bounded.get).asScala) else Future.value()
    } yield ()
  }

  def get(name: String): Future[Option[Ingredient]] = {
    println(s"CassandraConnector: $name")

    for {
      statement <- getStatement
      row <- session.flatMap(_.executeAsync(statement.bind(name)).asScala).map(_.one())
    } yield {
      if (row == null) None
      else Some(Ingredient(
        name = row.getString("name"),
        cocktails = row.getString("cocktails").split(",").toSet
      ))
    }
  }
}
