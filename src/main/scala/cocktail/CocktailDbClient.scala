package cocktail

import com.twitter.finagle.http.Request
import com.twitter.io.Buf
import com.twitter.util.Future
import core.HttpsClient
import core.Models.Result
import io.circe.parser.decode

// https://www.thecocktaildb.com/api/json/v1/1/search.php?s=margarita
object CocktailDbClient {
  val serviceSearch = new HttpsClient("www.thecocktaildb.com")

  def search(query: String): Future[Result] = {
    serviceSearch
      .sendGet(Request("/api/json/v1/1/search.php", ("s", query)))
      .map { result =>
        println(result)
        decode[Result](result.contentString) match {
          case Right(value) => value
          case Left(ex) => throw ex
        }
      }
  }

  def getImage(name: String): Future[Array[Byte]] = {
    println(name)
    serviceSearch
      .sendGet(Request(name))
      .map { result =>
        println(result)
        Buf.ByteArray.Owned.extract(result.content)
      }
  }
}
