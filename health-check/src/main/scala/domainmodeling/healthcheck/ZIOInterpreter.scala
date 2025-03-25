package domainmodeling.healthcheck

import domainmodeling.healthcheck.InfraModel.Db.DbType
import domainmodeling.healthcheck.db.DoobieZIOdBHealthcheck
import domainmodeling.healthcheck.http.SttpHealthCheck
import domainmodeling.healthcheck.kafka.KafkaHealthCheck
import doobie.*
import neotype.*
import sttp.client3.httpclient.zio.{HttpClientZioBackend, SttpClient}
import zio.*
import zio.kafka.admin.AdminClient

object ZIOInterpreter {
  def interpret(
    errorCondition: ErrorCondition
  ): URIO[Transactor[Task] & zio.kafka.admin.AdminClient & SttpClient, List[StatusError]] =
    errorCondition match
      case ErrorCondition.Or(left, right) =>
        interpret(left).zipPar(interpret(right)).map { case (leftErrors, rightErrors) => leftErrors ++ rightErrors }
      case ErrorCondition.DBErrorCondition(DbType.Postgres, checkTables) =>
        DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsPostgres, checkTables)

      case ErrorCondition.DBErrorCondition(DbType.MySql, checkTables) =>
        DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsMySql, checkTables)
      case ErrorCondition.KafkaErrorCondition(topics) =>
        KafkaHealthCheck
          .checkTopics(topics)
          .map(_.toList)
      case ErrorCondition.ExternalHttpErrorCondition(url) =>
        SttpHealthCheck.check(url)

}
