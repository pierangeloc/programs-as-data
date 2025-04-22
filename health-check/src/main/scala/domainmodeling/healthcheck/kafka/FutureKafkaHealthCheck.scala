package domainmodeling.healthcheck.kafka


import domainmodeling.infra.InfraModel.Kafka.Topic
import domainmodeling.healthcheck.{Message, Source, StatusError}
import org.apache.kafka.clients.admin.{AdminClient => KafkaAdminClient}
import neotype.*

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

object FutureKafkaHealthCheck {
  def checkTopics(adminClient: KafkaAdminClient, topics: List[Topic])(implicit ec: ExecutionContext): Future[List[StatusError]] = {
    Future {
      try {
        val existingTopics = adminClient.listTopics().names().get().asScala.toSet
        val requiredTopics = topics.map(_.unwrap)
        val missingTopics = requiredTopics.filterNot(existingTopics.contains)

        if (missingTopics.isEmpty) {
          List.empty
        } else {
          List(StatusError(
            Source("Kafka"),
            Message(s"Required Kafka topics [${missingTopics.mkString(", ")}] do not exist")
          ))
        }
      } catch {
        case ex: Exception =>
          List(StatusError(
            Source("Kafka"),
            Message(s"Error checking Kafka topics: ${ex.getMessage}")
          ))
      }
    }
  }
}
