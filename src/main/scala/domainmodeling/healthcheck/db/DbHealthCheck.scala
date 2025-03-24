package domainmodeling.healthcheck.db

import domainmodeling.healthcheck.InfraModel.Db.TableName
import domainmodeling.healthcheck.{Message, Source, StatusError}
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import neotype.*
import zio.*
import zio.interop.catz.*
import zio.{Cause, Task, ZIO}

object DoobieZIOdBHealthcheck {

  def existsPostgres(tableName: TableName): ConnectionIO[Boolean] = {
    val s: String = tableName.unwrap
    sql"""SELECT EXISTS (
         |  SELECT * FROM
         |    information_schema.tables
         |  WHERE
         |    table_schema LIKE 'public' AND
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
    checkTables: List[TableName]
  ): ZIO[Transactor[Task], Nothing, List[StatusError]] = {

    val test = for {
      tx      <- ZIO.service[Transactor[Task]]
      results <- ZIO.foreach(checkTables)(tn => checkTx(tn).transact(tx).map(exists => (tn, exists))).orDie
      _       <- ZIO.logInfo("Checking tables: " + results.map { case (tn, exists) => s"$tn -> $exists" }.mkString(", "))
      res <- if (results.forall(_._2)) ZIO.succeed(List())
             else
               ZIO.succeed(
                 List(
                   StatusError(
                     Source("Postgres"),
                     Message(s"Expected tables did not exist [${checkTables.map(_.unwrap).mkString(", ")}] ")
                   )
                 )
               )
    } yield res

    test
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
  }
}
