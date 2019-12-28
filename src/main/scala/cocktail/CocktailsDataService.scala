package cocktail

import com.twitter.util.Future
import core.FutureHelper
import service.Models.Drink
import db.{CassandraConnector, CocktailImage}

class CocktailsDataService(cassandraConnector: CassandraConnector, cocktailDbClient: CocktailDbClient) extends FutureHelper {
  private val alphabet: Seq[Char] = 'a' to 'y'

  def reload(): Future[Unit] = {
    for {
      images <- getAllImages()
      _ <- batchTraverse(images, cassandraConnector.upsert)
    } yield ()
  }

  private def getAllImages(): Future[Seq[CocktailImage]] = {
    for {
      drinks <- getAllCocktails()
      images <- batchTraverse(drinks.map(_.strDrinkThumb), cocktailDbClient.getImage)
    } yield {
      images.toSeq.map { case (name, image) =>
        val drink = drinks.find(_.strDrinkThumb.contains(name)).get
        CocktailImage(
          name = drink.strDrink.toLowerCase,
          ingredients = drink.ingredients.getOrElse(Set.empty).mkString(", "),
          recipe = drink.strInstructions,
          image = image
        )
      }
    }
  }

  private def getAllCocktails(): Future[Seq[Drink]] = {
    batchTraverse(alphabet, cocktailDbClient.searchByFirstLetter).map(_.values.toSeq.flatten.distinct)
  }
}