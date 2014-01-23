package models
import slick.driver.ExtendedProfile

class DAL(override val profile: ExtendedProfile)
  extends RecordComponent
  with Profile {

  import profile.simple._
}
