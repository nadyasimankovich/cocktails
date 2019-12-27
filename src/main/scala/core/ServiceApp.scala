package core

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import cocktail.{CocktailDbClient, CocktailsDataService, TokenReLoader, TokenState}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.request.ContentType
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.util.Future
import db.{CassandraConnector, CocktailImage}
import zio.clock.Clock
import zio.Runtime
import zio.internal.PlatformLive

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
    override def get(name: String): Future[Option[CocktailImage]] = {
      cache.get(name, name => super.get(name))
    }
  }
  private val cocktailDbClient = new CocktailDbClient(new AtomicReference[TokenState])
  private val cocktailsHandler = new CocktailHandler(cassandraConnector, cocktailDbClient)
  private val tokenReLoader = new TokenReLoader(cocktailDbClient)

  private val runtime = Runtime(
    new Clock {
      override val clock: Clock.Service[Any] = Clock.Live.clock
    },
    PlatformLive.Default
  )
  runtime.unsafeRunAsync_(tokenReLoader.reload())

  scheduledExecutor.schedule(new DataActivity(new CocktailsDataService(cassandraConnector, cocktailDbClient)).update, 1L, TimeUnit.MINUTES)

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
