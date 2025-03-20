package domainmodeling.healthcheck.kafka

import domainmodeling.healthcheck.Infra.Kafka.Topic
import domainmodeling.healthcheck.{Message, Source, StatusError}
import zio.{RIO, ZIO}
import zio.kafka.admin.*
import neotype.unwrap

object KafkaHealthCheck {

  def describeTopics(topics: List[Topic]): RIO[AdminClient, Option[StatusError]] =
    for {
      adminClient       <- ZIO.service[AdminClient]
      topicDescriptions <- adminClient.describeTopics(topics.map(_.unwrap))
    } yield
      if (topicDescriptions.size == topics.size) None
      else
        Some(
          StatusError(
            Source("Kafka"),
            Message(
              s"Expected Topics did not exist. Expected: [${topics.mkString(",")}], Got: [${topicDescriptions.keys.mkString(",")}]"
            )
          )
        )
}
