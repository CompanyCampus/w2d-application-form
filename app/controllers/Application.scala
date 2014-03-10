package controllers

import play.api._
import play.api.mvc._
import play.api.mvc.Security._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n._
import play.api.Play.configuration
import play.api.Play.current
import org.joda.time.DateTime
import scala.util.{Try, Success, Failure}
import scala.util.control.Exception.allCatch
import java.util.UUID

import models._

object Application extends Controller {

def getLang()(implicit request: Request[AnyContent], lang: Lang) =
  request.getQueryString("lang") map(Lang(_)) getOrElse lang

val formBMC = Form(mapping(
  "partners" -> text.verifying(nonEmpty),
  "activities" -> text.verifying(nonEmpty),
  "resources" -> text.verifying(nonEmpty),
  "propositions" -> text.verifying(nonEmpty),
  "customerRelationships" -> text.verifying(nonEmpty),
  "channels" -> text.verifying(nonEmpty),
  "customerSegments" -> text.verifying(nonEmpty),
  "costStructure" -> text.verifying(nonEmpty),
  "revenueStreams" -> text.verifying(nonEmpty)
)(RecordBMC.apply)(RecordBMC.unapply))
val formInfo = Form(mapping(
  "pitch" -> text.verifying(minLength(1), maxLength(400)),
  "name" -> text.verifying(nonEmpty),
  "company" -> text.verifying(nonEmpty),
  "companyCreation" -> optional(jodaDate("yyyy-MM-dd")),
  "companyWebsite" -> text.verifying(nonEmpty),
  "email" -> email.verifying(nonEmpty),
  "phone" -> text.verifying(nonEmpty),
  "vine" -> optional(text),
  "twitter" -> optional(text),
  "angelco" -> optional(text.verifying(
    Messages("angelco.invalid"),
    a => a.startsWith("https://angel.co/")
  )),
  "presentationUrl" -> optional(text),
  "amount" -> optional(number)
)(RecordInfo.apply)(RecordInfo.unapply))

  def index = Action { implicit request =>
    implicit val lang = getLang
    Ok(views.html.index())
  }

  def authenticate = Action { implicit request =>
    request.body.asFormUrlEncoded flatMap { rawdata =>
      val data = rawdata map { x => (x._1, x._2.headOption) } collect {
        case (k: String, v: Some[String]) => (k, v.get)
      }
      for {
        email <- data.get("email")
        password <- data.get("password")
      } yield {
        User.get(email) map { u =>
          if(u.hasPassword(password)) {
            Redirect("/submission").withSession("username" -> u.email)
          } else {
            Redirect("/").flashing("error" -> Messages("incorrectpassword"))
          }
        } getOrElse {
          User.create(email, password).save match {
            case Success(u) => {
              u.sendValidationEmail
              Redirect("/submission").withSession(
                "username" -> u.email
              ).flashing(
                "message" -> Messages("accountcreated")
              )
            }
            case Failure(e) => {
              println("--- Account creation error ---")
              println(email)
              println(e.getMessage)
              println("------------------------------")
              InternalServerError("Something wrong happened while trying to create your account. Try again?")
            }
          }
        }
      }
    } getOrElse BadRequest("Incomplete form")
  }

  def validateUser(id: UUID, token: String) = Action { implicit request =>
    User.get(id) map { u =>
      if(token == u.token) {
        u.validate match {
          case Success(u) => {
            u.resetToken
            Redirect("/submission").withSession(
              "username" -> u.email
            ).flashing(
              "message" -> Messages("accountvalidated")
            )
          }
          case Failure(e) => {
            println("--- Account validation error ---")
            println(email)
            println(e.getMessage)
            println("------------------------------")
            InternalServerError("Something wrong happened while trying to validate your account. Try again?")
          }
        }
      } else BadRequest("Wrong token")
    } getOrElse NotFound
  }

  def submission = Authenticated { email =>
    Action { implicit request =>
      implicit val lang = getLang
      User.get(email) map { u =>
        if(!u.hasVcRights) {
          Ok(views.html.submission(
            formBMC.fill(u.record.bmc),
            formInfo.fill(u.record.info),
            u,
            u.record.submitted,
            (configuration getBoolean "closed" getOrElse false)
          ))
        } else {
          Redirect("/records")
        }
      } getOrElse Unauthorized
    }
  }
  
