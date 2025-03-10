package domainmodeling.healthcheck

import domainmodeling.healthcheck.Infra.Db.*
import domainmodeling.healthcheck.Infra.HttpConnection.Url
import domainmodeling.healthcheck.Infra.Kafka.*

object Example1 {
  import Dsl.*
  val dbType  = DbType.Postgres
  val dbConnectionString = ConnectionString("jdbc:postgresql://localhost:5432/myPgDb")
  val dbCreds = Credentials(Username("user"), Password("password"))

  val kafkaTopic = Topic("events")
  val kafkaBootstrapServers = BootstrapServers("kafka:9092")
  
  val errorCondition = dbErrorCondition(dbType, TableName("user"), TableName("access_token")) || kafkaErrorCondition(kafkaTopic, kafkaBootstrapServers)
}

object Example2 {
  import Dsl.*

  val dbType = DbType.MySql
  val dbConnectionString = ConnectionString("jdbc:postgresql://localhost:3306/myMysqlDb")
  val dbCreds = Credentials(Username("user"), Password("password"))
  
  val errorCondition = dbErrorCondition(dbType, TableName("user"), TableName("access_token")) || getHttp2xx(Url("https://httpbin.org/status/200"))

}