package core

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import db.CocktailImage

import scala.concurrent.duration._

trait ImageCache {
  val cache: Cache[String, Option[CocktailImage]] = Scaffeine()
      .recordStats()
      .expireAfterWrite(10.minutes)
      .maximumSize(10)
      .build[String, Option[CocktailImage]]()
}
