package service

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import db.CocktailImage

import scala.concurrent.Future
import scala.concurrent.duration._

trait ImageCache {
  protected val cache: Cache[String, Future[Option[CocktailImage]]] = Scaffeine()
    .recordStats()
    .expireAfterWrite(1.hour)
    .maximumSize(10)
    .build[String, Future[Option[CocktailImage]]]()

  def invalidate(key: String): Unit = cache.invalidate(key)

  def invalidateAll(keys: Seq[String]): Unit = cache.invalidateAll(keys)
}
