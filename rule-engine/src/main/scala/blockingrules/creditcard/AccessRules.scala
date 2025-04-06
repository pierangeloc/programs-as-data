package blockingrules.creditcard

import blockingrules.creditcard.model.basetypes.*
import zio.URIO

/**
 * We want to use the blocking rule information to display shops in a map. We don't want to display shops that we know will be 100% blocked by our
 * blocking logic
 */
object AccessRules {
  case class UserContext(userId: UserId)

  case class Shop(id: ShopId, name: ShopName, latitude: Latitude, longitude: Longitude, categories: List[PurchaseCategory])


  trait Service {
    def getShops(): URIO[UserContext, List[Shop]]
  }
}
