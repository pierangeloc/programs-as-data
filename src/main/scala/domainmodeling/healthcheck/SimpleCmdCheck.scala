package domainmodeling.healthcheck

import domainmodeling.healthcheck.Infra.Db.DbType.Postgres
import domainmodeling.healthcheck.infra.{DbConnectionParams, Doobie, KafkaParams, KafkaUtils}
import sttp.client3.httpclient.zio.HttpClientZioBackend
import zio.kafka.admin.{AdminClient, AdminClientSettings}
import zio.*


object SimpleCmdCheck extends ZIOAppDefault:

  val kafkaParams = ZLayer.succeed(KafkaUtils.adminClientSettings(KafkaParams(List("localhost:9092"))))
  val dbParams = ZLayer.succeed(DbConnectionParams("jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres", 16))

  def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIOInterpreter.interpret(Example1.errorCondition)
      .flatMap(errors => ZIO.logInfo("Errors: " + errors.mkString(", ")))
      .repeat(Schedule.fixed(5.seconds))
      .provide(
      Doobie.transactor(Postgres),
      dbParams,
      kafkaParams,
      AdminClient.live,
      HttpClientZioBackend.layer()
    )


