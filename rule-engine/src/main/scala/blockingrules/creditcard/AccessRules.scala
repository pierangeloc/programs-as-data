package blockingrules.creditcard

import blockingrules.creditcard.AccessRules.UserContext
import blockingrules.creditcard.model.basetypes.*
import zio.URIO

import java.net.InetAddress
import java.time.ZonedDateTime

/**
 * We want to use the blocking rule information to display shops in a map. We don't want to display shops that we know will be 100% blocked by our
 * blocking logic
 */
object AccessRules {

  // can exclude based on operation time, or ipAddress being of a proxy, or because of device signature (e.g. browser being used, OS)
  case class UserContext(userId: UserId, operationTime: ZonedDateTime, deviceSignature: UserContext.DeviceSignature, ipAddress: InetAddress, position: UserContext.Position, age: Age)
  
  object UserContext {
    case class Position(latitude: Latitude, longitude: Longitude)
    case class DeviceSignature(os: String, browser: String)
  }

  case class Shop(id: ShopId, name: ShopName, country: Country, categories: List[PurchaseCategory])


  trait Service {
    def getShops(): URIO[UserContext, List[Shop]]
  }
}
