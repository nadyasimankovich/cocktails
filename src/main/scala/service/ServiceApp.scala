package service

import java.util.concurrent.ScheduledThreadPoolExecutor

import com.codahale.metrics.jmx.JmxReporter
import com.twitter.finagle.metrics.MetricsStatsReceiver
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.HttpServer

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