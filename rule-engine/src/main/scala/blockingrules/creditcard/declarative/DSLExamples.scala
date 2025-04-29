package blockingrules.creditcard.declarative

import blockingrules.creditcard.declarative.BlockingLogicDeclarative.DSL
import blockingrules.creditcard.declarative.BlockingLogicDeclarative.DSL.fraudProbabilityExceeds
import blockingrules.creditcard.model.basetypes.{Country, Probability, ShopCategory}

object DSLExamples {
  import DSL.*
  val blockingRule1 = (purchaseCountryIsOneOf(Country.UK) && purchaseCategoryIsOneOf(ShopCategory.Gambling)) ||
    (purchaseCountryIsOneOf(Country.China) && purchaseCategoryIsOneOf(ShopCategory.Electronics)) ||
    (purchaseCountryIsOneOf(Country.Italy) && purchaseCategoryIsOneOf(
      ShopCategory.Gambling
    ) && purchaseAmountExceeds(
      1000
    )) ||
    (purchaseCountryIsOneOf(Country.Netherlands) && purchaseAmountExceeds(500)) ||
    (purchaseCountryIsOneOf(Country.US) && purchaseAmountExceeds(1000) && fraudProbabilityExceeds(Probability(0.8)))

  object Evolution {
    val br1 = purchaseCountryIsOneOf(Country.China) && purchaseCategoryIsOneOf(ShopCategory.Electronics)
    val br2 = br1 || (purchaseCountryIsOneOf(Country.UK) && purchaseCategoryIsOneOf(ShopCategory.Gambling))
    val br3 = br2 || (purchaseCountryIsOneOf(Country.Italy) && purchaseCategoryIsOneOf(
      ShopCategory.Gambling,
      ShopCategory.Adult
    ) && purchaseAmountExceeds(
      1000
    ) && fraudProbabilityExceeds(Probability(0.8)))
  }

}

/*
What kind of WHERE clauses should result from the application of these rules?


 */
