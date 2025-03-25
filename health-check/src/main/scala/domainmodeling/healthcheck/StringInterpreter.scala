package domainmodeling.healthcheck

import domainmodeling.healthcheck.InfraModel.Db.DbType
import domainmodeling.healthcheck.db.DoobieZIOdBHealthcheck
import domainmodeling.healthcheck.http.SttpHealthCheck
import domainmodeling.healthcheck.kafka.KafkaHealthCheck
import doobie.*
import neotype.*
import sttp.client3.httpclient.zio.{HttpClientZioBackend, SttpClient}

object StringInterpreter {
  def interpret(
    errorCondition: ErrorCondition
  ): String =
    errorCondition match
      case ErrorCondition.Or(left, right) =>
        s"""${interpret(left)}
          |- or -
          | ${interpret(right)}""".stripMargin
      case ErrorCondition.DBErrorCondition(DbType.Postgres, checkTables) =>
        s"Tables ${checkTables.mkString(", ")} do not exist in Postgres"
      case ErrorCondition.DBErrorCondition(DbType.MySql, checkTables) =>
        s"Tables ${checkTables.mkString(", ")} do not exist in MySQL"
      case ErrorCondition.KafkaErrorCondition(topics) =>
        s"Topics ${topics.mkString(", ")} do not exist in Kafka"
      case ErrorCondition.ExternalHttpErrorCondition(url) =>
        s"GET $url does not return a 2xx status code"

}
