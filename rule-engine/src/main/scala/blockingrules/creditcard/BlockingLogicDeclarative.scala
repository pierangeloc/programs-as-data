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
    case class And[L <: BlockingRule, R <: BlockingRule](l: L, r: R) extends BlockingRule
    case class Or[L <: BlockingRule, R <: BlockingRule](l: L, r: R)  extends BlockingRule

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
   * With a bit of extra boilerplate, we defined a rigourous set of constructors
   * and combinators that we can use to express all the possible complicated
   * rules that our business counterparts might require
   */

  object LiveInterpreter {

    case class Input(
      creditCard: CreditCard,
      purchase: Purchase
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

    def creditCardFlagged(rule: BlockingRule.CreditCardFlagged)(input: Input): URIO[CreditCardFlaggedService, Boolean] =
      ZIO.serviceWithZIO[CreditCardFlaggedService](_.isFlagged(input.creditCard.cardNumber))

    def fraudProbability(rule: BlockingRule.FraudProbabilityExceeds)(input: Input): URIO[FraudScoreService, Boolean] =
      ZIO
        .serviceWithZIO[FraudScoreService](_.getFraudScore(input.creditCard, input.purchase))
        .map(score => score.unwrap > rule.threshold.unwrap)

//      // The power of Scala3 :heart:
//      /**
//       * Helper type class to compute the ZIO environment required for a given
//       * BlockingRule
//       */
//      type EnvironmentFor[R <: BlockingRule] = R match
//        case BlockingRule.CreditCardFlagged       => CreditCardFlaggedService
//        case BlockingRule.FraudProbabilityExceeds => FraudScoreService
//        case BlockingRule.And[l, r]               => EnvironmentFor[l] & EnvironmentFor[r]
//        case BlockingRule.Or[l, r]                => EnvironmentFor[l] & EnvironmentFor[r]
//        case BlockingRule.PurchaseOccursInCountry => Any
//        case BlockingRule.PurchaseCategoryEquals  => Any
//        case BlockingRule.PurchaseAmountExceeds   => Any
//
//      /**
//       * Function to execute a BlockingRule while enforcing the compiled
//       * environment
//       */
//      def evaluate[R <: BlockingRule](
//        rule: R,
//        input: LiveInterpreter.Input
//      )(using
//        ev: Matchable[R]
//      ): ZIO[EnvironmentFor[R], Throwable, Boolean] = rule match {
//
//        case r @ BlockingRule.PurchaseOccursInCountry(_) =>
//          LiveInterpreter.purchaseOccursInCountry(r)(input)
//
//        case r @ BlockingRule.PurchaseCategoryEquals(_) =>
//          LiveInterpreter.purchaseCategoryEquals(r)(input)
//
//        case r @ BlockingRule.PurchaseAmountExceeds(_) =>
//          LiveInterpreter.purchaseAmountExceeds(r)(input)
//
//        case r @ BlockingRule.CreditCardFlagged() =>
//          LiveInterpreter.creditCardFlagged(r)(input)
//
//        case r @ BlockingRule.FraudProbabilityExceeds(_) =>
//          LiveInterpreter.fraudProbability(r)(input)
//
//        case BlockingRule.And(l, r) =>
//          for {
//            leftResult  <- evaluate(l, input)
//            rightResult <- evaluate(r, input)
//          } yield leftResult && rightResult
//
//        case BlockingRule.Or(l, r) =>
//          for {
//            leftResult  <- evaluate(l, input)
//            rightResult <- evaluate(r, input)
//          } yield leftResult || rightResult
//      }

    trait BlockingRuleEnvironment[R <: BlockingRule] {
      type Environment

      def evaluate(rule: R, input: LiveInterpreter.Input): ZIO[Environment, Throwable, Boolean]
    }

    object BlockingRuleEnvironment {

      type Aux[R <: BlockingRule, Env0] = BlockingRuleEnvironment[R] { type Environment = Env0 }

      given purchaseOccursInCountryEnv: Aux[BlockingRule.PurchaseOccursInCountry, Any] =
        new BlockingRuleEnvironment[BlockingRule.PurchaseOccursInCountry] {
          type Environment = Any
          def evaluate(
            rule: BlockingRule.PurchaseOccursInCountry,
            input: LiveInterpreter.Input
          ): ZIO[Any, Throwable, Boolean] =
            LiveInterpreter.purchaseOccursInCountry(rule)(input)
        }

      given creditCardFlaggedEnv: Aux[BlockingRule.CreditCardFlagged, CreditCardFlaggedService] =
        new BlockingRuleEnvironment[BlockingRule.CreditCardFlagged] {
          type Environment = CreditCardFlaggedService
          def evaluate(
            rule: BlockingRule.CreditCardFlagged,
            input: LiveInterpreter.Input
          ): ZIO[CreditCardFlaggedService, Throwable, Boolean] =
            LiveInterpreter.creditCardFlagged(rule)(input)
        }

      // Add additional instances for other BlockingRules...
      given fraudProbabilityEnv: Aux[BlockingRule.FraudProbabilityExceeds, FraudScoreService] =
        new BlockingRuleEnvironment[BlockingRule.FraudProbabilityExceeds] {
          type Environment = FraudScoreService
          def evaluate(
            rule: BlockingRule.FraudProbabilityExceeds,
            input: LiveInterpreter.Input
          ): ZIO[FraudScoreService, Nothing, Boolean] =
            LiveInterpreter.fraudProbability(rule)(input)
        }

      given purchaseCategoryEqualsEnv: Aux[BlockingRule.PurchaseCategoryEquals, Any] =
        new BlockingRuleEnvironment[BlockingRule.PurchaseCategoryEquals] {
          type Environment = Any
          def evaluate(
            rule: BlockingRule.PurchaseCategoryEquals,
            input: LiveInterpreter.Input
          ): ZIO[Any, Throwable, Boolean] =
            LiveInterpreter.purchaseCategoryEquals(rule)(input)
        }

      given purchaseAmountExceedsEnv: Aux[BlockingRule.PurchaseAmountExceeds, Any] =
        new BlockingRuleEnvironment[BlockingRule.PurchaseAmountExceeds] {
          type Environment = Any
          def evaluate(
            rule: BlockingRule.PurchaseAmountExceeds,
            input: LiveInterpreter.Input
          ): ZIO[Any, Throwable, Boolean] =
            LiveInterpreter.purchaseAmountExceeds(rule)(input)
        }

      given andEnv[L <: BlockingRule, R <: BlockingRule, EnvL, EnvR](using
        L: BlockingRuleEnvironment.Aux[L, EnvL],
        R: BlockingRuleEnvironment.Aux[R, EnvR]
      ): Aux[BlockingRule.And[L, R], EnvL & EnvR] =
        new BlockingRuleEnvironment[BlockingRule.And[L, R]] {
          type Environment = EnvL & EnvR
          def evaluate(
            rule: BlockingRule.And[L, R],
            input: LiveInterpreter.Input
          ): ZIO[EnvL & EnvR, Throwable, Boolean] =
            for {
              leftResult  <- L.evaluate(rule.l, input)
              rightResult <- R.evaluate(rule.r, input)
            } yield leftResult && rightResult
        }

      given orEnv[L <: BlockingRule, R <: BlockingRule, EnvL, EnvR](using
                                                                     L: BlockingRuleEnvironment.Aux[L, EnvL],
                                                                     R: BlockingRuleEnvironment.Aux[R, EnvR]
                                                                    ): Aux[BlockingRule.Or[L, R], EnvL & EnvR] =
        new BlockingRuleEnvironment[BlockingRule.Or[L, R]] {
          type Environment = EnvL & EnvR

          def evaluate(
                        rule: BlockingRule.Or[L, R],
                        input: LiveInterpreter.Input
                      ): ZIO[EnvL & EnvR, Throwable, Boolean] =
            for {
              leftResult <- L.evaluate(rule.l, input)
              rightResult <- R.evaluate(rule.r, input)
            } yield leftResult || rightResult
        }

    }

    def evaluate[R <: BlockingRule](rule: R, input: LiveInterpreter.Input)(
      using env: BlockingRuleEnvironment[R]
    ): ZIO[env.Environment, Throwable, Boolean] =
      env.evaluate(rule, input)

  }

}
