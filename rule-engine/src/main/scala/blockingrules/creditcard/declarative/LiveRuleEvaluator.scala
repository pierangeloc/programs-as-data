package blockingrules.creditcard.declarative

import blockingrules.creditcard.{CreditCardFlaggedService, FraudScoreService}
import blockingrules.creditcard.declarative.BlockingLogicDeclarative.BlockingRule
import blockingrules.creditcard.model.{CreditCard, Purchase}
import zio.{UIO, ZIO}
import neotype.*

/**
 * With a bit of extra boilerplate, we defined a rigorous set of constructors
 * and combinators that we can use to express all the possible complicated
 * rules that our business counterparts might require
 */
object LiveRuleEvaluator {
  case class Input(
                    creditCard: CreditCard,
                    purchase: Purchase
                  )

  private def purchaseOccursInCountry(rule: BlockingRule.PurchaseOccursInCountry)(input: Input): UIO[Boolean] =
    ZIO.succeed(input.purchase.inCountry == rule.country)

  private def purchaseCategoryEquals(rule: BlockingRule.PurchaseCategoryEquals)(input: Input): UIO[Boolean] =
    ZIO.succeed(input.purchase.category == rule.purchaseCategory)

  private def purchaseAmountExceeds(rule: BlockingRule.PurchaseAmountExceeds)(input: Input): UIO[Boolean] =
    ZIO.succeed(input.purchase.amount.unwrap > rule.amount)

  private def creditCardFlagged(ccFlaggedService: CreditCardFlaggedService)(input: Input): UIO[Boolean] =
    ccFlaggedService.isFlagged(input.creditCard.cardNumber)

  private def fraudProbability(rule: BlockingRule.FraudProbabilityExceeds, fraudScoreService: FraudScoreService)(
    input: Input
  ): UIO[Boolean] =
    fraudScoreService
      .getFraudScore(input.creditCard, input.purchase)
      .map(score => score.unwrap > rule.threshold.unwrap)

  def evaluate(rule: BlockingRule, ccFlaggedService: CreditCardFlaggedService, fraudScoreService: FraudScoreService)(
    input: Input
  ): UIO[Boolean] = {
    def eval(rule: BlockingRule): UIO[Boolean] = rule match {
      case BlockingRule.PurchaseOccursInCountry(country) =>
        purchaseOccursInCountry(BlockingRule.PurchaseOccursInCountry(country))(input)
      case BlockingRule.PurchaseCategoryEquals(purchaseCategory) =>
        purchaseCategoryEquals(BlockingRule.PurchaseCategoryEquals(purchaseCategory))(input)
      case BlockingRule.PurchaseAmountExceeds(amount) =>
        purchaseAmountExceeds(BlockingRule.PurchaseAmountExceeds(amount))(input)
      case BlockingRule.CreditCardFlagged() => creditCardFlagged(ccFlaggedService)(input)
      case BlockingRule.FraudProbabilityExceeds(threshold) =>
        fraudProbability(BlockingRule.FraudProbabilityExceeds(threshold), fraudScoreService)(input)
      case BlockingRule.And(l, r) => eval(l).zipWith(eval(r))(_ && _)
      case BlockingRule.Or(l, r)  => eval(l).zipWith(eval(r))(_ || _)
    }

    eval(rule)
  }
}
