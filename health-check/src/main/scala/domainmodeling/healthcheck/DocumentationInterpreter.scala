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

object DocumentationInterpreter {
  private object Colors {
    val RED = "\u001B[31m"
    val YELLOW = "\u001B[33m"
    val GREEN = "\u001B[32m"
    val RESET = "\u001B[0m"
    val BOLD = "\u001B[1m"
  }

  def interpret(
                 errorCondition: ErrorCondition
               ): String =  {
    def interpretInner(condition: ErrorCondition, depth: Int = 0, isFirstOr: Boolean = true): String = {
      val indent = "  " * depth
      condition match {
        case ErrorCondition.Or(left, right) =>
          val header = if (isFirstOr) s"${indent}${Colors.BOLD}Either of:${Colors.RESET}\n" else ""
          s"""$header${interpretInner(left, depth, false)}
             |${indent}${Colors.BOLD}OR${Colors.RESET}
             |${interpretInner(right, depth, false)}""".stripMargin

        case ErrorCondition.DBErrorCondition(dbType, tables) =>
          s"""${indent}${Colors.YELLOW}Database Check (${dbType}):${Colors.RESET}
             |${indent}  Required tables: ${Colors.GREEN}${tables.mkString(", ")}${Colors.RESET}""".stripMargin

        case ErrorCondition.KafkaErrorCondition(topics) =>
          s"""${indent}${Colors.YELLOW}Kafka Check:${Colors.RESET}
             |${indent}  Required topics: ${Colors.GREEN}${topics.map(_.unwrap).mkString(", ")}${Colors.RESET}""".stripMargin

        case ErrorCondition.HttpErrorCondition(url) =>
          s"""${indent}${Colors.YELLOW}HTTP Check:${Colors.RESET}
             |${indent}  URL: ${Colors.GREEN}${url.unwrap}${Colors.RESET}""".stripMargin
      }
    }

    interpretInner(errorCondition)
  }


}
