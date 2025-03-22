package domainmodeling.healthcheck

import domainmodeling.healthcheck.ErrorCondition.{DBErrorCondition, ExternalHttpErrorCondition, KafkaErrorCondition}
import domainmodeling.healthcheck.Infra.Db.{DbType, TableName}
import domainmodeling.healthcheck.Infra.HttpConnection.Url
import domainmodeling.healthcheck.Infra.Kafka.{BootstrapServers, Topic}

object Dsl:
  def dbErrorCondition(dbType: DbType, checkTables: TableName*): ErrorCondition =
    DBErrorCondition(dbType, checkTables.toList)

  def kafkaErrorCondition(topics: Topic*): ErrorCondition =
    KafkaErrorCondition(topics.toList)

  def getHttp2xx(url: Url): ErrorCondition = ExternalHttpErrorCondition(url)
