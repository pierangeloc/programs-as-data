package domainmodeling.healthcheck

import neotype.Newtype

enum DbType:
  case Postgres, MySql

object Topic extends Newtype[String]

type Topic = Topic.Type

object BootstrapServers extends Newtype[String]

type BootstrapServers = BootstrapServers.Type

object Url extends Newtype[String]
type Url = Url.Type
