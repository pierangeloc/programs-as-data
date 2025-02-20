package domainmodeling.healthcheck

import domainmodeling.healthcheck.ErrorCondition.{And, DBErrorCondition, ExternalHttpErrorCondition, KafkaErrorCondition, Or}
import domainmodeling.healthcheck.Infra.Db.{ConnectionString, Credentials, DbType}
import domainmodeling.healthcheck.Infra.Kafka.{BootstrapServers, Topic}
import domainmodeling.healthcheck.Infra.HttpConnection.Url

sealed trait ErrorCondition { self =>
  def &&(other: ErrorCondition): ErrorCondition = And(self, other)
  def ||(other: ErrorCondition): ErrorCondition = Or(self, other)
}

object ErrorCondition {
  case class And(left: ErrorCondition, right: ErrorCondition)                      extends ErrorCondition
  case class Or(left: ErrorCondition, right: ErrorCondition)                       extends ErrorCondition
  case class DBErrorCondition(dbType: DbType, connectionString: ConnectionString, credentials: Credentials)  extends ErrorCondition
  case class KafkaErrorCondition(topic: Topic, bootstrapServers: BootstrapServers) extends ErrorCondition
  case class ExternalHttpErrorCondition(url: Url)                                  extends ErrorCondition
}
