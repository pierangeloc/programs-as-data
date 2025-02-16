package domainmodeling.healthcheck

import neotype.Newtype

object Source extends Newtype[String]

type Source = Source.Type

object Message extends Newtype[String]

type Message = Message.Type
