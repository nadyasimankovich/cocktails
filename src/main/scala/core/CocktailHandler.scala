package core

import cocktail.CocktailDbClient
import com.twitter.util.Future
import core.Models._
import db.{CassandraConnector, CocktailImage}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._

class CocktailHandler(cassandraConnector: CassandraConnector) {

  private def imageLink(name: String) = s"http://localhost:8080/images/${name.toLowerCase}"

  def search(query: String): Future[Json] = {
    for {
      result <- CocktailDbClient.search(query)
      images <- Future.traverseSequentially(result.drinks) { drink =>
        CocktailDbClient.getImage(drink.strDrinkThumb).map { drink -> _}
      }
    } yield {
      val response = MyResult(
        drinks = images.map { case (drink, image) =>
          cassandraConnector.upsert(CocktailImage(
            name = drink.strDrink.toLowerCase,
            recipe = drink.strInstructions,
            image = image
          ))

          CocktailInfo(
            name = drink.strDrink,
            recipe = drink.strInstructions,
            link = imageLink(drink.strDrink)
          )
        }
      )

      response.asJsonObject.asJson
    }
  }

  def get(name: String): Future[Option[Json]] = {
    Future.value {
      cassandraConnector.get(name) match {
        case Some(result) => Some(CocktailInfo(
          name = result.name,
          recipe = result.recipe,
          link = imageLink(result.name)
        ).asJsonObject.asJson)
        case _ => None
      }
    }
  }

  def getImage(query: String): Future[Array[Byte]] = {
    Future.value {
      cassandraConnector.get(query) match {
        case Some(result) => result.image
        case None => Array.empty
      }
    }
  }
}