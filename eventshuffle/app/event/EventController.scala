package event

import event.Event._
import play.api.mvc._
import play.api.libs.json.{Json, Reads}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EventController @Inject()(val controllerComponents: ControllerComponents, eventRepo: EventRepo)(implicit ec: ExecutionContext) extends BaseController {

  def list = Action.async {
    req =>
      eventRepo.listEvents().map{
        events =>
          Ok(Json.toJson(events))
      }
  }

  def getEvent(id: UUID) = Action.async {
    eventRepo
      .getEvent(id)
      .map {
        case Some(event) => Ok(Json.toJson(event))
        case None => NotFound
      }
  }

  def createEvent() = Action.async(parse.json[Event]) { req =>
    val event : Event= req.body
    eventRepo.createEvent(event)
      .map(uuid => Map("id"-> uuid))
      .map(map => Ok(Json.toJson(map)))
  }

  def voteEvent(id: UUID) = Action.async(parse.json[Vote]) { req =>
    val vote = req.body
    eventRepo
      .voteEvent(id,vote)
      .map {
      case Some(event) => Ok(Json.toJson(event))
      case None => NotFound
    }

  }
  def eventResult(id:UUID) = Action.async{
    eventRepo.eventResult(id).map {
      case Some(event) => Ok(Json.toJson(event))
      case None => NotFound
    }
  }
}

