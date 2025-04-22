package domainmodeling.healthcheck.http


import domainmodeling.healthcheck.HttpConnection.Url
import domainmodeling.healthcheck.{Message, Source, StatusError}
import sttp.client3._
import sttp.model.StatusCode
import neotype.*

import scala.concurrent.{ExecutionContext, Future}

object FutureHttpHealthCheck {
  def check[F[_], O](client: SttpBackend[Future, O], url: Url)(implicit ec: ExecutionContext): Future[List[StatusError]] = {
    basicRequest
      .get(uri"${url.unwrap}")
      .response(asString)
      .send(client)
      .map { response =>
        if (response.code.isSuccess) {
          List.empty
        } else {
          List(StatusError(
            Source("Http"),
            Message(s"HTTP request to ${url.unwrap} failed with status: ${response.code}")
          ))
        }
      }
      .recover {
        case ex: Exception =>
          List(StatusError(
            Source("Http"),
            Message(s"Error performing HTTP request: ${ex.getMessage}")
          ))
      }
  }
}

