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
import scala.util.{Success, Failure}
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
  "companyCreation" -> jodaDate("yyyy-MM-dd"),
  "companyWebsite" -> text.verifying(nonEmpty),
  "email" -> email.verifying(nonEmpty),
  "phone" -> text.verifying(nonEmpty),
  "vine" -> optional(text.verifying(
    Messages("vine.invalid"),
    v => v.startsWith("https://vine.co/v/")
  )),
  "twitter" -> optional(text),
  "angelco" -> optional(text.verifying(
    Messages("angelco.invalid"),
    a => a.startsWith("https://angel.co/")
  )),
  "presentationUrl" -> optional(text),
  "amount" -> optional(number)
)(RecordInfo.apply)(RecordInfo.unapply))

  def index = Action { implicit request =>
    if(configuration getBoolean "closed" getOrElse false) {
      Ok(views.html.closed()(
        request, getLang
      ))
    } else {
      Ok(views.html.index(
        formBMC, formInfo
      )(
        request, getLang
      ))
    }
  }

  def addRecord = Action { implicit request =>
    val bmc = formBMC.bindFromRequest
    val info =  formInfo.bindFromRequest
    if(bmc.hasErrors || info.hasErrors) {
      BadRequest(views.html.index(bmc, info)(
        request, getLang
      ))
    } else {
      Record.create(bmc.get, info.get).save match { // Fuck this shit
        case Success(r) => r.sendNotificationEmail; Ok(
          views.html.confirmation()
          (request, getLang)
        )
        case Failure(e) => {
          println("--- Record saving error ---")
          println("Company: " + info.get.company)
          println("Name: " + info.get.name)
          println("E-mail: " + info.get.email)
          println("Error: " + e.getMessage)
          println("--------------------")
          InternalServerError(Messages("record.error"))
        }
      }
    }
  }

  def loginForm = Form(
    tuple(
      "login" -> text,
      "password" -> text
    ) verifying("Wrong credentials", result => result match {
      case (l, p) => {
        val login = configuration getString "admin.login" getOrElse ""
        val password = configuration getString "admin.password" getOrElse ""

        val vclogin = configuration getString "vc.login" getOrElse ""
        val vcpassword = configuration getString "vc.password" getOrElse ""

        (l == login && p == password) || (l == vclogin && p == vcpassword)
      }
      case _ => false
    })
  )

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      user => Redirect(routes.Application.records).withSession("username" -> user._1)
    )
  }

  def records = Authenticated { username =>
    val login = configuration getString "admin.login" getOrElse ""
    Action { implicit request =>
      Ok(views.html.records(
        if(username == login) {
          Record.getAll
        } else {
          Record.getAll filter (_.selected)
        }
      ))
    }
  }

  def record(id: UUID) = Authenticated { username =>
    Action { implicit request =>
      Record.get(id) map { r =>
        Ok(views.html.record(
          formBMC.fill(r.bmc),
          formInfo.fill(r.info)
        ))
      } getOrElse BadRequest
    }
  }

}
