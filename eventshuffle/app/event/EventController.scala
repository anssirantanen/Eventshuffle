package event

import event.Event._
import play.api.mvc._
import play.api.libs.json.{Json, Reads}
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

  def getEvent(id: String) = Action.async {
    Future.successful(Ok(""))
  }

  def createEvent() = Action.async(parse.json[Event]) { req =>
    val event : Event= req.body
    eventRepo.createEvent(event)
      .map(uuid => Map("id"-> uuid))
      .map(map => Ok(Json.toJson(map)))
  }

  def voteEvent(id: String) = Action.async(parse.json[Vote]) { req =>
    Future.successful(Ok(""))
  }

}

