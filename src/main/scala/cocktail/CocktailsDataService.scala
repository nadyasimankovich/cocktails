package cocktail

import com.twitter.util.Future
import core.FutureHelper
import service.Models.Drink
import db._
import service.ImageCache

class CocktailsDataService(
  catalogRepository: CatalogRepository with ImageCache,
  ingredientsRepository: IngredientsRepository,
  cocktailDbClient: CocktailDbClient
) extends FutureHelper {
  private val alphabet: Seq[Char] = 'a' to 'y'

  def reload(): Future[Unit] = {
    for {
      images <- getAllImages()
      _ <- batchTraverse(images, catalogRepository.upsert)
      _ <- batchTraverse(images.flatMap(_.toIngredientLink), ingredientsRepository.upsert)
    } yield {
      catalogRepository.invalidateAll(images.map(_.name))
    }
  }

  private def getAllImages(): Future[Seq[CocktailImage]] = {
    for {
      drinks <- getAllCocktails()
      images <- batchTraverse(drinks, drinks.map(_.strDrinkThumb), cocktailDbClient.getImage)
    } yield {
      images.map { case (drink, image) =>
        CocktailImage(
          name = drink.strDrink.toLowerCase,
          ingredients = drink.ingredients.getOrElse(Set.empty).map(_.toLowerCase),
          recipe = drink.strInstructions,
          image = image
        )
      }.toSeq
    }
  }

  private def getAllCocktails(): Future[Seq[Drink]] = {
    batchTraverse(alphabet, cocktailDbClient.searchByFirstLetter).map(_.values.toSeq.flatten.distinct)
  }
}