package core

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.request.ContentType
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}
import db.{CassandraConnector, CocktailImage}

object ServiceApp extends HttpServer {
  override val defaultHttpPort: String = ":8080"

  override def configureHttp(router: HttpRouter): Unit = {
    router.
      add[ServiceController]
  }
}

class ServiceController extends Controller {
  private val cassandraConnector = new CassandraConnector with ImageCache {
    override def get(name: String): Option[CocktailImage] = {
      cache.get(name, name => super.get(name))
    }
  }
  private val cocktailsHandler = new CocktailHandler(cassandraConnector)

  get("/search") { request: Request =>
    cocktailsHandler.search(request.getParam("query")).map { body =>
      response.ok(body).contentTypeJson()
    }
  }

  get("/:name") { request: Request =>
    val query = java.net.URLDecoder
      .decode(request.path.replace("/", ""), "UTF-8")
        .toLowerCase

    cocktailsHandler.getImage(query).map { body =>
      if (body.isEmpty) response.notFound
      else response.ok(body).contentType(ContentType.JPEG.contentTypeName)
    }
  }
}
