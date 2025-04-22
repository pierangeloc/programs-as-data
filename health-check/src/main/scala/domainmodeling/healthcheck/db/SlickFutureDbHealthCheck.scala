// In file: domainmodeling/healthcheck/db/SlickFutureDbHealthCheck.scala
package domainmodeling.healthcheck.db

import domainmodeling.infra.InfraModel.Db.TableName
import domainmodeling.healthcheck.{Message, Source, StatusError}
import slick.jdbc.JdbcProfile
import neotype.*

import scala.concurrent.{ExecutionContext, Future}

object SlickFutureDbHealthCheck {

  def checkPostgresTables(db: slick.jdbc.JdbcBackend#Database, jdbcProfile: JdbcProfile, tables: List[TableName])
                         (implicit ec: ExecutionContext): Future[List[StatusError]] = {
    import jdbcProfile.api._

    val futures = tables.map { tableName =>
      val tableName_str = tableName.unwrap

      val query = sql"""
        SELECT EXISTS (
          SELECT * FROM information_schema.tables
          WHERE table_schema LIKE 'public' AND
          table_type LIKE 'BASE TABLE' AND
          table_name = $tableName_str
        )
      """.as[Boolean].head

      db.run(query).map(exists => (tableName, exists))
    }

    Future.sequence(futures).map { results =>
      if (results.forall(_._2)) {
        List.empty
      } else {
        val existingTables = results.filter(_._2).map(_._1.unwrap).mkString(", ")
        val missingTables = results.filterNot(_._2).map(_._1.unwrap).mkString(", ")

        List(StatusError(
          Source("Postgres"),
          Message(s"Required tables [$existingTables] exist but [$missingTables] do not exist.")
        ))
      }
    }.recover {
      case ex: Exception =>
        List(StatusError(Source("Postgres"), Message(s"Error checking PostgreSQL tables: ${ex.getMessage}")))
    }
  }

  def checkMySqlTables(db: slick.jdbc.JdbcBackend#Database, jdbcProfile: JdbcProfile, tables: List[TableName])
                      (implicit ec: ExecutionContext): Future[List[StatusError]] = {
    import jdbcProfile.api._

    val futures = tables.map { tableName =>
      val tableName_str = tableName.unwrap

      val query = sql"""
        SELECT EXISTS (
          SELECT 1 FROM information_schema.tables
          WHERE table_name = $tableName_str
        )
      """.as[Int].head.map(_ > 0)

      db.run(query).map(exists => (tableName, exists))
    }

    Future.sequence(futures).map { results =>
      if (results.forall(_._2)) {
        List.empty
      } else {
        val existingTables = results.filter(_._2).map(_._1.unwrap).mkString(", ")
        val missingTables = results.filterNot(_._2).map(_._1.unwrap).mkString(", ")

        List(StatusError(
          Source("MySQL"),
          Message(s"Required tables [$existingTables] exist but [$missingTables] do not exist.")
        ))
      }
    }.recover {
      case ex: Exception =>
        List(StatusError(Source("MySQL"), Message(s"Error checking MySQL tables: ${ex.getMessage}")))
    }
  }
}