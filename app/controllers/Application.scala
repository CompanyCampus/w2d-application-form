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

def sendNotificationEmail(r: Record) = {
  import _root_.util.Mailer

  Mailer.send(
    subject = "Nouvelle candidature Startup Contest W2D 2013",
    recipients = List("f.herveou@tuttivox.com", "adrien.crette@clever-cloud.com"),
    from = "W2D2013 Startup Contest <noreply@companycamp.us>",
    message = """
Nouvelle candidature pour le Startup Contest
Nom : """ + r.info.name + """
Startup : """ + r.info.company + """

Login administration : https://w2d-form.cleverapps.io/login
Candidature : https://w2d-form.cleverapps.io/records/""" + r.id.toString()
  )
}

  def index = Action { implicit request =>
    Ok(views.html.index(
      formBMC, formInfo
    )(
      request, request.getQueryString("lang") map { Lang(_) } getOrElse lang
    ))
  }

  def addRecord = Action { implicit request =>
    val bmc = formBMC.bindFromRequest
    val info =  formInfo.bindFromRequest
    if(bmc.hasErrors || info.hasErrors) {
      BadRequest(views.html.index(bmc, info)(
        request, request.getQueryString("lang") map { Lang(_) } getOrElse lang
      ))
    } else {
      Record.create(bmc.get, info.get).save match { // Fuck this shit
        case Success(r) => sendNotificationEmail(r); Ok(
          views.html.confirmation()
          (request, request.getQueryString("lang") map { Lang(_) } getOrElse lang)
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

        l == login && p == password
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
    Action { implicit request =>
      Ok(views.html.records(Record.getAll))
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

  def closed = Action { implicit request =>
    Ok(views.html.closed()(
      request, request.getQueryString("lang") map { Lang(_) } getOrElse lang
    ))
  }

}