  def updateRecord() = this._saveRecord(true)
  def saveRecord() = this._saveRecord(false)
  def _saveRecord(saveonly: Boolean) = Authenticated { email =>
    Action { implicit request =>
      implicit val lang = getLang
      User.get(email) map { u =>
        val bmc = formBMC.bindFromRequest
        val info =  formInfo.bindFromRequest
        if(!saveonly && (bmc.hasErrors || info.hasErrors)) {
          BadRequest(views.html.submission(
            bmc, info, u, false, (configuration getBoolean "closed" getOrElse false)
          ))
        } else {
          if(u.record.submitted) {
            if(saveonly) {
              Forbidden
            } else {
              Redirect("/submission").flashing("error" -> Messages("alreadysubmitted"))
            }
          } else {
            val bmcValue = bmc.value getOrElse RecordBMC(
              partners = bmc.data.get("partners").getOrElse(""),
              activities = bmc.data.get("activities").getOrElse(""),
              resources = bmc.data.get("resources").getOrElse(""),
              propositions = bmc.data.get("propositions").getOrElse(""),
              customerRelationships = bmc.data.get("customerRelationships").getOrElse(""),
              channels = bmc.data.get("channels").getOrElse(""),
              customerSegments = bmc.data.get("customerSegments").getOrElse(""),
              costStructure = bmc.data.get("costStructure").getOrElse(""),
              revenueStreams = bmc.data.get("revenueStreams").getOrElse("")
            )
            val infoValue = info.value getOrElse RecordInfo(
              pitch = info.data.get("pitch").getOrElse(""),
              name = info.data.get("name").getOrElse(""),
              company = info.data.get("company").getOrElse(""),
              companyCreation = info.data.get("companyCreation").flatMap(d => Try(new DateTime(d)).toOption),
              companyWebsite = info.data.get("companyWebsite").getOrElse(""),
              email = info.data.get("email").getOrElse(""),
              phone = info.data.get("phone").getOrElse(""),
              vine = info.data.get("vine"),
              twitter = info.data.get("twitter"),
              angelco = info.data.get("angelco"),
              presentationUrl = info.data.get("presentationUrl"),
              amount = info.data.get("amount").flatMap(x => Try(x.toInt).toOption)
            )
            u.record.copy(
              bmc = bmcValue, info = infoValue, date = new DateTime, submitted = !saveonly && u.kind == UserKind.Startup
            ).update match {
              case Success(r) => if(saveonly) Ok else Ok(views.html.confirmation())
              case Failure(e) => InternalServerError
            }
          }
        }
      } getOrElse Unauthorized
    }
  }

  def records = Authenticated { email =>
    Action { implicit request =>
      User.get(email) map { u =>
        if(u.hasVcRights) {
          Ok(views.html.records(
            u.kind match {
              case UserKind.Admin => Record.getAll
              case UserKind.VC => Record.getAll filter (_.selected)
              case _ => List()
            }, u
          ))
        } else Forbidden
      } getOrElse Unauthorized
    }
  }

  def record(id: UUID) = Authenticated { email =>
    Action { implicit request =>
      User.get(email) map { u =>
        if(u.hasVcRights) {
          Record.get(id) map { r =>
            Ok(views.html.record(
              formBMC.fill(r.bmc),
              formInfo.fill(r.info)
            ))
          } getOrElse BadRequest
        } else Forbidden
      } getOrElse Unauthorized
    }
  }

  def users() = Authenticated { email =>
    Action { implicit request =>
      User.get(email) map { u =>
        if(u.kind == UserKind.Admin) {
          Ok(views.html.users(User.getAll, u, addUserForm))
        } else {
          Forbidden
        }
      } getOrElse Unauthorized
    }
  }

  def addUserForm = Form(
    tuple(
      "email" -> text.verifying("E-mail already used", email => User.get(email).isEmpty),
      "kind" -> text.verifying("Incorrect kind", kind => UserKind.fromString(kind).isDefined)
    )
  )

  def addUser() = Authenticated { email =>
    Action { implicit request =>
      User.get(email) map { u =>
        if(u.kind == UserKind.Admin) {
          addUserForm.bindFromRequest.fold(
            formWithErrors => BadRequest(views.html.users(User.getAll, u, formWithErrors)),
            user => {
              User.create(user._1, UserKind.fromString(user._2).getOrElse(UserKind.Unverified)).save match {
                case Success(u) => u.sendCreatedEmail; Redirect(routes.Application.users)
                case Failure(e) => InternalServerError(e.getMessage)
              }
            }
          )
        } else {
          Forbidden
        }
      } getOrElse Unauthorized
    }
  }

  def deleteUser(id: UUID) = Authenticated { email =>
    Action { implicit request =>
      User.get(email) map { u =>
        if(u.kind == UserKind.Admin) {
          User.delete(id)
          NoContent
        } else {
          Forbidden
        }
      } getOrElse Unauthorized
    }
  }

  def forgottenpasswordPage() = Action { implicit request =>
    
  }

  def choosePasswordForm = Form(
    tuple(
      "token" -> text,
      "password" -> text(minLength = 8).verifying(nonEmpty)
    )
  )

  def choosePasswordPage(id: UUID, token: String) = Action { implicit request =>
    User.get(id) map { u =>
      if(u.token == token && u.password.isEmpty) {
        Ok(views.html.choosepassword(choosePasswordForm.fill((token, "")), u))
      } else {
        Forbidden
      }
    } getOrElse NotFound
  }

  def choosePassword(id: UUID) = Action { implicit request =>
    User.get(id) map { u =>
      choosePasswordForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.choosepassword(formWithErrors, u)),
        data => {
          if(u.token == data._1) {
            if(u.password.isEmpty) {
              u.setPassword(data._2)
              Redirect(routes.Application.records).withSession("username" -> u.email)
            } else {
              Forbidden
            }
          } else {
            Unauthorized
          }
        }
      )
    } getOrElse NotFound
  }

}
