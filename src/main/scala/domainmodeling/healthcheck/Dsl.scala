package domainmodeling.healthcheck

import domainmodeling.healthcheck.ErrorCondition.{DBErrorCondition, ExternalHttpErrorCondition, KafkaErrorCondition}
import domainmodeling.healthcheck.Infra.Db.{ConnectionString, Credentials, DbType, TableName}
import domainmodeling.healthcheck.Infra.Kafka.{BootstrapServers, Topic}
import domainmodeling.healthcheck.Infra.HttpConnection.Url

object Dsl:
  def dbErrorCondition(dbType: DbType, checkTables: TableName*): ErrorCondition =
    DBErrorCondition(DbType.Postgres, checkTables.toList)

  def kafkaErrorCondition(topic: Topic, bootstrapServers: BootstrapServers): ErrorCondition =
    KafkaErrorCondition(topic, bootstrapServers)

  def getHttp2xx(url: Url): ErrorCondition = ExternalHttpErrorCondition(url)
