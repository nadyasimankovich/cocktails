package load

import io.gatling.core.Predef._
import io.gatling.core.feeder.FeederBuilder
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._
import scala.util.Random

class LoadTest extends Simulation {

  private val httpConf: HttpProtocolBuilder = http.baseUrl("http://localhost:8080/")
    .check(status is 200)

  private val cocktails: FeederBuilder = csv("./src/test/resources/cocktails.csv").circular
  private val ingredients: FeederBuilder = csv("./src/test/resources/ingredients.csv").circular

  private def randomString(size: Int): String = Random.alphanumeric.take(size).toSeq.mkString

  private val loadScenario: ScenarioBuilder = scenario("cocktails")
    .feed(cocktails)
    .feed(ingredients)
    .exec(http("search cocktails").get("search").queryParam("query", "${cocktailName}"))
    .exec(http("search ingredients").get("search").queryParam("ingredients", "${ingredient}"))
    .exec(http("images").get("images/${cocktailName}"))
    .exec(session => session.set("name", s"my_cocktail_${randomString(5)}"))
    .exec(
      http("add").post("add")
        .header("Content-Type", "application/json")
        .body(StringBody(
          "{\"name\":\"${name}\",".concat(
            s""""ingredients":"ingredient_${randomString(2)},ingredient_${randomString(2)},ingredient_${randomString(2)}","recipe":"shake it"}""".stripMargin
          )))
    )
    .exec(
      http("put image").put("images/${name}/add")
        .formUpload("image", "/Users/n.simankovich/IdeaProjects/cocktails/test_image.jpg")
    )
    .pause(1.seconds)
    .exec(http("check cocktail").get("get").queryParam("name", "${name}"))
    .exec(http("check image").get("images/${name}"))


  setUp(
    loadScenario
      .inject(
        rampUsersPerSec(1) to 10 during 1.minutes,
        constantUsersPerSec(10) during 5.minutes
      )
      .protocols(httpConf)
  )
    .assertions(global.failedRequests.count is 0L)
}