package service

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import cocktail.{CocktailDbClient, CocktailsDataService, TokenReLoader, TokenState}
import com.codahale.metrics.jmx.JmxReporter
import com.twitter.finagle.http.Request
import com.twitter.finagle.metrics.MetricsStatsReceiver
import com.twitter.finatra.http.request.{ContentType, RequestUtils}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.util.Future
import db.{CassandraConnector, CatalogRepository, CocktailImage, IngredientsRepository}
import service.Models.UserCocktailInfo
import zio.Runtime
import zio.clock.Clock
import zio.internal.PlatformLive

object ServiceApp extends HttpServer {
  override val defaultHttpPort: String = ":8080"

  private val serviceController = new ServiceController(new ScheduledThreadPoolExecutor(1))
  private val reporter: JmxReporter = JmxReporter.forRegistry(MetricsStatsReceiver.metrics).build()

  override def configureHttp(router: HttpRouter): Unit = {
    router.
      add(serviceController)
  }

  reporter.start()
}

class ServiceController(scheduledExecutor: ScheduledThreadPoolExecutor) extends Controller {
  private val cassandraConnector = new CassandraConnector
  private val ingredientsRepository = new IngredientsRepository(cassandraConnector)
  private val catalogRepository = new CatalogRepository(cassandraConnector) with ImageCache {
    override def get(name: String): Future[Option[CocktailImage]] = {
      cache.get(name, name => super.get(name))
    }
  }
  private val cocktailDbClient = new CocktailDbClient(new AtomicReference[TokenState])
  private val cocktailsHandler = new CocktailHandler(catalogRepository, ingredientsRepository, cocktailDbClient)
  private val tokenReLoader = new TokenReLoader(cocktailDbClient)

  private val runtime = Runtime(
    new Clock {
      override val clock: Clock.Service[Any] = Clock.Live.clock
    },
    PlatformLive.Default
  )
  runtime.unsafeRunAsync_(tokenReLoader.reload())

  scheduledExecutor.schedule(
    new DataActivity(new CocktailsDataService(catalogRepository, ingredientsRepository, cocktailDbClient)).update,
    2L, TimeUnit.MINUTES
  )

  get("/search") { request: Request =>
    (request.getParam("query"), request.getParam("ingredients")) match {
      case (q, null) =>
        cocktailsHandler.search(decode(q)).map { body =>
          response.ok(body.noSpaces).contentTypeJson()
        }

      case (null, ing) =>
        cocktailsHandler.searchByIngredients(decode(ing).split(",")).map { body =>
          response.ok(body.noSpaces).contentTypeJson()
        }

      case _ => response.badRequest("query or ingredients param not found")
    }
  }

  get("/get") { request: Request =>
    cocktailsHandler.get(decode(request.getParam("name"))).map { body =>
      if (body.isEmpty) response.notFound
      else response.ok(body.get.noSpaces).contentTypeJson()
    }
  }

  get("/images/:name") { request: Request =>
    cocktailsHandler.getImage(decode(request.getParam("name"))).map { body =>
      if (body.isEmpty) response.notFound
      else response.ok(body).contentType(ContentType.JPEG.contentTypeName)
    }
  }

  // curl -X PUT -F image=@photo_2019-12-21_18-30-47.jpg http://localhost:8080/images/test/add
  put("/images/:name/add") { request: Request =>
    val name = decode(request.getParam("name"))
    val file = RequestUtils.multiParams(request).get("image")

    cocktailsHandler.addImage(name, file.map(_.data).getOrElse(Array.empty))
      .onFailure { _ =>
        response.notFound(s"cocktail $name not found")
      }
      .onSuccess { _ =>
        response.ok(s"image for $name successfully updated")
      }
  }

  post("/add") { request: UserCocktailInfo =>
    cocktailsHandler.addCocktail(request.toDbCocktails).map { _ =>
      response.ok
    }
  }

  private def decode(str: String): String = {
    java.net.URLDecoder
      .decode(str.replace("/", ""), "UTF-8")
      .toLowerCase
  }
}
