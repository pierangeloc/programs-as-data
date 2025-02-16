package domainmodeling.healthcheck

import domainmodeling.healthcheck.ErrorCondition.And
import neotype.Newtype

object Source extends Newtype[String]
type Source = Source.Type

object Message extends Newtype[String]
type Message = Message.Type

case class StatusError(source: Source, message: Message)

sealed trait ErrorCondition { self =>
  def &&(other: ErrorCondition): ErrorCondition = And(self, other)
  def ||(other: ErrorCondition): ErrorCondition = Or(self, other)
}

object ErrorCondition:
  case class And(left: ErrorCondition, right: ErrorCondition) extends ErrorCondition
  case class Or(left: ErrorCondition, right: ErrorCondition) extends ErrorCondition