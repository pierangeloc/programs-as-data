package domainmodeling.healthcheck

import domainmodeling.healthcheck.Infra.Db.DbType
import domainmodeling.healthcheck.db.DoobieZIOdBHealthcheck
import domainmodeling.healthcheck.kafka.KafkaHealthCheck
import doobie.*
import neotype.*
import sttp.client3.*
import sttp.client3.httpclient.zio.*
import zio.*
import zio.kafka.admin.AdminClient

object ZIOInterpreter {
  def interpret(
    errorCondition: ErrorCondition
  ): URIO[Transactor[Task] & zio.kafka.admin.AdminClient & HttpClientZioBackend, List[StatusError]] =
    errorCondition match
      case ErrorCondition.Or(left, right) =>
        for {
          leftErrors  <- interpret(left)
          rightErrors <- interpret(right)
        } yield leftErrors ++ rightErrors
      case ErrorCondition.DBErrorCondition(DbType.Postgres, checkTables) =>
        for {
          tx  <- ZIO.service[Transactor[Task]]
          res <- DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsPostgres, tx, checkTables)
        } yield res

      case ErrorCondition.DBErrorCondition(DbType.MySql, checkTables) =>
        for {
          tx  <- ZIO.service[Transactor[Task]]
          res <- DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsMySql, tx, checkTables)
        } yield res
      case ErrorCondition.KafkaErrorCondition(topics) =>
        for {
          ac <- ZIO.service[AdminClient]
          res <- KafkaHealthCheck
                   .describeTopics(topics)
                   .catchAllCause(c =>
                     ZIO.logErrorCause("Error calling Kafka", c) *> ZIO.some(
                       StatusError(Source("Kafka"), Message("Error performing the topic existence check"))
                     )
                   )
                   .timeout(3.seconds)
                   .map {
                     case Some(r) => r
                     case None    => Some(StatusError(Source("Kafka"), Message("Kafka Operation timed out")))
                   }
        } yield res.toList
      case ErrorCondition.ExternalHttpErrorCondition(url) =>
        for {
          be <- ZIO.service[HttpClientZioBackend]
          res <- basicRequest
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
