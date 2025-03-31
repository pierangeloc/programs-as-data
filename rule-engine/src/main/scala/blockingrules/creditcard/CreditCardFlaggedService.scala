package blockingrules.creditcard

import blockingrules.creditcard.model.basetypes.CardNumber
import zio.UIO

trait CreditCardFlaggedService {
  def isFlagged(cardNumber: CardNumber): UIO[Boolean]
}
