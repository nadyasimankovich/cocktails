package cocktail

import com.twitter.util.Future
import core.FutureHelper
import core.Models.Drink
import db.{CassandraConnector, CocktailImage}

class CocktailsDataService(cassandraConnector: CassandraConnector) extends FutureHelper {
  private val alphabet: Seq[Char] = 'a' to 'y'

  def reload(): Future[Unit] = {
    for {
      images <- getAllImages()
      _ <- batchTraverse(images, cassandraConnector.upsert)
    } yield ()
  }

  private def getAllImages(): Future[Seq[CocktailImage]] = {
    for {
      drinks <- getAllCocktails().map(_.distinct.toSeq)
      images <- batchTraverse(drinks.map(_.strDrinkThumb), CocktailDbClient.getImage)
    } yield {
      images.flatMap { case (name, image) =>
        drinks.find(_.strDrinkThumb.contains(name)).map { drink =>
          CocktailImage(
            name = drink.strDrink.toLowerCase,
            recipe = drink.strInstructions,
            image = image
          )
        }
      }
    }
  }

  private def getAllCocktails(): Future[Seq[Drink]] = {
    batchTraverse(alphabet, CocktailDbClient.searchByFirstLetter).map(_.map(_._2)).map(_.flatten)
  }
}
