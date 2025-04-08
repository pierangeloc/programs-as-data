package domainmodeling.healthcheck.http

import domainmodeling.healthcheck.HttpConnection.Url
import domainmodeling.healthcheck.{Message, Source, StatusError}
import sttp.client3.*
import sttp.client3.httpclient.zio.*
import zio.*
import neotype.*


object SttpHealthCheck {
  def check(url: Url): URIO[SttpClient, List[StatusError]] =
  for {
    be <- ZIO.service[SttpClient]
    res <-   basicRequest
      .response(asStringAlways)
      .get(uri"${url.unwrap}")
      .send(be)
      .timeout(3.seconds)
      .map {
        case Some(r) =>
          if (r.isSuccess) None else Some(StatusError(Source("Http"), Message("Http call returned ")))
        case None => Some(StatusError(Source("Http"), Message("Http call timed out")))
      }
      .catchAllCause(c =>
        ZIO.logErrorCause("Error calling Http dependency", c) *> ZIO.some(
          StatusError(Source("Http"), Message("Error performing the http call for healthcheck"))
        )
      )
    
  } yield res.toList

}
