package domainmodeling.healthcheck

import domainmodeling.healthcheck.Infra.Db.DbType
import doobie.Transactor
import doobie.free.connection.ConnectionIO
import zio.{Task, UIO, URIO, ZIO}


object ZIOInterpreter {
  def interpret(errorCondition: ErrorCondition): URIO[Transactor[Task], List[StatusError]] = {
    errorCondition match

      case ErrorCondition.Or(left, right) => for {
        leftErrors <- interpret(left)
        rightErrors <- interpret(right)
      } yield
        leftErrors ++ rightErrors
      case ErrorCondition.DBErrorCondition(DbType.Postgres) => 
        for {
          tx <- ZIO.service[Transactor[Task]]
        }

      case ErrorCondition.DBErrorCondition(DbType.MySql) => ???
      case ErrorCondition.KafkaErrorCondition(topic, bootstrapServers) => ???
      case ErrorCondition.ExternalHttpErrorCondition(url) => ???
  }
}

object DBHealthcheck {
  import doobie.*
  import doobie.implicits.*
  import cats.implicits.*
  import zio.interop.catz.*
  
  def existsPostgres(tableName: String): ConnectionIO[Boolean] =
    sql"""SELECT EXISTS (
      |  SELECT * FROM
      |    information_schema.tables
      |  WHERE
      |    table_name LIKE 'public' AND 
      |    table_type LIKE 'BASE TABLE' AND 
      |    table_name = $tableName
       )""".stripMargin.query[Boolean].unique
    
  
  def existsMySql(tableName: String): ConnectionIO[Boolean] =
    sql"""select exists (
       |  select 1 from information_schema.tables
       |  where table_name = $tableName
       )""".stripMargin.query[Int].map(_ > 0).unique
}