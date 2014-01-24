package models
import java.util.UUID
import scala.util.Try
import org.mindrot.jbcrypt._
import play.api.Play.configuration
import play.api.Play.current

case class User (
  id: UUID,
  email: String,
  password: Option[String] = None,
  token: String,
  kind: UserKind.Value
) {

  def save = {
    Try {
      AppDB.dal.Users.add(this)
      this
    }
  }

  def setPassword(password: String) {
    Try {
      val updated = this.copy(password = Some(BCrypt.hashpw(password, BCrypt.gensalt)))
      AppDB.dal.Users.updatePassword(updated)
      updated
    }
  }

  def sendCreatedEmail() {
    import _root_.util.Mailer
    
    val url = (
      configuration getString "baseurl" getOrElse "http://localhost:9000/"
    ) + "users/" + id.toString + "/choosepassword?token=" + this.token

    Mailer.send(
      subject = "Your " + this.kind.toString + " account",
      recipient = this.email,
      from = "W2D2014 Startup Contest <noreply@companycamp.us>",
      message = """
Hello,

Your """ + this.kind.toString + """ account has been created on the Startup Contest site.
You need to choose a password here: """ + url
    )
  }
}

object User {
  def create(email: String, kind: UserKind.Value): User = {
    User(
      id = UUID.randomUUID(),
      email = email,
      token = scala.util.Random.alphanumeric.take(40).mkString,
      kind = kind
    )
  }

  def authenticate(email: String, password: String): Boolean = {
    (for {
      u <- this.get(email)
      pw <- u.password
    } yield {
      BCrypt.checkpw(password, pw)
    }) getOrElse false
  }

  def get(id: UUID) = AppDB.dal.Users.get(id)
  def get(email: String) = AppDB.dal.Users.get(email)
  def getAll() = AppDB.dal.Users.getAll
  
  def delete(id: UUID) = AppDB.dal.Users.delete(id)
}

trait UserComponent {
  this: Profile =>

  import profile.simple._

  class Users(tag: Tag) extends Table[User](tag, "users") {
    implicit val userKindTM = MappedColumnType.base[UserKind.Value, String](
      uk => uk.toString,
      str => UserKind.withName(str)
    )

    def id = column[UUID]("user_id", O.PrimaryKey)
    def email = column[String]("user_email")
    def password = column[Option[String]]("user_password")
    def token = column[String]("user_token")
    def kind = column[UserKind.Value]("user_kind")
    def * = (
      id, email, password, token, kind
    ) <> ((User.apply _).tupled, User.unapply)
  }

  object Users extends TableQuery(new Users(_)) {
    def add(user: User) = {
      AppDB.database.withSession { implicit session: Session =>
        this.insert(user)
      }
    }

    def get(id: UUID): Option[User] = {
      AppDB.database.withSession { implicit session: Session =>
        this.filter(_.id === id).firstOption
      }
    }
    
    def get(email: String): Option[User] = {
      AppDB.database.withSession { implicit session: Session =>
        this.filter(_.email === email).firstOption
      }
    }

    def getAll() = {
      AppDB.database.withSession { implicit session: Session =>
        this.list
      }
    }

    def updatePassword(user: User) = {
      AppDB.database.withSession { implicit session: Session =>
        this.filter(_.id === user.id) map(_.password) update(user.password)
      }
    }

    def delete(id: UUID) {
      AppDB.database.withSession { implicit session: Session =>
        this.filter(_.id === id).delete
      }
    }
  }
}
