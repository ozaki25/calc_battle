package actors

import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.routing.FromConfig
import play.api.libs.json.{Json, JsValue, Writes}
import play.libs.Akka
import UserActor._
import com.example.calcbattle.examiner.actors.ExaminerActor
import com.example.calcbattle.examiner.models.Question

object UserActor {
  val examiner = Akka.system().actorOf(FromConfig.props(), name = "examinerRouter")
  def props(uid: UID)(out: ActorRef) = Props(new UserActor(uid, FieldActor.field, examiner, out))

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

  implicit val questuinWrites = new Writes[Question] {
    def writes(question: Question): JsValue = {
      Json.obj("first" -> question.first, "second" -> question.second)
    }
  }
}

class UserActor(uid: UID, field: ActorRef, examiner: ActorRef, out: ActorRef) extends Actor {
  override def preStart() = {
    FieldActor.field ! FieldActor.Subscribe(uid)
  }

  def receive = {
    case js: JsValue => {
      (js \ "result").validate[Boolean] foreach { field ! FieldActor.Result(_) }
      examiner ! ExaminerActor.Create
    }
    case q: Question => {
      val question = Json.obj("type" -> "question", "question" -> q)
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
