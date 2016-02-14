package actors

import actors.SocketActor._
import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import akka.routing.FromConfig
import play.api.libs.json.{Json, JsValue, Writes}
import play.libs.Akka
import com.example.calcbattle.examiner.actors.ExaminerActor
import com.example.calcbattle.examiner.models.Question
import com.example.calcbattle.user.actors.FieldActor.{Participation, UID, Join}
import com.example.calcbattle.user.actors.UserWorker.{Result, User}

object SocketActor {
  val examinerRouter = Akka.system().actorOf(FromConfig.props(), name = "examinerRouter")
  val userRouter = Akka.system().actorOf(FromConfig.props(), name = "userRouter")
  def props(uid: UID)(out: ActorRef) = Props(new SocketActor(uid, FieldActor.field, examinerRouter, userRouter, out))

  case class User(uid: UID, continuationCorrect: Int)
  case class UpdateUser(result: User, finish: Boolean)
  case class UpdateUsers(results: Set[User])

  implicit val userWrites = new Writes[User] {
    def writes(user: SocketActor.User): JsValue = {
      Json.obj(user.uid.id -> user.continuationCorrect)
    }
  }

  implicit val usersWrites = new Writes[Set[SocketActor.User]] {
    def writes(users: Set[SocketActor.User]): JsValue = {
      Json.toJson(users.map { user: SocketActor.User =>
        user.uid.id -> user.continuationCorrect
      }.toMap)
    }
  }

  implicit val questionWrites = new Writes[Question] {
    def writes(question: Question): JsValue = {
      Json.obj("first" -> question.first, "second" -> question.second)
    }
  }

  implicit val tmpUserWrites = new Writes[com.example.calcbattle.user.actors.UserWorker.User] {
    def writes(user: com.example.calcbattle.user.actors.UserWorker.User): JsValue = {
      Json.obj(user.uid.id -> user.continuationCorrect)
    }
  }
}

class SocketActor(uid: UID, field: ActorRef, examinerRouter: ActorRef, userRouter: ActorRef, out: ActorRef) extends Actor {
  override def preStart() = {
    userRouter ! Join(uid)
    FieldActor.field ! FieldActor.Subscribe(uid)
  }

  def receive = {
    case js: JsValue =>
      (js \ "result").validate[Boolean] foreach { isCorrect =>
        field ! FieldActor.Result(isCorrect)
        userRouter ! com.example.calcbattle.user.actors.UserWorker.Result(isCorrect)
      }
      examinerRouter ! ExaminerActor.Create
    case q: Question =>
      val question = Json.obj("type" -> "question", "question" -> q)
      out ! question
    case UpdateUser(user, finish) =>
      val js = Json.obj("type" -> "updateUser", "user" -> user, "finish" -> finish)
      out ! js
    case UpdateUsers(users) =>
      val js = Json.obj("type" -> "updateUsers", "users" -> users)
      out ! js
    case Participation(users) =>
      val uids: Set[String] = users map { _.id }
      val js = Json.obj("type" -> "participation", "uids" -> uids)
      out ! js
    case u:com.example.calcbattle.user.actors.UserWorker.User =>
      val js = Json.obj("type" -> "tmpUpdateUser", "user" -> u)
      out ! js
  }
}
