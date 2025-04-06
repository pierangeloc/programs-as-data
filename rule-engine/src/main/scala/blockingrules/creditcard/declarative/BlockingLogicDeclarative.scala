package blockingrules.creditcard.declarative

import blockingrules.creditcard.declarative.MermaidInterpreter.{MermaidRenderable, Shape}
import blockingrules.creditcard.model.basetypes.*
import blockingrules.creditcard.model.{CreditCard, Purchase}
import blockingrules.creditcard.{CreditCardFlaggedService, FraudScoreService, declarative}
import cats.Show
import datastructures.Tree
import neotype.*
import zio.*

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.GZIPOutputStream

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


}


