package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n._
import org.joda.time.DateTime
import scala.util.{Success, Failure}

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
  "pitch" -> text.verifying(nonEmpty),
  "name" -> text.verifying(nonEmpty),
  "company" -> text.verifying(nonEmpty),
  "companyCreation" -> jodaDate("yyyy-MM-dd"),
  "companyWebsite" -> text.verifying(nonEmpty),
  "email" -> email.verifying(nonEmpty),
  "phone" -> text.verifying(nonEmpty),
  "twitter" -> optional(text),
  "angelco" -> optional(text),
  "presentationUrl" -> optional(text),
  "amount" -> optional(number)
)(RecordInfo.apply)(RecordInfo.unapply))
 
  def index = Action { implicit request =>
    Ok(views.html.index(formBMC, formInfo))
  }

  def addRecord = Action { implicit request =>
    val bmc = formBMC.bindFromRequest
    val info =  formInfo.bindFromRequest
    if(bmc.hasErrors || info.hasErrors) {
      BadRequest(views.html.index(bmc, info))
    } else {
      Record.create(bmc.get, info.get).save match { // Fuck this shit
        case Success(r) => Ok(Messages("record.saved"))
        case Failure(e) => InternalServerError(Messages("record.error"))
      }
    }
  }
  
}
