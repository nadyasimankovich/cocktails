package core

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Service}
import com.twitter.util.Future

class HttpsClient(host: String) {
  val service: Service[Request, Response] = Http.client
    .withTls(host)
    .newClient(s"$host:443")
    .toService

  def sendGet(request: Request): Future[Response] = {
    service(request)
  }
}