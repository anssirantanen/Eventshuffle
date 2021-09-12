package event

import java.time.LocalDate
import play.api.libs.json._

import java.util.UUID

case class Event (id: Option[String], name : String, dates: Seq[LocalDate], votes: Option[Seq[Voted]])
case class EventList(events:Seq[EventListItem])
case class EventListItem(id: UUID, name : String)

case class Voted(date: LocalDate, people: Seq[String])
case class Vote(name: String, votes: Seq[LocalDate])

object Event {
  implicit val eventFormat: OFormat[Event] = Json.format[Event]
  implicit val eventListItemFormat: OFormat[EventListItem] = Json.format[EventListItem]
  implicit val eventListFormat: OFormat[EventList] = Json.format[EventList]

  implicit val votedFormat: OFormat[Voted] = Json.format[Voted]
  implicit val voteFormat: OFormat[Vote] = Json.format[Vote]
}