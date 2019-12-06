import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.PopulationBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

class LoadTest extends Simulation {

  val httpConf: HttpProtocolBuilder = http.baseUrl("http:localhost:8080")
    .check(status is 200)

  val loadScenario: PopulationBuilder = scenario("cocktails")
    .feed(csv("./load/src/test/resources/data.csv"))
    .exec(http("search").get("search").queryParam("query", "${query}"))
    .exec(http("get").get("get").queryParam("name", "${name}"))
    .exec(http("images").get("/images/${name}"))
    .inject(
      rampUsersPerSec(1) to 100 during 3.minutes,
    )
    .protocols(httpConf)


}
