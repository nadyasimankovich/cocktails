package core

import cocktail.CocktailDbClient
import com.twitter.util.Future
import core.Models._
import db.{CassandraConnector, CocktailImage}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._

class CocktailHandler(cassandraConnector: CassandraConnector, cocktailDbClient: CocktailDbClient) extends FutureHelper {

  private def imageLink(name: String) = s"http://localhost:8080/images/${name.toLowerCase}"

  def search(query: String): Future[Json] = {
    for {
      drinks <- cocktailDbClient.search(query).map(_.distinct.toSeq)
      images <- batchTraverse(drinks.map(_.strDrinkThumb), cocktailDbClient.getImage).map { result =>
        result.map { case (name, res) =>
          drinks.find(_.strDrinkThumb.contains(name)).get -> res
        }
      }
      _ <- batchTraverse(images.map { case (drink, image) => CocktailImage(
        name = drink.strDrink.toLowerCase,
        recipe = drink.strInstructions,
        image = image
      )
      }.toSeq, cassandraConnector.upsert)
    } yield {
      val response = MyResult(
        drinks = images.map { case (drink, _) =>
          CocktailInfo(
            name = drink.strDrink,
            recipe = drink.strInstructions,
            link = imageLink(drink.strDrink)
          )
        }.toSeq
      )

      response.asJsonObject.asJson
    }
  }

  def get(name: String): Future[Option[Json]] = {
    cassandraConnector.get(name).map { res =>
      res.map { i =>
        CocktailInfo(
          name = i.name,
          recipe = i.recipe,
          link = imageLink(i.name)
        ).asJsonObject.asJson
      }
    }
  }

  def getImage(name: String): Future[Array[Byte]] = {
    cassandraConnector.get(name).map {
      case Some(result) => result.image
      case None => Array.empty
    }
  }
}