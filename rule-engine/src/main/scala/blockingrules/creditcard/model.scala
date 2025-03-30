package blockingrules.creditcard

import neotype.Newtype

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
      // suggest purchase categories that have different risk levels. Not higrisk/lowrisk, but e.g. "electronics", "food", "travel", "entertainment", etc.
      case Electronics, Food, Travel, Entertainment, Weapons, Crypto, Other

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
