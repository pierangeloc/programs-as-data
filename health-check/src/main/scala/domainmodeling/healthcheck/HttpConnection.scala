package domainmodeling.healthcheck

import neotype.*

object HttpConnection:
  object Url extends Newtype[String]
  type Url = Url.Type
