package controllers


import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json.JsValue

import scala.concurrent.Future
import java.util.UUID

import actors.SocketActor
import com.example.calcbattle.user.actors.FieldActor.UID

class Application extends Controller {
  val UID = "uid"

  def index = Action { implicit request =>
    val uid: String = request.session.get(UID).getOrElse {
      UUID.randomUUID().toString.replace("_", "")
    }
    Ok(views.html.index(uid)).withSession {
      request.session + (UID -> uid)
    }
  }

  def ws = WebSocket.tryAcceptWithActor[JsValue, JsValue] { implicit request =>
    Future.successful(request.session.get(UID) match {
      case None => Left(Forbidden)
      case Some(uid) => Right(SocketActor.props(new UID(uid)))
    })
  }
}
