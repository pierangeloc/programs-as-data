package blockingrules.creditcard.declarative

import blockingrules.creditcard.declarative.BlockingLogicDeclarative.BlockingRule
import blockingrules.creditcard.model.basetypes.*
import zio.{UIO, URIO}

import java.net.InetAddress
import java.time.ZonedDateTime

/**
 * We want to use the blocking rule information to display shops in a map. We
 * don't want to display shops that we know will be 100% blocked by our blocking
 * logic
 */
object AccessRules {

  // can exclude based on operation time, or ipAddress being of a proxy, or because of device signature (e.g. browser being used, OS)
  case class UserContext(
    userId: UserId,
    operationTime: ZonedDateTime,
    deviceSignature: UserContext.DeviceSignature,
    ipAddress: InetAddress,
    position: UserContext.Position,
    age: Age
  )

  object UserContext {
    case class Position(latitude: Latitude, longitude: Longitude)
    case class DeviceSignature(os: String, browser: String)
  }

  // TODO: move shop in the model with all the rest
  case class Shop(
    id: ShopId,
    name: ShopName,
    country: Country,
    categories: List[ShopCategory]
  )

  trait ShopRepository {
    def getShops(filter: ShopRepository.Filter): UIO[List[Shop]]
    def isBlacklisted(shopId: ShopId): UIO[Boolean]
  }

  object ShopRepository {
    case class Filter(
      excludeCountriesAndCategories: Option[List[Filter.CountryCategory]],
      excludeBlacklisted: Option[Boolean]
    ) { self =>

      import cats.implicits.*
      def ||(other: Filter): Filter =
        Filter(
          excludeCountriesAndCategories combine other.excludeCountriesAndCategories,
          (excludeBlacklisted, other.excludeBlacklisted) match {
            case (b, None)            => b
            case (None, b)            => b
            case (Some(b1), Some(b2)) => Some(b1 || b2)
          }
        )

      private def intersect[A](l: Option[List[A]], r: Option[List[A]]): Option[List[A]] =
        (l, r) match {
          case (Some(l), Some(r)) => Some(l.intersect(r))
          case (None, right)      => right
          case (left, None)       => left
        }

      // TODO:think further about this one
      def &&(other: Filter): Filter =
        Filter(
          intersect(excludeCountriesAndCategories, other.excludeCountriesAndCategories),
          (excludeBlacklisted, other.excludeBlacklisted) match {
            case (Some(true), Some(true)) => Some(true)
            case (None, Some(b))          => Some(b)
            case (Some(b), None)          => Some(b)
            case (None, None)             => None
            case (Some(_), Some(false))   => ???
            case (Some(false), Some(_))   => ???
          }
        )
    }

    object Filter {
      val allPass = Filter(None, None)

      case class CountryCategory(country: Option[Country], category: Option[ShopCategory])

//      def and(cc1: CountryCategory, cc2: CountryCategory)
    }
  }

//  object FilterInterpreter {
//    def apply(blockingRule: BlockingRule): ShopRepository.Filter = blockingRule match
//      case BlockingRule.And(l, r) => apply(l) && apply(r)
//      case BlockingRule.Or(l, r)  => apply(l) || apply(r)
//      case BlockingRule.PurchaseOccursInCountry(country) =>
//        ShopRepository.Filter.allPass.copy(excludeCountries = Some(List(country)))
//      case BlockingRule.PurchaseCategoryEquals(purchaseCategory) =>
//        ShopRepository.Filter.allPass.copy(excludeCategories = Some(List(purchaseCategory)))
//      case BlockingRule.PurchaseAmountExceeds(_)      => ShopRepository.Filter.allPass
//      case BlockingRule.FraudProbabilityExceeds(_) => ShopRepository.Filter.allPass
//      case BlockingRule.CreditCardFlagged()                => ShopRepository.Filter.allPass
//      case blockingrules.creditcard.declarative.BlockingLogicDeclarative.BlockingRule.ShopIsBlacklisted() =>
//        ShopRepository.Filter.allPass.copy(excludeBlacklisted = Some(true))
//  }

}
