package models
import java.util.UUID
import scala.util.Try
import org.joda.time.DateTime
import java.sql.Timestamp
import scala.slick.session._

case class Record (
  id: UUID,
  date: DateTime
) {
  def save() = {
    Try {
      AppDB.database.withSession { implicit session: Session =>
        AppDB.dal.Records.add(this)
      }
      this
    }
  }
}

object Record {
  def applyFromDAL(
    id: UUID, date: Timestamp
  ): Record = {
    Record(
      id = id,
      date = new DateTime(date)
    )
  }
  def unapplyToDAL(record: Record): Option[(
    UUID, Timestamp
  )] = {
    Some(record.id, new Timestamp(record.date.getMillis))
  }

  def get(id: UUID) = {
    AppDB.dal.Records.get(id)
  }

  def getAll() = {
    AppDB.dal.Records.getAll
  }
}

trait RecordComponent {
  this: Profile =>

  import profile.simple._

  object Records extends Table[Record]("records") {
    def id = column[UUID]("record_id", O.PrimaryKey)
    def date = column[Timestamp]("record_date")
    def * = (
      id ~ date
    ) <> (Record.applyFromDAL _, Record.unapplyToDAL _)

    def add(record: Record)(implicit session: Session) = {
      this.insert(record)
    }

    def get(id: UUID): Option[Record] = {
      AppDB.database.withSession { implicit session: Session =>
        Query(Records).filter(_.id === id).firstOption
      }
    }

    def getAll() = {
      AppDB.database.withSession { implicit session: Session =>
        Query(Records).list
      }
    }
  }
}
