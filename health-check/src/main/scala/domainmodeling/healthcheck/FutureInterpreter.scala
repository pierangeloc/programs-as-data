package domainmodeling.healthcheck

import domainmodeling.infra.InfraModel.Db.DbType
import domainmodeling.healthcheck.db.SlickFutureDbHealthCheck
import domainmodeling.healthcheck.http.FutureHttpHealthCheck
import domainmodeling.healthcheck.kafka.FutureKafkaHealthCheck
import neotype.*
import org.apache.kafka.clients.admin.{AdminClient => KafkaAdminClient}
import sttp.client3.{SttpBackend => SttpFutureBackend}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object FutureSlickInterpreter {
  def interpret( db: slick.jdbc.JdbcBackend#Database,
                 jdbcProfile: slick.jdbc.JdbcProfile,
                 kafkaClient: KafkaAdminClient,
                 httpClient: SttpFutureBackend[Future, Any])(
                 errorCondition: ErrorCondition
               )(implicit
                 ec: ExecutionContext
               ): Future[List[StatusError]] = {

    // Helper method to handle timeouts consistently
    def withTimeout[T](future: Future[T], default: T, timeoutDuration: Duration = 3.seconds): Future[T] = {
      import scala.concurrent.blocking

      val promise = scala.concurrent.Promise[T]()

      // Schedule a timeout
      ec.execute(() => {
        blocking {
          Thread.sleep(timeoutDuration.toMillis)
          promise.trySuccess(default)
        }
      })

      // Complete with actual result if it arrives before timeout
      future.onComplete(result => promise.tryComplete(result))

      promise.future
    }

    errorCondition match {
      case ErrorCondition.Or(left, right) =>
        // Run both checks in parallel
        val leftFuture = interpret(db, jdbcProfile, kafkaClient, httpClient)(left)
        val rightFuture = interpret(db, jdbcProfile, kafkaClient, httpClient)(right)

        // Combine results
        for {
          leftErrors <- leftFuture
          rightErrors <- rightFuture
        } yield leftErrors ++ rightErrors

      case ErrorCondition.DBErrorCondition(DbType.Postgres, checkTables) =>
        withTimeout(
          SlickFutureDbHealthCheck.checkPostgresTables(db,jdbcProfile, checkTables),
          List(StatusError(Source("Postgres"), Message("Database operation timed out")))
        )

      case ErrorCondition.DBErrorCondition(DbType.MySql, checkTables) =>
        withTimeout(
          SlickFutureDbHealthCheck.checkMySqlTables(db, jdbcProfile, checkTables),
          List(StatusError(Source("MySQL"), Message("Database operation timed out")))
        )

      case ErrorCondition.KafkaErrorCondition(topics) =>
        withTimeout(
          FutureKafkaHealthCheck.checkTopics(kafkaClient, topics),
          List(StatusError(Source("Kafka"), Message("Kafka operation timed out"))),
          5.seconds // Kafka may need more time
        )

      case ErrorCondition.HttpErrorCondition(url) =>
        withTimeout(
          FutureHttpHealthCheck.check(httpClient, url),
          List(StatusError(Source("Http"), Message("Http call timed out")))
        )
    }
  }
}