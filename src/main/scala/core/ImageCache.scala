package core

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import com.twitter.util.Future
import db.CocktailImage

import scala.concurrent.duration._

trait ImageCache {
  val cache: Cache[String, Future[Option[CocktailImage]]] = Scaffeine()
      .recordStats()
      .expireAfterWrite(10.minutes)
      .maximumSize(10)
      .build[String, Future[Option[CocktailImage]]]()
}
