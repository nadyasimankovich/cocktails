package cocktail

import com.twitter.finagle.http.Request
import com.twitter.io.Buf
import com.twitter.util.Future
import core.HttpsClient
import core.Models.Result
import io.circe.parser.decode

// https://www.thecocktaildb.com/api/json/v1/1/search.php?s=margarita
object CocktailDbClient {
  private val serviceSearch = new HttpsClient("www.thecocktaildb.com")

  def search(query: String): Future[Result] = {
    serviceSearch
      .sendGet(Request("/api/json/v1/1/search.php", ("s", query)))
      .map { result =>
        decode[Result](result.contentString) match {
          case Right(value) => value
          case Left(ex) => throw ex
        }
      }
  }

  def search(firstLetter: Char): Future[Option[Result]] = {
    serviceSearch
      .sendGet(Request("/api/json/v1/1/search.php", ("f", firstLetter.toString)))
      .map { result =>
        decode[Result](result.contentString) match {
          case Right(value) => Some(value)
          case Left(_) => None
        }
      }
  }

  def getImage(name: String): Future[Array[Byte]] = {
    serviceSearch
      .sendGet(Request(name))
      .map { result =>
        Buf.ByteArray.Owned.extract(result.content)
      }
  }
}
