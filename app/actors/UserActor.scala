package actors

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.json.{Json, JsValue}
import models.Question

object UserActor {
  def props(uid: String)(out: ActorRef) = Props(new UserActor(uid, FieldActor.field, out))
  case class UpdateUsers(results: Map[String, Int])
  case class UpdateUser(result: (String, Int), finish: Boolean)
}


class UserActor(uid: String, field: ActorRef, out: ActorRef) extends Actor {
  import UserActor.{UpdateUser, UpdateUsers}

  override def preStart() = {
    FieldActor.field ! FieldActor.Subscribe(uid)
  }

  def receive = {
    case js: JsValue => {
      (js \ "result").validate[Boolean] foreach { field ! FieldActor.Result(_) }
      val question = Json.obj("type" -> "question", "question" -> Question.create())
      out ! question
    }
    case FieldActor.Result(isCorrect) if sender == field => {
      val js = Json.obj("type" -> "result", "uid" -> uid, "isCorrect" -> isCorrect)
      out ! js
    }
    case UpdateUser(result, finish) if sender == field => {
      val js = Json.obj("type" -> "updateUser", "user" -> Map(result), "finish" -> finish)
      out ! js
    }
    case UpdateUsers(results: Map[String, Int]) if sender == field => {
      val js = Json.obj("type" -> "updateUsers", "users" -> results)
      out ! js
    }
  }
}
