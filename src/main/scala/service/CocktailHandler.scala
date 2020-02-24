package service

import cocktail.CocktailDbClient
import core.FutureHelper
import Models._
import db._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import core._

import scala.concurrent.Future

class CocktailHandler(
  catalogRepository: CatalogRepository with ImageCache,
  ingredientsRepository: IngredientsRepository,
  cocktailDbClient: CocktailDbClient
) extends FutureHelper {

  private def imageLink(name: String) = s"http://localhost:8080/images/${name.toLowerCase}"

  def search(query: String): Future[Json] = {
    for {
      drinks <- cocktailDbClient.search(query).map(_.distinct.toSeq)
      images <- batchTraverse(drinks, drinks.map(_.strDrinkThumb), cocktailDbClient.getImage)
      imagesDb = images.map { case (drink, image) => CocktailImage(
        name = drink.strDrink.toLowerCase,
        ingredients = drink.ingredients.getOrElse(Set.empty).map(_.toLowerCase),
        recipe = drink.strInstructions,
        image = image
      )
      }.toSeq
      _ <- batchTraverse(imagesDb, catalogRepository.upsert)
      _ <- batchTraverse(imagesDb.flatMap(_.toIngredientLink), ingredientsRepository.upsert)
    } yield {
      catalogRepository.invalidateAll(imagesDb.map(_.name))

      val response = MyResult(
        drinks = images.map { case (drink, _) =>
          CocktailInfo(
            name = drink.strDrink,
            ingredients = drink.ingredients.getOrElse(Set.empty).mkString(","),
            recipe = drink.strInstructions,
            link = imageLink(drink.strDrinkThumb)
          )
        }.toSeq
      )

      response.asJsonObject.asJson
    }
  }

  def searchByIngredients(query: Seq[String]): Future[Json] = {
    for {
      cocktailsName <- batchTraverse(query, ingredientsRepository.get)
        .map(_.values.toSeq.flatten.flatMap(_.cocktails).toSet)
      cocktails <- batchTraverse(cocktailsName.toSeq, catalogRepository.get)
        .map(_.values.flatten)
    } yield {
      val response = MyResult(
        drinks = cocktails.map { i =>
          CocktailInfo(
            name = i.name,
            ingredients = i.ingredients.mkString(","),
            recipe = i.recipe,
            link = imageLink(i.name)
          )
        }.toSeq
      )

      response.asJsonObject.asJson
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

  def addCocktail(cocktail: CocktailImage): Future[Unit] = {
    for {
      _ <- catalogRepository.upsert(cocktail)
      _ <- batchTraverse(cocktail.toIngredientLink.toSeq, ingredientsRepository.upsert)
    } yield {
      catalogRepository.invalidate(cocktail.name)
    }
  }

  def addImage(name: String, image: Array[Byte]): Future[Unit] = {
    if (image.isEmpty) Future.failed(new Throwable(s"image is empty"))
    else {
      for {
        _ <- catalogRepository.addImage(name, image)
      } yield catalogRepository.invalidate(name)
    }
  }
}