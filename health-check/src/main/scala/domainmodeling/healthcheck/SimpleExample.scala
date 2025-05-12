package domainmodeling.healthcheck

import domainmodeling.infra.InfraModel.Db.DbType.Postgres
import domainmodeling.infra.{DbConnectionParams, Doobie, KafkaParams, KafkaUtils}
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.kafka.admin.{AdminClient, AdminClientSettings}
import zio.*
import zio.logging.backend.SLF4J

object SimpleExample extends ZIOAppDefault:

  val slf4jLogger: ULayer[Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = slf4jLogger

  val kafkaParams = ZLayer.succeed(KafkaUtils.adminClientSettings(KafkaParams(List("localhost:29092"))))
  val dbParams = ZLayer.succeed(
    DbConnectionParams(
      url = "jdbc:postgresql://localhost:5432/mydb",
      username = "postgres",
      password = "postgres",
      maxConnections = 16
    )
  )

  def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] =
    (zio.Console.printLine("Performing Healthcheck as configured:") *>
      zio.Console.printLine(DocumentationInterpreter.interpret(Example1.errorCondition))) /**>
      
      ZIOInterpreter
        .interpret(Example1.errorCondition)
        .flatMap(errors => zio.Console.printLine("Errors: " + errors.mkString(", "))))
      .repeat(Schedule.fixed(5.seconds))
      .provide(
        Doobie.transactor(Postgres),
        dbParams,
        kafkaParams,
        AdminClient.live,
        HttpClientZioBackend.layer()
      )*/
