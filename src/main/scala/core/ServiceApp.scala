package core

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.request.ContentType
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}

object ServiceApp extends HttpServer {
  override val defaultHttpPort: String = ":8080"

  override def configureHttp(router: HttpRouter): Unit = {
    router.
      add[ServiceController]
  }
}

class ServiceController extends Controller {
  val cocktailsHandler = new CocktailHandler

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
