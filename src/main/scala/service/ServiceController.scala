package service

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}
import java.util.concurrent.atomic.AtomicReference

import cocktail.{CocktailDbClient, CocktailsDataService, TokenReLoader, TokenState}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.request.{ContentType, RequestUtils}
import db.{CassandraConnector, CatalogRepository, CocktailImage, IngredientsRepository}
import service.Models.UserCocktailInfo
import zio.Runtime
import zio.clock.Clock
import zio.internal.PlatformLive
import core._

import scala.concurrent.Future
import scala.util.{Failure, Success}

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
    10L, TimeUnit.MINUTES
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

  // curl -X PUT -F image=@test_image.jpg http://localhost:8080/images/test/add
  put("/images/:name/add") { request: Request =>
    val name = decode(request.getParam("name"))
    val file = RequestUtils.multiParams(request).get("image")

    cocktailsHandler.addImage(name.toLowerCase, file.map(_.data).getOrElse(Array.empty)).onComplete {
      case Success(_) => response.ok(s"image for $name successfully updated")
      case Failure(_) => response.notFound(s"cocktail $name not found")
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

