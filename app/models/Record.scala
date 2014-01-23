package models
import java.util.UUID
import scala.util.Try
import org.joda.time.DateTime
import java.sql.Timestamp
import play.api.libs.json._

case class Record (
  id: UUID,
  date: DateTime,
  bmc: RecordBMC,
  info: RecordInfo,
  selected: Boolean
) {

  def save() = {
    Try {
      AppDB.dal.Records.add(this)
      this
    }
  }

  def sendNotificationEmail {
    import _root_.util.Mailer

    Mailer.send(
      subject = "Nouvelle candidature Startup Contest W2D 2013",
      recipients = List("f.herveou@tuttivox.com", "adrien.crette@clever-cloud.com"),
      from = "W2D2013 Startup Contest <noreply@companycamp.us>",
      message = """
Nouvelle candidature pour le Startup Contest
Nom : """ + this.info.name + """
Startup : """ + this.info.company + """

Login administration : https://w2d-form.cleverapps.io/login
Candidature : https://w2d-form.cleverapps.io/records/""" + this.id.toString()
    )
  }
}

case class RecordBMC (
  partners: String,
  activities: String,
  resources: String,
  propositions: String,
  customerRelationships: String,
  channels: String,
  customerSegments: String,
  costStructure: String,
  revenueStreams: String
)
case class RecordInfo(
  pitch: String,
  name: String,
  company: String,
  companyCreation: DateTime,
  companyWebsite: String,
  email: String,
  phone: String,
  vine: Option[String],
  twitter: Option[String],
  angelco: Option[String],
  presentationUrl: Option[String],
  amount: Option[Int]
)

object Record {
  def create(bmc: RecordBMC, info: RecordInfo): Record = {
    Record(
      id = UUID.randomUUID(),
      date = new DateTime,
      bmc = RecordBMC(
        partners = bmc.partners,
        activities = bmc.activities,
        resources = bmc.resources,
        propositions = bmc.propositions,
        customerRelationships = bmc.customerRelationships,
        channels = bmc.channels,
        customerSegments = bmc.customerSegments,
        costStructure = bmc.costStructure,
        revenueStreams = bmc.revenueStreams
      ),
      info = RecordInfo(
        pitch = info.pitch,
        name = info.name,
        company = info.company,
        companyCreation = new DateTime(info.companyCreation),
        companyWebsite = info.companyWebsite,
        email = info.email,
        phone = info.phone,
        vine = info.vine,
        twitter = info.twitter,
        angelco = info.angelco,
        presentationUrl = info.presentationUrl,
        amount = info.amount
      ),
      selected = false
    )
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

  class Records(tag: Tag) extends Table[Record](tag, "records") {
    implicit val datetimeTM = MappedColumnType.base[DateTime, Timestamp](
      d => new Timestamp(d.getMillis),
      t => new DateTime(t)
    )

    def id = column[UUID]("record_id", O.PrimaryKey)
    def date = column[DateTime]("record_date")
    def partners = column[String]("record_partners")
    def activities = column[String]("record_activities")
    def resources = column[String]("record_resources")
    def propositions = column[String]("record_propostions")
    def customerRelationships = column[String]("record_customer-relationships")
    def channels = column[String]("record_channels")
    def customerSegments = column[String]("record_customer-segments")
    def costStructure = column[String]("record_cost-structure")
    def revenueStreams = column[String]("record_revenue-streams")
    def pitch = column[String]("record_pitch")
    def name = column[String]("record_name")
    def companyName = column[String]("record_company_name")
    def companyCreation = column[DateTime]("record_company_creation")
    def companyWebsite = column[String]("record_company_website")
    def email = column[String]("record_email")
    def phone = column[String]("record_phone")
    def vine = column[Option[String]]("record_vine")
    def twitter = column[Option[String]]("record_twitter")
    def angelco = column[Option[String]]("record_angelco")
    def presentationUrl = column[Option[String]]("record_presentationUrl")
    def amount = column[Option[Int]]("record_amount")
    def selected = column[Boolean]("record_selected")
    def * = (
      id, date,
      (partners, activities, resources, propositions, customerRelationships,
      channels, customerSegments, costStructure, revenueStreams),
      (pitch, name, companyName, companyCreation, companyWebsite, email, phone,
      vine, twitter, angelco, presentationUrl, amount),
      selected
    ).shaped <> ({ case (id, date, bmc, info, selected) =>
      Record(
        id, date, RecordBMC.tupled.apply(bmc), RecordInfo.tupled.apply(info),
        selected
      )
    }, { r: Record =>
      Some((
        r.id, r.date, RecordBMC.unapply(r.bmc).get,
        RecordInfo.unapply(r.info).get, r.selected
      ))
    })
  }

  object Records extends TableQuery(new Records(_)) {
    def add(record: Record) = {
      AppDB.database.withSession { implicit session: Session =>
        this.insert(record)
      }
    }

    def get(id: UUID): Option[Record] = {
      AppDB.database.withSession { implicit session: Session =>
        this.filter(_.id === id).firstOption
      }
    }

    def getAll() = {
      AppDB.database.withSession { implicit session: Session =>
        this.list
      }
    }
  }
}
