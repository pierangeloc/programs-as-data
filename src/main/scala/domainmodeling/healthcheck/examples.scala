package domainmodeling.healthcheck


object Example1 {
  import Dsl.*

  val errorCondition = dbErrorCondition("postgres") && kafkaErrorCondition("events", "kafka:9092")
}

object Example2 {
  import Dsl.*

  val errorCondition = dbErrorCondition("mysql") || getHttp2xx("http://localhost:8080")
}