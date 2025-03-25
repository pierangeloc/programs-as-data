package domainmodeling.healthcheck

import domainmodeling.healthcheck.ErrorCondition.{DBErrorCondition, HttpErrorCondition, KafkaErrorCondition, Or}
import domainmodeling.healthcheck.InfraModel.Db.{DbType, TableName}
import domainmodeling.healthcheck.InfraModel.Kafka.Topic
import domainmodeling.healthcheck.InfraModel.HttpConnection.Url

/**
 * 2. ** Composable solution **
 * Let's focus on what we need here: we want a simple, composable way to describe our problem.
 * We want to focus on the systems we are interested in, so let's focus on the systems we want to monitor, and let's provide some attributes
 *
 * For this we use a sum type with the different platforms we want to monitor, and we provide the necessary attributes for each platform.
 *
 * We need also a way to combine these conditions, so we provide a way to combine them with a logical OR.
 * This OR will be also a term of our sum type, so this allows for recursive inclusion
 */
sealed trait ErrorCondition { self =>
  def ||(other: ErrorCondition): ErrorCondition = Or(self, other)
}

object ErrorCondition {
  case class Or(left: ErrorCondition, right: ErrorCondition) extends ErrorCondition

  case class DBErrorCondition(dbType: DbType, checkTables: List[TableName]) extends ErrorCondition
  case class KafkaErrorCondition(topics: List[Topic])                              extends ErrorCondition
  case class HttpErrorCondition(url: Url)                           extends ErrorCondition
}
