import models.{DBeable, AppDB}
import play.api.db.DB
import play.api.GlobalSettings
import play.api.Application
import slick.jdbc.JdbcBackend.Session

object Global extends GlobalSettings with DBeable {

  override def onStart(app: Application) {
    implicit val application = app
    lazy val database = getDb
    lazy val dal = getDal
  }

}
