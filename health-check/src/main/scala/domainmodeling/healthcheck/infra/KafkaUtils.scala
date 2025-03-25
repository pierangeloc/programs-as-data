package domainmodeling.healthcheck.infra

import zio.kafka.admin.AdminClientSettings

case class KafkaParams(bootstrapServers: List[String])

object KafkaUtils:
  def adminClientSettings(kafkaParams: KafkaParams) =
    AdminClientSettings(kafkaParams.bootstrapServers)

  
  
