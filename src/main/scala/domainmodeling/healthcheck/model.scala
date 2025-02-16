package domainmodeling.healthcheck

import domainmodeling.healthcheck.ErrorCondition.{And, DBErrorCondition, ExternalHttpErrorCondition, KafkaErrorCondition, Or}

case class StatusError(source: Source, message: Message)

sealed trait ErrorCondition { self =>
  def &&(other: ErrorCondition): ErrorCondition = And(self, other)
  def ||(other: ErrorCondition): ErrorCondition = Or(self, other)
}

object ErrorCondition {
  case class And(left: ErrorCondition, right: ErrorCondition)                      extends ErrorCondition
  case class Or(left: ErrorCondition, right: ErrorCondition)                       extends ErrorCondition
  case class DBErrorCondition(dbType: DbType)                                      extends ErrorCondition
  case class KafkaErrorCondition(topic: Topic, bootstrapServers: BootstrapServers) extends ErrorCondition
  case class ExternalHttpErrorCondition(url: Url)                               extends ErrorCondition
}

object Dsl {
  def dbErrorCondition(dbType: String): ErrorCondition = dbType.toLowerCase match {
    case "postgres" => DBErrorCondition(DbType.Postgres)
    case "mysql"    => DBErrorCondition(DbType.MySql)
  }
  
  def kafkaErrorCondition(topic: String, bootstrapServers: String): ErrorCondition =
    KafkaErrorCondition(Topic(topic), BootstrapServers(bootstrapServers))
    
  def getHttp2xx(url: String): ErrorCondition = ExternalHttpErrorCondition(Url(url))
}



