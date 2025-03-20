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
          kafkaCheck(List(Topic("topic1"), Topic("topic2")))
        )
      )
      .map(_.flatten)
  }

}
