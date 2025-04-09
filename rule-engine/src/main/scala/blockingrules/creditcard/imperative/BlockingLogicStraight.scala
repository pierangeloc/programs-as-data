package blockingrules.creditcard.imperative

import blockingrules.creditcard.FraudScoreService
import blockingrules.creditcard.model.basetypes.{Country, PurchaseCategory}
import blockingrules.creditcard.model.{CreditCard, Purchase}
import neotype.*
import zio.*

/**
 * Requirement: Implement the logic to block a credit card based on the
 * following rules:
 *   1. If the operation occurs in a country that is forbidden for the CC, deny
 *      the operation
 *   2. If the operation is of a kind that raises a high risk
 *      of fraud, deny the operation
 *   4. If fraud detection AI determines a high
 *      probability of fraud, deny the operation 5. If the credit card is
 *      expired, deny the operation
 */
object BlockingLogicStraight {

  def isRiskyCategory(purchaseCategory: PurchaseCategory): Boolean =
    purchaseCategory match {
      case PurchaseCategory.Gambling => true
      case PurchaseCategory.Adult  => true
      case _                        => false
    }

  

  /**
   * This solution works, but it comes with a few drawbacks:
   *  1. The logic can do pretty much anything with the given inputs. It can compute ad hoc functions, we have basically no constraints in what we can do here
   *  2. It requires wiring up the FraudScoreService in the environment in order to test it
   *  3. It is strict about the way the fraud score can come from. It must only come from an external service, it can't come from somewhere else, such as configuration or a database
   *  4. One downside of this approach is also that this involvement of the service interferes with the logic definition
   *  5. The plus of an _interpretation_ is that it can be synthesized even by an AI model (not LLM) and deployed, A/B tested etc
   */
  def isBlocked(creditCard: CreditCard, purchase: Purchase): URIO[FraudScoreService, Boolean] =
    if (creditCard.forbiddenCountries.contains(purchase.shop.country))
      ZIO.succeed(true)
    else if (purchase.shop.country == Country.UK && purchase.shop.categories.exists(isRiskyCategory))
      ZIO.succeed(true)
    else // forbid category Electronics in China
    if (purchase.shop.country == Country.China && purchase.shop.categories.contains(PurchaseCategory.Electronics))
      ZIO.succeed(true)
    else if (purchase.shop.country == Country.UK && purchase.shop.categories.contains(PurchaseCategory.Gambling) && purchase.amount.unwrap > 1000)
      ZIO.succeed(false)
    else if ((purchase.shop.country == Country.Netherlands && purchase.amount.unwrap > 500) || (purchase.shop.country == Country.US && purchase.amount.unwrap > 1000))
     for {
      fss <- ZIO.service[FraudScoreService]
      score <- fss.getFraudScore(creditCard, purchase)
      res = score.unwrap > 0.8
    } yield res
    else ZIO.succeed(false)


}
