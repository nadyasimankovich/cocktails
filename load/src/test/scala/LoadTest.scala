import io.gatling.core.Predef._
import io.gatling.core.feeder.{ FeederBuilder}
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

class LoadTest extends Simulation {

  private val httpConf: HttpProtocolBuilder = http.baseUrl("http://localhost:8080/")
    .check(status is 200)

  private val feeder: FeederBuilder = csv("./src/test/resources/data.csv").circular

  private val loadScenario: ScenarioBuilder = scenario("cocktails")
    .feed(feeder)
    .exec(http("search").get("search").queryParam("query", "${name}"))
    .exec(http("get").get("get").queryParam("name", "${name}"))
    .exec(http("images").get("/images/${name}"))

  setUp(
    loadScenario
      .inject(
        rampUsersPerSec(1) to 100 during 5.minutes,
        constantUsersPerSec(100) during 5.minutes
      )
      .protocols(httpConf)
  ).assertions(
    global.failedRequests.count is 0L
  )
}
