package domainmodeling.healthcheck

import neotype.Newtype

object Infra:
  object Db:
    enum DbType:
      case Postgres, MySql
    object ConnectionString extends Newtype[String]
    type ConnectionString = ConnectionString.Type
    
    object Username extends Newtype[String]
    type Username = Username.Type
    object Password extends Newtype[String]
    type Password = Password.Type
    case class Credentials(username: Username, password: Password)
  
  object Kafka:
    object Topic extends Newtype[String]
  
    type Topic = Topic.Type
  
    object BootstrapServers extends Newtype[String]
  
    type BootstrapServers = BootstrapServers.Type
  
  object HttpConnection:
    object Url extends Newtype[String]
    type Url = Url.Type
