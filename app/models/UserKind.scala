package models
import scala.language.implicitConversions

object UserKind extends Enumeration {
  type UserKind = Value
  val Admin = Value("Admin")
  val VC = Value("VC")

  class UserKindValue(userKind: Value)

  implicit def valueToUserKindValue(userKind: Value) = new UserKindValue(userKind)
}
