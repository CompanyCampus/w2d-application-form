package util
import play.api.Play.current
import com.typesafe.plugin.{use, MailerPlugin}
import scala.util.{Try, Success, Failure}

object Mailer {
  def send(
    subject: String,
    recipients: List[String],
    from: String,
    message: String
  ) {
    val mail = use[MailerPlugin].email

    Try {
      mail.setSubject(subject)
      mail.setRecipient(recipients:_*)
      mail.setFrom(from)
      mail.send(message)
    } match {
      case Success(_) => Unit
      case Failure(e) => failureSending(subject, recipients, List(), from, message)
    }
  }

  def send(
    subject: String,
    recipients: List[String],
    hiddenRecipients: List[String],
    from: String,
    message: String
  ) {
    val mail = use[MailerPlugin].email
    Try {
      mail.setSubject(subject)
      mail.setRecipient(recipients:_*)
      mail.setBcc(hiddenRecipients:_*)
      mail.setFrom(from)
      mail.send(message)
    } match {
      case Success(_) => Unit
      case Failure(e) => failureSending(subject, recipients, List(), from, message)
    }
  }

  def send(subject: String, recipient: String, from: String, message: String) {
    this.send(subject, List(recipient), from, message)
  }

  def send(subject: String, recipient: Option[String], from: String, message: String) {
    recipient map { r =>
      this.send(subject, r, from, message)
    } getOrElse Unit
  }

  def failureSending(
    subject: String,
    recipients: List[String],
    hiddenRecipients: List[String],
    from: String,
    message: String
  ) {
    println("----------")
    println("Tried to send this:")
    println("Subject: " + subject)
    println("Recipients: " + recipients.mkString(", "))
    println("Hidden recipients: " + hiddenRecipients.mkString(", "))
    println("From: " + from)
    println("Message: " + message)
    println("----------")
  }
}
