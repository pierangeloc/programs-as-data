package blockingrules.creditcard

import blockingrules.creditcard.model.{CreditCard, Purchase}
import neotype.Newtype
import zio.UIO

trait FraudScoreService {
  def getFraudScore(creditCard: CreditCard, purchase: Purchase): UIO[FraudScoreService.Score]
}

object FraudScoreService {
  object Score extends Newtype[Double] {
    override  inline def  validate(input: Double) = if (input >= 0.0 && input <= 1.0) true else false
  }
  type Score = Score.Type
}
