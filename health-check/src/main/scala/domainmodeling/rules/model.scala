package domainmodeling.rules

import java.time.ZonedDateTime

opaque type LocationId = String
opaque type UserId     = String
opaque type AccessTime = ZonedDateTime
opaque type Country    = String
opaque type Address    = String
opaque type Latitude   = Double
opaque type Longitude  = Double

case class GeoCoordinates(
  latitude: Latitude,
  longitude: Longitude
)
case class UserProfile(
  userId: UserId,
  enabledCountries: Set[Country]
)

case class Location(
  address: Address
)

case class Features(
  locationId: LocationId,
  userId: UserId,
  accessTime: AccessTime
)

def hasAccess(features: Features): Boolean = true
