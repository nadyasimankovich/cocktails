package cocktail

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

import com.twitter.io.Buf
import com.twitter.util.Future
import core.HttpsClient
import service.Models.{Drink, Result}
import io.circe.parser.decode

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
      decode[Result](result.contentString) match {
        case Right(value) => value.drinks
        case Left(ex) => throw ex
      }
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
      decode[Result](result.contentString) match {
        case Right(value) => value.drinks
        case Left(_) => Seq.empty
      }
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
      newToken <- Future.value(TokenState(UUID.randomUUID().toString, 600))
    } yield {
      println(s"reloadToken: $newToken")
      token.set(newToken)
    }
  }
}