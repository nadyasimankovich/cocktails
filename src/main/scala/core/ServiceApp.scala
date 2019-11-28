package core

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import cocktail.CocktailsDataService
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.request.ContentType
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}
import db.{CassandraConnector, CocktailImage}

object ServiceApp extends HttpServer {
  override val defaultHttpPort: String = ":8080"

  private val serviceController = new ServiceController(new ScheduledThreadPoolExecutor(1))
  override def configureHttp(router: HttpRouter): Unit = {
    router.
      add(serviceController)
  }
}

class ServiceController(scheduledExecutor: ScheduledThreadPoolExecutor) extends Controller {
  private val cassandraConnector = new CassandraConnector with ImageCache {
    override def get(name: String): Option[CocktailImage] = {
      cache.get(name, name => super.get(name))
    }
  }
  private val cocktailsHandler = new CocktailHandler(cassandraConnector)
  scheduledExecutor.schedule(new DataActivity(new CocktailsDataService(cassandraConnector)).update, 1L, TimeUnit.MINUTES)

  get("/search") { request: Request =>
    cocktailsHandler.search(request.getParam("query")).map { body =>
      response.ok(body.noSpaces).contentTypeJson()
    }
  }

  get("/get") { request: Request =>
    cocktailsHandler.get(decode(request.getParam("name"))).map { body =>
      if (body.isEmpty) response.notFound
      else response.ok(body.get.noSpaces).contentTypeJson()
    }
  }

  get("/images/:name") { request: Request =>
    cocktailsHandler.getImage(decode(request.path)).map { body =>
      if (body.isEmpty) response.notFound
      else response.ok(body).contentType(ContentType.JPEG.contentTypeName)
    }
  }

  private def decode(str: String): String = {
    java.net.URLDecoder
      .decode(str.replace("/", ""), "UTF-8")
      .toLowerCase
  }
}
