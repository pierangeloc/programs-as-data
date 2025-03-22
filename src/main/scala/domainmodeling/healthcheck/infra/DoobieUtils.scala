package domainmodeling.healthcheck.infra

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import domainmodeling.healthcheck.Infra.Db.DbType
import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.util.log.LogEvent
import zio.{Cause, Task, ZIO, ZLayer}
import zio.interop.catz.*

case class DbConnectionParams(
  url: String,
  username: String,
  password: String,
  maxConnections: Int
)

object Hikari {
  def config(connectionParams: DbConnectionParams, dbType: DbType): HikariConfig = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(connectionParams.url)
    hikariConfig.setUsername(connectionParams.username)
    hikariConfig.setPassword(connectionParams.password)
    hikariConfig.setMaximumPoolSize(connectionParams.maxConnections)
    dbType match {
      case DbType.Postgres => hikariConfig.setDriverClassName("org.postgresql.Driver")
      case DbType.MySql    => hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver")
    }
    hikariConfig
  }

  def datasource(connectionParams: DbConnectionParams, dbType: DbType): HikariDataSource =
    new HikariDataSource(config(connectionParams, dbType))
}

object Doobie {
  def transactor(dbType: DbType): ZLayer[DbConnectionParams, Throwable, Transactor[Task]] = ZLayer.scoped {
    for {
      connectionParams <- ZIO.service[DbConnectionParams]
      hikariConfig      = Hikari.config(connectionParams, dbType)
      xa <- HikariTransactor
              .fromHikariConfig[Task](
                hikariConfig,
                Some {
                  case doobie.util.log.ProcessingFailure(sql, _, _, _, _, failure) =>
                    ZIO.logErrorCause("Error processing query. SQL = " + sql, Cause.die(failure))
                  case doobie.util.log.ExecFailure(sql, _, _, _, failure) =>
                    ZIO.logErrorCause("Error executing query. SQL = " + sql, Cause.die(failure))
                  case doobie.util.log.Success(_, _, _, _, _) =>
                    ZIO.unit
                }
              )
              .toScopedZIO
    } yield xa
  }
}
