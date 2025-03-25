package domainmodeling.healthcheck

import domainmodeling.healthcheck.ErrorCondition.{DBErrorCondition, HttpErrorCondition, KafkaErrorCondition}
import domainmodeling.healthcheck.InfraModel.Db.{DbType, TableName}
import domainmodeling.healthcheck.InfraModel.HttpConnection.Url
import domainmodeling.healthcheck.InfraModel.Kafka.{BootstrapServers, Topic}

object Dsl:
  def dbErrorCondition(dbType: DbType, checkTables: TableName*): ErrorCondition =
    DBErrorCondition(dbType, checkTables.toList)

  def kafkaErrorCondition(topics: Topic*): ErrorCondition =
    KafkaErrorCondition(topics.toList)

  def getHttp2xx(url: Url): ErrorCondition = HttpErrorCondition(url)
