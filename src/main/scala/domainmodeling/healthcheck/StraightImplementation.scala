package domainmodeling.healthcheck

import domainmodeling.healthcheck.Infra.Db.{DbType, TableName}
import domainmodeling.healthcheck.db.DoobieZIOdBHealthcheck
import doobie.Transactor
import zio.*

object StraightImplementation {
  def checkErrors(): URIO[Transactor[Task], List[StatusError]] = {
    def dbCheck(dbType: DbType, checkTables: List[TableName]): ZIO[Transactor[Task], Nothing, List[StatusError]] =
      for {
        tx <- ZIO.service[Transactor[Task]]
        res <- dbType match
                 case DbType.Postgres =>
                   DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsPostgres, tx, checkTables)
                 case DbType.MySql =>
                   DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsMySql, tx, checkTables)

      } yield res

    ZIO.foreach(List(dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2")))))(identity).map(_.flatten)
  }

}
