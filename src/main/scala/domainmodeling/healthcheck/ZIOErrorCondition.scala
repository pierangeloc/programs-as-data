package domainmodeling.healthcheck

import zio.UIO


object ZIOInterpreter {
  def interpret(errorCondition: ErrorCondition): UIO[List[StatusError]] = {
    errorCondition match
      //TODO: think of an output that allows us to model the OR/AND conditions. List is not sufficient
      case ErrorCondition.And(left, right) => for {
        leftErrors <- interpret(left)
        rightErrors <- interpret(right)
      } yield
        leftErrors ++ rightErrors
      case ErrorCondition.Or(left, right) => for {
        leftErrors <- interpret(left)
        rightErrors <- interpret(right)
      } yield
        leftErrors ++ rightErrors
      case ErrorCondition.DBErrorCondition(dbType, connectionString, credentials) => ???
      case ErrorCondition.KafkaErrorCondition(topic, bootstrapServers) => ???
      case ErrorCondition.ExternalHttpErrorCondition(url) => ???
  }
}
