package cocktail

import com.twitter.util.Future
import core.FutureHelper
import service.Models.Drink
import db._

class CocktailsDataService(
  catalogRepository: CatalogRepository,
  ingredientsRepository: IngredientsRepository,
  cocktailDbClient: CocktailDbClient
) extends FutureHelper {
  private val alphabet: Seq[Char] = 'a' to 'y'

  def reload(): Future[Unit] = {
    for {
      images <- getAllImages()
      _ <- batchTraverse(images, catalogRepository.upsert)
      _ <- batchTraverse(images.flatMap(cocktailToIngredientLink), ingredientsRepository.upsert)
    } yield ()
  }

  private def getAllImages(): Future[Seq[CocktailImage]] = {
    for {
      drinks <- getAllCocktails()
      images <- batchTraverse(drinks, drinks.map(_.strdrinkthumb), cocktailDbClient.getImage)
    } yield {
      images.map { case (drink, image) =>
        CocktailImage(
          name = drink.strdrink,
          ingredients = drink.ingredients.getOrElse(Set.empty),
          recipe = drink.strinstructions,
          image = image
        )
      }.toSeq
    }
  }

  private def getAllCocktails(): Future[Seq[Drink]] = {
    batchTraverse(alphabet, cocktailDbClient.searchByFirstLetter).map(_.values.toSeq.flatten.distinct)
  }
}