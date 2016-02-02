package actors

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import play.api.libs.json.{Json, JsValue, Writes}
import models.Question
import UserActor._

object UserActor {
  def props(uid: UID)(out: ActorRef) = Props(new UserActor(uid, FieldActor.field, out))

  val workerRouter = context.actorOf(FromConfig.props(Props[ExaminerActor]), name = "workerRouter")


  val system = ActorSystem("ClusterSystem", config)
  system.actorOf(Props[StatsWorker], name = "statsWorker")
  system.actorOf(Props[StatsService], name = "statsService")

  case class User(uid: UID, continuationCorrect: Int)
  case class UpdateUsers(results: Set[User])
  case class UpdateUser(result: User, finish: Boolean)
  class UID(val id: String) extends AnyVal

  implicit val userWrites = new Writes[User] {
    def writes(user: User): JsValue = {
      Json.obj(user.uid.id -> user.continuationCorrect)
    }
  }

  implicit val usersWrites = new Writes[Set[User]] {
    def writes(users: Set[User]): JsValue = {
      Json.toJson(users.map { user: User =>
        user.uid.id -> user.continuationCorrect
      }.toMap)
    }
  }
}

class UserActor(uid: UID, field: ActorRef, out: ActorRef) extends Actor with ActorLogging {
  override def preStart() = {
    FieldActor.field ! FieldActor.Subscribe(uid)
  }

  def receive = {
    case js: JsValue => {
      (js \ "result").validate[Boolean] foreach { field ! FieldActor.Result(_) }
      val question = Json.obj("type" -> "question", "question" -> Question.create())
      out ! question
    }
    case UpdateUser(user, finish) if sender == field => {
      val js = Json.obj("type" -> "updateUser", "user" -> user, "finish" -> finish)
      out ! js
    }
    case UpdateUsers(users) if sender == field => {
      val js = Json.obj("type" -> "updateUsers", "users" -> users)
      out ! js
    }
  }
}
