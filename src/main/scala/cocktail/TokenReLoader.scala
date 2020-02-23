package cocktail

import zio.clock.Clock
import zio.duration._
import zio.{Schedule, ZIO}

import scala.concurrent.ExecutionContext

case class TokenState(token: String, ttl: Int)

class TokenReLoader(cocktailDbClient: CocktailDbClient) {
  private val policy = Schedule.fixed(60.seconds)

  def reload(): ZIO[Clock, Throwable, Option[Int]] = {
    ZIO.fromFuture { implicit ec: ExecutionContext =>
      import core.FutureUtils._
      cocktailDbClient.reloadToken.asScala
    }.repeat(policy)
      .timeout(50.millis)
      .mapError { _ =>
        new Throwable("Something bad happened when token has been updated")
      }
  }
}
