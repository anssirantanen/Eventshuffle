package event

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc._

import java.sql.{Date, Types}
import java.time.LocalDate
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class EventRepo @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext){
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  //Manual conversions as slick's sql api is a torso
  implicit class PgPositionedResult(r: PositionedResult) {
    def nextUUID : UUID =  UUID.fromString(r.nextString)
    def nextUUIDOption : Option[UUID] = r.nextStringOption().map(UUID.fromString(_))
  }

  implicit val getListResult = GetResult(r=>  EventListItem(r.nextUUID,r.<<))
  implicit val uuidSetter = SetParameter[UUID]{(uuid, params )=>
    params.setString(uuid.toString)
  }
  implicit val localdateSetter = SetParameter[LocalDate]{ (date, params )=>
    params.setDate(Date.valueOf(date))
  }
  private def selectEventsQ():DBIO[Seq[EventListItem]] = sql"""SELECT id, name from event""".as[EventListItem]
  private def selectEvent(id:UUID):DBIO[Option[UUID]] = sql"""SELECT name from event where id = ${id}::uuid""".as[UUID].headOption
  private def createEventQ(id:UUID,event:Event) = sqlu"""INSERT INTO event VALUES (${id}::uuid,${event.name})"""
  private def createDaysQ(id:UUID,event: Event) = {
    event.dates.map{date =>
      sqlu"""INSERT INTO event_date VALUES (${date},${id}::uuid)"""
    }
  }

  def listEvents():Future[EventList] = {
    db.run(selectEventsQ().asTry).map {
      case Success(value) =>
        EventList(value)
      case Failure(exception) =>  println(exception.getMessage)
        EventList(Seq.empty)
  }
  }
  def createEvent(event: Event)= {
    val eventId = UUID.randomUUID()
    val requests = DBIO.sequence(Seq(createEventQ(eventId,event)) ++ createDaysQ(eventId,event)).transactionally
    db.run(requests).map(_ => eventId)
  }

  def getEvent(id:UUID): Future[Option[Event]] = {

    val tran = for(
      getName = selectEvent(id)
    ) yield ()
  db.run(selectEvent(id)).
  }
}

