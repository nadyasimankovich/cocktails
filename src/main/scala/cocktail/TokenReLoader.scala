package cocktail

import zio.clock.Clock
import zio.duration._
import zio.{Schedule, ZIO}

case class TokenState(token: String, ttl: Int)

class TokenReLoader(cocktailDbClient: CocktailDbClient) {
  private val policy = Schedule.fixed(60.seconds)

  def reload(): ZIO[Clock, Throwable, Int] = {
    ZIO.effect(cocktailDbClient.reloadToken)
      .repeat(policy)
  }
}
