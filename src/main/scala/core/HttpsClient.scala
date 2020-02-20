package core

import com.twitter.finagle.http.{Request, RequestBuilder, Response}
import com.twitter.finagle.{Http, Service}
import com.twitter.util.Future
import com.twitter.finagle.http.Method

class HttpsClient(host: String) {
  private val service: Service[Request, Response] = Http
    .client
    .withTls(host)
    .newService(s"$host:443")

  def sendGet(
    path: String,
    params: Map[String, String] = Map.empty,
    headers: Map[String, String] = Map.empty,
    method: Method = Method.Get
  ): Future[Response] = {
    val request = RequestBuilder()
      .url(Request.queryString("https://" + host + path, params))
      .addHeaders(headers)
      .build(method, None)

    service(request)
  }
}