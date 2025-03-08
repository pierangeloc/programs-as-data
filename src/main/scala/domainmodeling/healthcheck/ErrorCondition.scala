package domainmodeling.healthcheck

import domainmodeling.healthcheck.ErrorCondition.{DBErrorCondition, ExternalHttpErrorCondition, KafkaErrorCondition, Or}
import domainmodeling.healthcheck.Infra.Db.{ConnectionString, Credentials, DbType}
import domainmodeling.healthcheck.Infra.Kafka.{BootstrapServers, Topic}
import domainmodeling.healthcheck.Infra.HttpConnection.Url

//TODO: Add an ignore aspect 
sealed trait ErrorCondition { self =>
  def ||(other: ErrorCondition): ErrorCondition = Or(self, other)
}

object ErrorCondition {
  case class Or(left: ErrorCondition, right: ErrorCondition)                       extends ErrorCondition
  
  case class DBErrorCondition(dbType: DbType)  extends ErrorCondition
  case class KafkaErrorCondition(topic: Topic, bootstrapServers: BootstrapServers) extends ErrorCondition
  case class ExternalHttpErrorCondition(url: Url)                                  extends ErrorCondition
}
