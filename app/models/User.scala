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
  kind: UserKind
) {
  lazy val record = Record.getOf(this) getOrElse Record.createFor(this)

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

  def hasPassword(password: String) = {
    this.password map { pw =>
      BCrypt.checkpw(password, pw)
    } getOrElse false
  }

  def hasVcRights = this.kind.hasVcRights

  def validate() = {
    val updated = this.copy(kind = UserKind.Startup)
    Try {
      AppDB.dal.Users.updateKind(updated)
      updated
    }
  }

  def resetToken() = {
    val updated = this.copy(token = scala.util.Random.alphanumeric.take(40).mkString)
    Try {
      AppDB.dal.Users.updateToken(updated)
      updated
    }
  }

  def sendValidationEmail() {
    import _root_.util.Mailer

    val link =
      (configuration getString "baseurl" getOrElse "https://w2d-startupcontest.cleverapps.io/") +
      "users/" + this.id.toString + "/validate?token=" + this.token

    Mailer.send(
      subject = "Validate your Startup Contest account",
      recipient = this.email,
      from = "W2D2014 Startup Contest <noreply@companycamp.us>",
      message = s"""
Hello,

You need to validate your account to be able to submit your application.

Click here: $link
"""
    )
  }

  def sendResetPasswordEmail() {
    import _root_.util.Mailer

    val link =
      (configuration getString "baseurl" getOrElse "https://w2d-startupcontest.cleverapps.io/") +
      "users/" + this.id.toString + "/choosepassword?token=" + this.token

    Mailer.send(
      subject = "Reset your Startup Contest account password",
      recipient = this.email,
      from = "W2D2014 Startup Contest <noreply@companycamp.us>",
      message = s"""
Hello,

You have requested to reset the password of your Startup Contest account.

Click here to do so: $link

If you didn't ask for this, you can simply ignore this e-mail.
"""
    )
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
  def create(email: String, kind: UserKind): User = {
    User(
      id = UUID.randomUUID(),
      email = email,
      token = scala.util.Random.alphanumeric.take(40).mkString,
      kind = kind
    )
  }

  def create(email: String, password: String): User = {
    User(
      id = UUID.randomUUID(),
      email = email,
      password = Some(BCrypt.hashpw(password, BCrypt.gensalt)),
      token = scala.util.Random.alphanumeric.take(40).mkString,
      kind = UserKind.Unverified
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
    implicit val userKindTM = MappedColumnType.base[UserKind, String](
      uk => uk.toString,
      str => UserKind.fromString(str).getOrElse(UserKind.Unverified)
    )

    def id = column[UUID]("user_id", O.PrimaryKey)
    def email = column[String]("user_email")
    def password = column[Option[String]]("user_password")
    def token = column[String]("user_token")
    def kind = column[UserKind]("user_kind")
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

    def updateKind(user: User) = {
      AppDB.database.withSession { implicit session: Session =>
        this.filter(_.id === user.id) map (_.kind) update(user.kind)
      }
    }

    def updateToken(user: User) = {
      AppDB.database.withSession { implicit session: Session =>
        this.filter(_.id === user.id) map (_.token) update(user.token)
      }
    }

    def delete(id: UUID) {
      AppDB.database.withSession { implicit session: Session =>
        this.filter(_.id === id).delete
      }
    }
  }
}
