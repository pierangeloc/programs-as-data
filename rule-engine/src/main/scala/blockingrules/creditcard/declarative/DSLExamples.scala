package blockingrules.creditcard.declarative

import blockingrules.creditcard.declarative.BlockingLogicDeclarative.DSL
import blockingrules.creditcard.declarative.BlockingLogicDeclarative.DSL.{
  fraudProbabilityExceeds,
  purchaseAmountExceeds,
  purchaseCategoryEquals,
  purchaseInCountry
}
import blockingrules.creditcard.model.basetypes.{Country, Probability, PurchaseCategory}

object DSLExamples {
  import DSL.*
  val example1 = (purchaseInCountry(Country.UK) && purchaseCategoryEquals(PurchaseCategory.Crypto)) ||
    (purchaseInCountry(Country.China) && purchaseCategoryEquals(PurchaseCategory.Electronics)) ||
    (purchaseInCountry(Country.Italy) && purchaseCategoryEquals(PurchaseCategory.Gambling) && purchaseAmountExceeds(
      1000
    )) ||
    ((purchaseInCountry(Country.Netherlands) && purchaseAmountExceeds(500)) || (purchaseInCountry(
      Country.US
    ) && purchaseAmountExceeds(1000))) && fraudProbabilityExceeds(Probability(0.8))
}
