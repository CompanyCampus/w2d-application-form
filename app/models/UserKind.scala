package models

sealed trait UserKind { def hasVcRights = false }
object UserKind {
  case object Admin extends UserKind { override def hasVcRights = true }
  case object VC extends UserKind { override def hasVcRights = true }
  case object Startup extends UserKind
  case object Unverified extends UserKind

  def fromString(x: String): Option[UserKind] = {
    x match {
      case "Admin" => Some(Admin)
      case "VC" => Some(VC)
      case "Startup" => Some(Startup)
      case "Unverified" => Some(Unverified)
      case _ => None
    }
  }
}
