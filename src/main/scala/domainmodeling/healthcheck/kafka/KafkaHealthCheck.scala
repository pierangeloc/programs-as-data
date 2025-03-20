package domainmodeling.healthcheck.kafka

import domainmodeling.healthcheck.Infra.Kafka.Topic
import domainmodeling.healthcheck.{Message, Source, StatusError}
import zio.*
import zio.kafka.admin.*
import neotype.unwrap

object KafkaHealthCheck {

  def checkTopics(topics: List[Topic]): URIO[AdminClient, Option[StatusError]] =
    (for {
      adminClient <- ZIO.service[AdminClient]
      topicDescriptions <- adminClient
                             .describeTopics(topics.map(_.unwrap))
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
        ))
      .catchAllCause(c =>
        ZIO.logErrorCause("Error calling Kafka", c) *> ZIO.some(
          StatusError(Source("Kafka"), Message("Error performing the topic existence check"))
        )
      )
      .timeout(3.seconds)
      .map {
        case Some(r) => r
        case None    => Some(StatusError(Source("Kafka"), Message("Kafka Operation timed out")))
      }
}
