package domainmodeling.healthcheck

import domainmodeling.infra.InfraModel.Db.DbType
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
      case ErrorCondition.HttpErrorCondition(url) =>
        SttpHealthCheck.check(url)

}

///make a new Animal Trait


/*
 * Experimental:

 * The problem with the interpreter above is that it takes all the possible dependencies, even if the actual error condition does not need all of them.
 * E.g. if the error condition is just about a DBErrorCondition, it does not need the kafka AdminClient and SttpClient dependencies.
 * We want to use some type sorcery to determine the right output type
 */

object SmartInterpreter {

  trait WithRequirement[-R, A]
  case class DBErrorConditionWithRequirement(tx: Transactor[Task], original: ErrorCondition.DBErrorCondition)
      extends WithRequirement[Transactor[Task], ErrorCondition.DBErrorCondition]
  case class KafkaErrorConditionWithRequirement(adminClient: AdminClient, original: ErrorCondition.KafkaErrorCondition)
      extends WithRequirement[AdminClient, ErrorCondition.KafkaErrorCondition]

  case class HttpErrorConditionWithRequirement(adminClient: AdminClient, original: ErrorCondition.KafkaErrorCondition)
    extends WithRequirement[AdminClient, ErrorCondition.KafkaErrorCondition]
}
