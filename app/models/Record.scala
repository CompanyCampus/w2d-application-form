package models
import java.util.UUID
import scala.util.Try
import org.joda.time.DateTime
import java.sql.Timestamp
import scala.slick.session._

case class Record (
  id: UUID,
  date: DateTime,
  partners: String,
  activities: String,
  resources: String,
  propositions: String,
  customerRelationships: String,
  channels: String,
  customerSegments: String,
  costStructure: String,
  revenueStreams: String,
  pitch: String,
  name: String,
  company: String,
  companyCreation: DateTime,
  companyWebsite: String,
  email: String,
  phone: String,
  twitter: String,
  angelco: String,
  presentationUrl: String,
  amount: Integer
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
    id: UUID, date: Timestamp,
    partners: String, activities: String, resources: String, propositions: String,
    customerRelationships: String, channels: String, customerSegments: String,
    costStructure: String, revenueStreams: String,
    pitch: String, name: String, company: String, companyCreation: Timestamp,
    companyWebsite: String, email: String, phone: String, twitter: String,
    angelco: String, presentationUrl: String, amount: Int
  ): Record = {
    Record(
      id = id,
      date = new DateTime(date),
      partners = partners,
      activities = activities,
      resources = resources,
      propositions = propositions,
      customerRelationships = customerRelationships,
      channels = channels,
      customerSegments = customerSegments,
      costStructure = costStructure,
      revenueStreams = revenueStreams,
      pitch = pitch,
      name = name,
      company = company,
      companyCreation = new DateTime(companyCreation),
      companyWebsite = companyWebsite,
      email = email,
      phone = phone,
      twitter = twitter,
      angelco = angelco,
      presentationUrl = presentationUrl,
      amount = amount
    )
  }
  def unapplyToDAL(record: Record): Option[(
    UUID, Timestamp,
    String, String, String, String,
    String, String, String,
    String, String,
    String, String, String, Timestamp,
    String, String, String, String,
    String, String, Int
  )] = {
    Some(
      record.id, new Timestamp(record.date.getMillis),
      record.partners, record.activities, record.resources, record.propositions,
      record.customerRelationships, record.channels, record.customerSegments,
      record.costStructure, record.revenueStreams,
      record.pitch, record.name, record.company, new Timestamp(record.companyCreation.getMillis),
      record.companyWebsite, record.email, record.phone, record.twitter,
      record.angelco, record.presentationUrl, record.amount
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

  object Records extends Table[Record]("records") {
    def id = column[UUID]("record_id", O.PrimaryKey)
    def date = column[Timestamp]("record_date")
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
    def company = column[String]("record_company")
    def companyCreation = column[Timestamp]("record_company-creation")
    def companyWebsite = column[String]("record_company-website")
    def email = column[String]("record_email")
    def phone = column[String]("record_phone")
    def twitter = column[String]("record_twitter")
    def angelco = column[String]("record_angelco")
    def presentationUrl = column[String]("record_presentationUrl")
    def amount = column[Int]("record_amount")
    def * = (
      id ~ date ~ partners ~ activities ~ resources ~ propositions ~
      customerRelationships ~ channels ~ customerSegments ~
      costStructure ~ revenueStreams ~ pitch ~ name ~ company ~
      companyCreation ~ companyWebsite ~ email ~ phone ~ twitter ~
      angelco ~ presentationUrl ~ amount
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
