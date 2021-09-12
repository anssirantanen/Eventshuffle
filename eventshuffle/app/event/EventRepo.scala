package event

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc._

import java.sql.{Date, Types}
import java.time.LocalDate
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.classTag
import scala.util.{Failure, Success}

@Singleton
class EventRepo @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[PostgresProfile]

  import dbConfig._
  import profile.api._

  //Manual conversions as slick's sql api is a torso
  implicit class PgPositionedResult(r: PositionedResult) {
    def nextUUID: UUID = UUID.fromString(r.nextString)

    def nextUUIDOption: Option[UUID] = r.nextStringOption().map(UUID.fromString(_))

  }
  implicit class FromPostgresArray(s: String) {
    def toStringArray: Seq[String] = s.replaceAll ("[{}]", "").split(",").toSeq
  }
  implicit val uuidSetter = SetParameter[UUID] { (uuid, params) =>
    params.setString(uuid.toString)
  }
  implicit val localdateSetter = SetParameter[LocalDate] { (date, params) =>
    params.setDate(Date.valueOf(date))
  }
  implicit val localDateArrayGetter: GetResult[Voted] = GetResult(r => Voted(r.nextDate().toLocalDate, Option(r.nextString()).map(_.toStringArray).getOrElse(Seq.empty)))

  implicit val getListResult: GetResult[EventListItem] = GetResult(r => EventListItem(r.nextUUID, r.<<))


  private def selectEventsQ(): DBIO[Seq[EventListItem]] = sql"""SELECT id, name from event""".as[EventListItem]

  private def selectNameQ(id: UUID): DBIO[Option[String]] = sql"""SELECT name from event where id = ${id}::uuid""".as[String].headOption

  private def selectDaysQ(id: UUID): DBIO[Seq[Voted]] = sql"""SELECT event_date, event_voter FROM event_date WHERE event = ${id}::UUID""".as[Voted]

  private def createEventQ(id: UUID, event: Event) = sqlu"""INSERT INTO event VALUES (${id}::uuid,${event.name})"""

  private def createDaysQ(id: UUID, event: Event) = {
    event.dates.map { date =>
      sqlu"""INSERT INTO event_date VALUES (${date},${id}::uuid)"""
    }
  }
  private def voteDay(id:UUID, day: LocalDate, name: String) =
  sqlu"""UPDATE event_date SET event_voter  = (SELECT array_agg(distinct e) from unnest(event_voter || ARRAY[$name]::text[] ) e ) WHERE event_date = $day AND event = $id::uuid"""
  private def resultQ(id:UUID) =
  sql"""select event_date, event_voter from event_date
         where (select count (voters) from (select distinct( unnest(event_voter)) as voters
         from event_date where event = $id::uuid) s)
          = array_length(event_voter,1) AND event = $id::uuid""".as[Voted]


  def listEvents(): Future[EventList] = {
    db.run(selectEventsQ().asTry).map {
      case Success(value) =>
        EventList(value)
      case Failure(exception) => println(exception.getMessage)
        EventList(Seq.empty)
    }
  }

  def createEvent(event: Event): Future[UUID] = {
    val eventId = UUID.randomUUID()
    val requests = DBIO.sequence(Seq(createEventQ(eventId, event)) ++ createDaysQ(eventId, event)).transactionally
    db.run(requests).map(_ => eventId)
  }

  private def getDetails(id:UUID) = for {
    nameOpt <- selectNameQ(id)
    voted <- selectDaysQ(id)
    res = nameOpt.map(name => Event(Some(id), name, voted.map(_.date), Some(voted)))
  } yield (res)

  def getEvent(id: UUID): Future[Option[Event]] =
    db.run(getDetails(id).transactionally)

  def voteEvent(id:UUID, vote:Vote): Future[Option[Event]] = {
    val qs = vote.votes.map(day => voteDay(id,day,vote.name))
    db.run(DBIO.sequence(qs).andThen(getDetails(id)).transactionally)
  }

  def eventResult(id:UUID) : Future[Option[VoteResult]] = {
    def query = for {
      nameOpt <- selectNameQ(id)
      votes <- resultQ(id)
    } yield (nameOpt.map(name=> VoteResult(id,name,votes)))
    db.run(query)
  }
}

