package domainmodeling.healthcheck

import domainmodeling.healthcheck.Infra.Db.{DbType, TableName}
import doobie.*
import doobie.implicits.*
import doobie.free.connection.ConnectionIO
import zio.*
import zio.interop.catz.*
import cats.implicits.*

import java.util.concurrent.TimeoutException

object ZIOInterpreter {
  def interpret(errorCondition: ErrorCondition): URIO[Transactor[Task], List[StatusError]] =
    errorCondition match

      case ErrorCondition.Or(left, right) =>
        for {
          leftErrors  <- interpret(left)
          rightErrors <- interpret(right)
        } yield leftErrors ++ rightErrors
      case ErrorCondition.DBErrorCondition(DbType.Postgres, checkTables) =>
        for {
          tx  <- ZIO.service[Transactor[Task]]
          res <- DBHealthcheck.status(DBHealthcheck.existsPostgres, tx, checkTables)
        } yield res

      case ErrorCondition.DBErrorCondition(DbType.MySql, checkTables)  =>
        for {
          tx  <- ZIO.service[Transactor[Task]]
          res <- DBHealthcheck.status(DBHealthcheck.existsMySql, tx, checkTables)
        } yield res
      case ErrorCondition.KafkaErrorCondition(topic, bootstrapServers) => ???
      case ErrorCondition.ExternalHttpErrorCondition(url)              => ???
}

object DBHealthcheck {
  import doobie.*
  import doobie.implicits.*
  import cats.implicits.*
  import zio.interop.catz.*
  import neotype.*

  def existsPostgres(tableName: TableName): ConnectionIO[Boolean] = {
    val s: String = tableName.unwrap
    sql"""SELECT EXISTS (
         |  SELECT * FROM
         |    information_schema.tables
         |  WHERE
         |    table_name LIKE 'public' AND 
         |    table_type LIKE 'BASE TABLE' AND 
         |    table_name = ${s}
       )""".stripMargin.query[Boolean].unique
  }

  def existsMySql(tableName: TableName): ConnectionIO[Boolean] = {
    val s: String = tableName.unwrap

    sql"""select exists (
         |  select 1 from information_schema.tables
         |  where table_name = ${s}
       )""".stripMargin.query[Int].map(_ > 0).unique
  }

  def status(
    checkTx: TableName => ConnectionIO[Boolean],
    tx: Transactor[Task],
    checkTables: List[TableName]
  ): ZIO[Transactor[Task], Nothing, List[StatusError]] =
    for {
      tx                   <- ZIO.service[Transactor[Task]]
      cx                    = checkTables.map(checkTx).sequence.map(_.reduce(_ && _))
      txTask: Task[Boolean] = cx.transact(tx)
      res <-
        txTask
          .map(res =>
            if (res) List() else List(StatusError(Source("Postgres"), Message("Expected Tables did not exist")))
          )
          .timeout(3.seconds)
          .flatMap {
            case Some(r) => ZIO.succeed(r)
            case None    => ZIO.fail(new Exception("DB Operation timed out"))
          }
          .catchAll(e =>
            ZIO.logErrorCause("Error calling DB", Cause.die(e)) *>
              ZIO.succeed(
                List(StatusError(Source("Postgres"), Message("Error performing the table existence check")))
              )
          )
    } yield res
}
