package domainmodeling.healthcheck

import domainmodeling.healthcheck.Infra.Db.{DbType, TableName}
import domainmodeling.healthcheck.Infra.HttpConnection.Url
import domainmodeling.healthcheck.Infra.Kafka.Topic
import domainmodeling.healthcheck.db.DoobieZIOdBHealthcheck
import domainmodeling.healthcheck.http.SttpHealthCheck
import domainmodeling.healthcheck.kafka.KafkaHealthCheck
import doobie.Transactor
import zio.*
import zio.kafka.admin.AdminClient


/**
 * 1. **Problem**: We want to check the health of our system. We want to check the health of the database, Kafka, and HTTP dependency.
 * 
 * This is the straight implementation of the health check.
 * It is a simple implementation that checks the health of the database, Kafka, and HTTP dependency. It is simply an executional encoding
 *
 * The downside of this implementation is that it is not composable. If we want to add a new health check, we need to modify the `checkErrors` method.
 * Another downside is that it is bound to a technology: it expects that we use Doobie for the database, zio-kafka for Kafka, and Sttp for HTTP.
 *
 * If this code goes in a common library shared among different services, we would need to have one for our legacy services based on akka-http, another for our new services based on http4s, etc.
 *
 * Moreover, the `checkErrors` method is not testable. We cannot test it without actually connecting to the database, Kafka, and HTTP.
 * This is because the `checkErrors` method is a mix of business logic and side effects.
 */
object StraightImplementation {
  def checkErrors() = {
    def dbCheck(dbType: DbType, checkTables: List[TableName]): ZIO[Transactor[Task], Nothing, List[StatusError]] =
      for {
        res <- dbType match
                 case DbType.Postgres =>
                   DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsPostgres, checkTables)
                 case DbType.MySql =>
                   DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsMySql, checkTables)

      } yield res

    def kafkaCheck(topics: List[Topic]) =
      KafkaHealthCheck.checkTopics(topics).map(_.toList)

    def httpCheck(url: Url) = SttpHealthCheck.check(url)

    ZIO
      .collectAll(
        List(
          dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2"))),
          kafkaCheck(List(Topic("topic1"), Topic("topic2"))),
          httpCheck(Url("http://localhost:8080"))
        )
      )
      .map(_.flatten)
  }

}
