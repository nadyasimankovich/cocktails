package core

import cocktail.CocktailDbClient
import com.twitter.util.Future
import core.Models.Result
import db.CocktailImage

class CocktailsDataService {
  private val alphabet: Seq[Char] = 'a' to 'y'

  def reload = {

  }

  private def getAllImages: Future[Seq[CocktailImage]] = {
    for {
      result <- getAllCocktails
      images <- Future.traverseSequentially(result.flatMap(_.drinks)) { drink =>
        CocktailDbClient.getImage(drink.strDrinkThumb).map { image =>
          drink -> image
        }
      }
    } yield {
      images.map { case (drink, image) =>
        CocktailImage(
          name = drink.strDrink.toLowerCase,
          recipe = drink.strInstructions,
          image = image
        )
      }
    }
  }

  private def getAllCocktails: Future[Seq[Result]] = {
    alphabet.grouped(5).foldLeft[Future[Seq[Result]]](Future.value(Seq.empty)) { case (f, group) =>
      f.flatMap { _ =>
        println(s"#$group started")
        Future.traverseSequentially(group) { letter =>
          CocktailDbClient.search(letter)
        }
      }
    }
  }
}
