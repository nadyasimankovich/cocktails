package cocktail

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

import com.twitter.io.Buf
import core.HttpsClient
import io.circe.Json
import service.Models.Drink
import io.circe.parser.decode
import scala.concurrent.Future
import core._
// https://www.thecocktaildb.com/api/json/v1/1/search.php?s=margarita
class CocktailDbClient(token: AtomicReference[TokenState]) {

  private val serviceSearch = new HttpsClient("www.thecocktaildb.com")

  def search(query: String): Future[Seq[Drink]] = {
    for {
      result <- serviceSearch
        .sendGet(
          path = "/api/json/v1/1/search.php",
          Map("s" -> query),
          headers = Map("Authorization" -> s"Bearer ${token.get().token}")
        )
    } yield {
      parseDrinks(result.contentString)
    }
  }

  def searchByFirstLetter(firstLetter: Char): Future[Seq[Drink]] = {
    for {
      result <- serviceSearch
        .sendGet(
          path = "/api/json/v1/1/search.php",
          Map("f" -> firstLetter.toString),
          headers = Map("Authorization" -> s"Bearer ${token.get().token}")
        )
    } yield {
      parseDrinks(result.contentString)
    }
  }

  def getImage(name: String): Future[Array[Byte]] = {
    for {
      result <- serviceSearch
        .sendGet(
          path = name,
          headers = Map("Authorization" -> s"Bearer ${token.get().token}")
        )
    } yield {
      Buf.ByteArray.Owned.extract(result.content)
    }
  }

  def reloadToken: Future[Unit] = {
    for {
      newToken <- Future.successful(TokenState(UUID.randomUUID().toString, 600))
    } yield {
      println(s"reloadToken: $newToken")
      token.set(newToken)
    }
  }

  private def parseDrinks(result: String): List[Drink] = {
    def parseIngredients(json: Json): Set[String] = {
      (1 to 15).flatMap { i =>
        json.\\(s"strIngredient$i").flatMap(_.asString)
      }.toSet
    }

    def contentToJson(response: String): List[Json]  = {
      decode[Json](response) match {
        case Right(value) =>
          value.\\("drinks").head.asArray.getOrElse(List.empty).toList
        case Left(ex) => throw ex
      }
    }

    contentToJson(result).map { json =>
      json.as[Drink] match {
        case Right(drink) =>
          drink.copy(ingredients = Some(parseIngredients(json)))
        case Left(ex) => throw ex
      }
    }
  }
}