package domainmodeling.healthcheck

import domainmodeling.healthcheck.Infra.Db.*
import domainmodeling.healthcheck.Infra.HttpConnection.Url
import domainmodeling.healthcheck.Infra.Kafka.*

object Example1 {
  import Dsl.*
  val dbType  = DbType.Postgres

  val kafkaTopic = Topic("events")
  
  val errorCondition = dbErrorCondition(dbType, TableName("user"), TableName("access_token")) || kafkaErrorCondition(kafkaTopic)
}

object Example2 {
  import Dsl.*

  val dbType = DbType.MySql
  
  val errorCondition = dbErrorCondition(dbType, TableName("user"), TableName("access_token")) || getHttp2xx(Url("https://httpbin.org/status/200"))

}