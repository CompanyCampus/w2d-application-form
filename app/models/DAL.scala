package models
import slick.driver.JdbcProfile

class DAL(override val profile: JdbcProfile)
  extends RecordComponent
  with UserComponent
  with Profile {

  import profile.simple._
}
