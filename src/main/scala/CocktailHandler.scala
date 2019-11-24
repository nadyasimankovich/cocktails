import com.twitter.util.Future
import io.circe.generic.auto._
import io.circe.syntax._
import Models._

class CocktailHandler {
  def search(query: String): Future[String] = {
    for {
      result <- CocktailDbClient.search(query)
      images <- Future.traverseSequentially(result.drinks) { drink =>
        CocktailDbClient.getImage(drink.strDrinkThumb)
      }
    } yield {
      val response = MyResult(
        drinks = result.drinks.zip(images).map { case (drink, image) =>
          CassandraConnector.insert(CocktailImage(
            name = drink.strDrink.toLowerCase,
            recipe = drink.strInstructions,
            image = image
          ))

          CocktailInfo(
            name = drink.strDrink,
            recipe = drink.strInstructions,
            link = s"http://localhost:8080/${drink.strDrink.toLowerCase}"
          )
        }
      )

      response.asJsonObject.asJson.noSpaces
    }
  }

  def getImage(query: String): Future[Array[Byte]] = {
    Future.value {
      CassandraConnector.get(query) match {
        case Some(result) => result.image
        case None => Array.empty
      }
    }
  }
}