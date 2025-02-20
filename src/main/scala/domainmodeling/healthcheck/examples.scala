package domainmodeling.healthcheck

import domainmodeling.healthcheck.Infra.Db.*
import domainmodeling.healthcheck.Infra.Kafka.*

object Example1 {
  import Dsl.*
  val dbType  = DbType.Postgres
  val dbConnectionString = ConnectionString("jdbc:postgresql://localhost:5432/myPGdb")
  val dbCreds = Credentials(Username("user"), Password("password"))

  val kafkaTopic = Topic("events")
  val kafkaBootstrapServers = BootstrapServers("kafka:9092")
  val errorCondition = dbErrorCondition(dbType, dbConnectionString, dbCreds) && kafkaErrorCondition(kafkaTopic, kafkaBootstrapServers)
}

object Example2 {
  import Dsl.*

//  val errorCondition = dbErrorCondition("mysql") || getHttp2xx("https://httpbin.org/status/200")
}