package actors

import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import akka.routing.FromConfig
import play.api.libs.json.{Json, JsValue, Writes}
import play.libs.Akka
import SocketActor._
import com.example.calcbattle.examiner.actors.ExaminerActor
import com.example.calcbattle.examiner.models.Question
import com.example.calcbattle.user.actors.FieldActor
import com.example.calcbattle.user.actors.FieldActor.UID
import com.example.calcbattle.user.actors.UserWorker

object SocketActor {
  val examinerRouter = Akka.system().actorOf(FromConfig.props(), name = "examinerRouter")
  val userRouter = Akka.system().actorOf(FromConfig.props(), name = "userRouter")
  def props(uid: UID)(out: ActorRef) = Props(new SocketActor(uid, examinerRouter, userRouter, out))

  implicit val userWrites = new Writes[UserWorker.UpdateUser] {
    def writes(user: UserWorker.UpdateUser): JsValue = {
      Json.obj("uid" -> user.uid.id, "continuationCorrect" -> user.continuationCorrect)
    }
  }

  implicit val questionWrites = new Writes[Question] {
    def writes(question: Question): JsValue = {
      Json.obj("first" -> question.first, "second" -> question.second)
    }
  }
}

class SocketActor(uid: UID, examinerRouter: ActorRef, userRouter: ActorRef, out: ActorRef) extends Actor {
  override def preStart() = {
    userRouter ! FieldActor.Join(uid)
  }

  def receive = {
    case js: JsValue =>
      (js \ "result").validate[Boolean] foreach { isCorrect =>
        userRouter ! UserWorker.Result(uid, isCorrect)
      }
      examinerRouter ! ExaminerActor.Create
    case q: Question =>
      val question = Json.obj("type" -> "question", "question" -> q)
      out ! question
    case FieldActor.Participation(users) =>
      println(users)
      val uids = users map { _.id }
      val js = Json.obj("type" -> "participation", "uids" -> uids)
      out ! js
    case u:UserWorker.UpdateUser =>
      println(u)
      val js = Json.obj("type" -> "updateUser", "user" -> u)
      out ! js
    case msg =>
      println(msg)
  }
}
