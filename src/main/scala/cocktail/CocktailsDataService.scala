package cocktail

import com.twitter.util.Future
import core.Models.Result
import db.{CassandraConnector, CocktailImage}

class CocktailsDataService(cassandraConnector: CassandraConnector) {
  private val alphabet: Seq[Char] = 'a' to 'y'

  def reload(): Future[Unit] = {
    for {
      images <- getAllImages()
      _ <- Future.traverseSequentially(images) { i =>
        Future.value(cassandraConnector.upsert(i))
      }
    } yield ()
  }

  private def getAllImages(): Future[Seq[CocktailImage]] = {
    for {
      result <- getAllCocktails()
      images <- Future.traverseSequentially(result.flatMap(_.drinks).distinct) { drink =>
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

  private def getAllCocktails(): Future[Seq[Result]] = {
    alphabet.grouped(5).foldLeft(Future.value(Seq.empty[Result])) { case (f, group) =>
      f.flatMap { res =>
        Future.traverseSequentially(group) { letter =>
          CocktailDbClient.search(letter)
        }.map {_.flatten ++ res}
      }
    }
  }
}
