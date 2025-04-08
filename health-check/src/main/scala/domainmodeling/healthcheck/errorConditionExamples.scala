package domainmodeling.healthcheck

import domainmodeling.infra.InfraModel.Db.*
import domainmodeling.healthcheck.HttpConnection.Url
import domainmodeling.infra.InfraModel.Kafka.*

object Example1 {
  import Dsl.*
  val dbType = DbType.Postgres

  val kafkaTopic = Topic("events")

  val errorCondition =
    dbErrorCondition(dbType, TableName("customer"), TableName("access_token")) ||
      kafkaErrorCondition(kafkaTopic) ||
      getHttp2xx(Url("http://localhost:8080/status/200"))

}

object Example2 {
  import Dsl.*

  val dbType = DbType.MySql

  val errorCondition = dbErrorCondition(dbType, TableName("user"), TableName("access_token")) || getHttp2xx(
    Url("https://httpbin.org/status/200")
  )

}
