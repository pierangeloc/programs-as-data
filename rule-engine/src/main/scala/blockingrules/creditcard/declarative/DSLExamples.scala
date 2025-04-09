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
  val example1 = (purchaseInOneOfCountries(Country.UK) && purchaseCategoryIsOneOf(PurchaseCategory.Adult)) ||
    (purchaseInOneOfCountries(Country.China) && purchaseCategoryIsOneOf(PurchaseCategory.Electronics)) ||
    (purchaseInOneOfCountries(Country.Italy) && purchaseCategoryIsOneOf(PurchaseCategory.Gambling) && purchaseAmountExceeds(
      1000
    )) ||
    ((purchaseInOneOfCountries(Country.Netherlands) && purchaseAmountExceeds(500)) || (purchaseInOneOfCountries(
      Country.US
    ) && purchaseAmountExceeds(1000))) && fraudProbabilityExceeds(Probability(0.8))
}
