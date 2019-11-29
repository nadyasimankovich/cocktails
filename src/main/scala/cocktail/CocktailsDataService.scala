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
      drinks <- getAllCocktails()
      images <- batchTraverse(drinks.map(_.strDrinkThumb), CocktailDbClient.getImage)
    } yield {
      images.map { case (name, image) =>
        val drink = drinks.find(_.strDrinkThumb.contains(name)).get
        CocktailImage(
          name = drink.strDrink.toLowerCase,
          recipe = drink.strInstructions,
          image = image
        )
      }.toSeq
    }
  }

  private def getAllCocktails(): Future[Seq[Drink]] = {
    batchTraverse(alphabet, CocktailDbClient.searchByFirstLetter).map(_.values.flatten.toSeq.distinct)
  }
}
