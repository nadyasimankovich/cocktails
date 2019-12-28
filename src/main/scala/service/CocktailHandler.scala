package service

import cocktail.CocktailDbClient
import com.twitter.util.Future
import core.FutureHelper
import Models._
import db._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._

class CocktailHandler(
  catalogRepository: CatalogRepository,
  ingredientsRepository: IngredientsRepository,
  cocktailDbClient: CocktailDbClient
) extends FutureHelper {

  private def imageLink(name: String) = s"http://localhost:8080/images/${name.toLowerCase}"

  def search(query: String): Future[Json] = {
    for {
      drinks <- cocktailDbClient.search(query).map(_.distinct.toSeq)
      images <- batchTraverse(drinks.map(_.strdrinkthumb), cocktailDbClient.getImage).map { result =>
        result.map { case (name, res) =>
          drinks.find(_.strdrinkthumb.contains(name)).get -> res
        }
      }
      imagesDb = images.map { case (drink, image) => CocktailImage(
        name = drink.strdrink,
        ingredients = drink.ingredients.getOrElse(Set.empty),
        recipe = drink.strinstructions,
        image = image
      )
      }.toSeq
      _ <- batchTraverse(imagesDb, catalogRepository.upsert)
      _ <- batchTraverse(imagesDb.flatMap(cocktailToIngredientLink), ingredientsRepository.upsert)
    } yield {
      val response = MyResult(
        drinks = images.map { case (drink, _) =>
          CocktailInfo(
            name = drink.strdrink,
            ingredients = drink.ingredients.getOrElse(Set.empty).mkString(","),
            recipe = drink.strinstructions,
            link = imageLink(drink.strdrink)
          )
        }.toSeq
      )

      response.asJsonObject.asJson
    }
  }

  def searchByIngredients(query: String): Future[Option[Json]] = {
    ingredientsRepository.get(query).map { res =>
      res.map { i =>
        i.cocktails.mkString(",").asJson
      }
    }
  }

  def get(name: String): Future[Option[Json]] = {
    catalogRepository.get(name).map { res =>
      res.map { i =>
        CocktailInfo(
          name = i.name,
          ingredients = i.ingredients.mkString(","),
          recipe = i.recipe,
          link = imageLink(i.name)
        ).asJsonObject.asJson
      }
    }
  }

  def getImage(name: String): Future[Array[Byte]] = {
    catalogRepository.get(name).map {
      case Some(result) => result.image
      case None => Array.empty
    }
  }
}