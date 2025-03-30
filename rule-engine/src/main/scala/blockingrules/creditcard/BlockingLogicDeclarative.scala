package blockingrules.creditcard

import blockingrules.creditcard.model.{CreditCard, Purchase}
import blockingrules.creditcard.model.basetypes.{CardNumber, Country, Probability, PurchaseCategory}
import neotype.*
import zio.*

object BlockingLogicDeclarative {

  sealed trait BlockingRule { self =>
    def &&(other: BlockingRule): BlockingRule = BlockingRule.And(self, other)
    def ||(other: BlockingRule): BlockingRule = BlockingRule.Or(self, other)
  }

  object BlockingRule {
    case class And(l: BlockingRule, r: BlockingRule) extends BlockingRule
    case class Or(l: BlockingRule, r: BlockingRule)  extends BlockingRule

    case class PurchaseOccursInCountry(country: Country)                  extends BlockingRule
    case class PurchaseCategoryEquals(purchaseCategory: PurchaseCategory) extends BlockingRule
    case class PurchaseAmountExceeds(amount: Double)                      extends BlockingRule
    case class FraudProbabilityExceeds(threshold: Probability)            extends BlockingRule
    case class CreditCardFlagged()                                        extends BlockingRule
  }

  object DSL {
    def purchaseInCountry(country: Country): BlockingRule           = BlockingRule.PurchaseOccursInCountry(country)
    def purchaseInOneOfCountries(countries: Country*): BlockingRule = countries.map(purchaseInCountry).reduce(_ || _)
    def purchaseCategoryEquals(purchaseCategory: PurchaseCategory): BlockingRule =
      BlockingRule.PurchaseCategoryEquals(purchaseCategory)
    def purchaseCategoryIsOneOf(purchaseCategories: PurchaseCategory*): BlockingRule =
      purchaseCategories.map(purchaseCategoryEquals).reduce(_ || _)
    def purchaseAmountExceeds(amount: Double): BlockingRule           = BlockingRule.PurchaseAmountExceeds(amount)
    def fraudProbabilityExceeds(threshold: Probability): BlockingRule = BlockingRule.FraudProbabilityExceeds(threshold)
    def creditCardFlagged: BlockingRule                               = BlockingRule.CreditCardFlagged()
  }

  /**
   * With a bit of extra boilerplate, we defined a rigourous set of constructors and combinators that we can use to express all the possible complicated rules
   * that our business counterparts might require
   */

  object LiveInterpreter {


    case class Input(
      creditCard: CreditCard,
      purchase: Purchase,
    )

    trait CreditCardFlaggedService {
      def isFlagged(cardNumber: CardNumber): UIO[Boolean]
    }

    def purchaseOccursInCountry(rule: BlockingRule.PurchaseOccursInCountry)(input: Input): UIO[Boolean] =
      ZIO.succeed(input.purchase.inCountry == rule.country)

    def purchaseCategoryEquals(rule: BlockingRule.PurchaseCategoryEquals)(input: Input): UIO[Boolean] =
      ZIO.succeed(input.purchase.category == rule.purchaseCategory)

    def purchaseAmountExceeds(rule: BlockingRule.PurchaseAmountExceeds)(input: Input): UIO[Boolean] =
        ZIO.succeed(input.purchase.amount.unwrap > rule.amount)

    def creditCardFlagged(rule: BlockingRule.CreditCardFlagged)(input: Input):  URIO[CreditCardFlaggedService, Boolean]=
      ZIO.serviceWithZIO[CreditCardFlaggedService](_.isFlagged(input.creditCard.cardNumber))

    def fraudProbability(rule: BlockingRule.FraudProbabilityExceeds)(input: Input): ZIO[FraudScoreService, Nothing, Any] =
      ZIO.serviceWithZIO[FraudScoreService](_.getFraudScore(input.creditCard, input.purchase)).map(score => score.unwrap > rule.threshold.unwrap)



  }

}
