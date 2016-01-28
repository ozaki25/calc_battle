package actors

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.json.{Json, JsValue}
import scala.util.Random

object UserActor {
  def props(uid: String)(out: ActorRef) = Props(new UserActor(uid, FieldActor.field, out))
  case class UpdateUsers(results: Map[String, Int])
  case class UpdateUser(result: (String, Int), finish: Boolean)
}


class UserActor(uid: String, field: ActorRef, out: ActorRef) extends Actor {
  import FieldActor.{Subscribe, Result}
  import UserActor.{UpdateUser, UpdateUsers}

  override def preStart() = {
    FieldActor.field ! Subscribe(uid)
  }

  def receive = {
    case js: JsValue => {
      (js \ "result").validate[Boolean] foreach { field ! Result(_) }
      val question = Json.obj("type" -> "question", "question" -> Map("a" -> random(), "b" -> random()))
      out ! question
    }
    case Result(isCorrect) if sender == field => {
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

  def random() = (Random.nextInt(9) + 1) * 10 + Random.nextInt(10)
}
