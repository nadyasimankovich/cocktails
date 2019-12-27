package cocktail

import zio.duration._
import zio.{Schedule, ZIO}

case class TokenState(token: String, ttl: Int)

class TokenReLoader(cocktailDbClient: CocktailDbClient) extends {
  private val policy = Schedule.fixed(60.seconds)

  def reload() = {
    ZIO.effect(cocktailDbClient.reloadToken)
      .repeat(policy)
  }
}
