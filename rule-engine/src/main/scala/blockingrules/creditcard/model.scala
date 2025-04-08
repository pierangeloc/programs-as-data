package blockingrules.creditcard

import neotype.Newtype

import java.util.UUID

object model {

  object basetypes {

    object Probability extends Newtype[Double] {
      override inline def validate(input: Double) = if (input >= 0.0 && input <= 1.0) true else false
    }
    type Probability = Probability.Type

    object Id extends Newtype[String]
    type Id = Id.Type

    object CardNumber extends Newtype[String]
    type CardNumber = CardNumber.Type

    object CardHolderName extends Newtype[String]
    type CardHolderName = CardHolderName.Type

    object CardType extends Newtype[String]
    type CardType = CardType.Type

    object ExpiryDate extends Newtype[String]
    type ExpiryDate = ExpiryDate.Type

    object Cvv extends Newtype[Int]
    type Cvv = Cvv.Type

    object Country extends Newtype[String] {
      val China       = Country("CHN")
      val US          = Country("USA")
      val Netherlands = Country("NLD")
      val Italy       = Country("ITA")
      val UK          = Country("GBR")
    }
    type Country = Country.Type

    object Currency extends Newtype[String]
    type Currency = Currency.Type

    object Amount extends Newtype[Double]
    type Amount = Amount.Type

    enum PurchaseCategory:
      case Electronics, Food, Travel, Entertainment, Gambling, Adult, Other

    object Latitude extends Newtype[Double]
    type Latitude = Latitude.Type
    
    object Longitude extends Newtype[Double]
    type Longitude = Longitude.Type
    
    object ShopName extends Newtype[String]
    type ShopName = ShopName.Type
    
    object ShopId extends Newtype[UUID]
    type ShopId = ShopId.Type
    
    object UserId extends Newtype[UUID]
    type UserId = UserId.Type
    
    object Age extends Newtype[Int] {
      override def validate(input: Int): Boolean | String = if (input >= 0 && input <= 120) true else "Age must be between 0 and 120"
    }
    type Age = Age.Type
    
    
    
    
  }

  case class CreditCard(
    id: basetypes.Id,
    cardNumber: basetypes.CardNumber,
    cardHolderName: basetypes.CardHolderName,
    cardType: basetypes.CardType,
    expiryDate: basetypes.ExpiryDate,
    cvv: basetypes.Cvv,
    country: basetypes.Country,
    forbiddenCountries: Set[basetypes.Country]
  )

  case class Purchase(
    id: basetypes.Id,
    creditCardId: basetypes.Id,
    amount: basetypes.Amount,
    inCountry: basetypes.Country,
    category: basetypes.PurchaseCategory
  )

}
