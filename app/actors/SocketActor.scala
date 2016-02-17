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
  val userRouter     = Akka.system().actorOf(FromConfig.props(), name = "userRouter")

  def props(uid: UID)(out: ActorRef) = Props(new SocketActor(uid, examinerRouter, userRouter, out))

  implicit val userWrites = new Writes[UserWorker.Updated] {
    def writes(user: UserWorker.Updated): JsValue = {
      Json.obj("uid" -> user.uid.id, "correctCount" -> user.correctCount, "nicName" -> user.name)
    }
  }

  implicit val questionWrites = new Writes[Question] {
    def writes(question: Question): JsValue = {
      Json.obj("first" -> question.first, "second" -> question.second)
    }
  }

  implicit val answerReads = Json.reads[ExaminerActor.Answer]
}

class SocketActor(uid: UID, examinerRouter: ActorRef, userRouter: ActorRef, out: ActorRef) extends Actor with ActorLogging {
  override def preStart() = {
    userRouter ! UserWorker.Create(uid)
  }

  def receive = {
    case js: JsValue =>
      (js \ "name").validate[String] foreach { userRouter ! UserWorker.UpdateName(uid, _) }
      (js \ "answer").validate[ExaminerActor.Answer] foreach { examinerRouter ! ExaminerActor.Check(_) }
      examinerRouter ! ExaminerActor.Create
    case ExaminerActor.Exam(q) =>
      val question = Json.obj("type" -> "question", "question" -> q)
      out ! question
    case ExaminerActor.Result(isCorrect) =>
      userRouter ! UserWorker.UpdateCorrectCount(uid, isCorrect)
    case FieldActor.UpdatedUserList(uids) =>
      val handler = context.actorOf(UsersHandler.props(uids.size, replyTo = self))
      uids.foreach { uid =>
        userRouter.tell(UserWorker.Get(uid), handler)
      }
    case u:UserWorker.Updated =>
      val js = Json.obj("type" -> "updateUser", "user" -> u, "finish" -> u.isFinish)
      out ! js
    case UsersHandler.UpdateUsers(users) =>
      val js = Json.obj("type" -> "updateUsers", "users" -> users)
      out ! js
    case UsersHandler.UsersGetTimeout =>
      log.warning("ユーザ一覧を取得できませんでした。")
    case UserWorker.DuplicateRequest =>
      log.warning("二重アクセスはできません。")
  }
}


object UsersHandler {
  def props(userSize: Int, replyTo: ActorRef) = Props(new UsersHandler(userSize, replyTo))
  case class UpdateUsers(users: Set[UserWorker.Updated])
  case object UsersGetTimeout
}

class UsersHandler(userSize: Int, replyTo: ActorRef) extends Actor {
  import akka.actor.ReceiveTimeout
  import scala.concurrent.duration._
  import UsersHandler._

  context.setReceiveTimeout(5 seconds)

  var users: Set[UserWorker.Updated] = Set()

  def receive = {
    case user: UserWorker.Updated =>
      context.setReceiveTimeout(1 second)
      users += user
      if(users.size == userSize) {
        replyTo ! UpdateUsers(users)
        context.stop(self)
      }
    case e: ReceiveTimeout =>
      replyTo ! UsersGetTimeout
      context.stop(self)
  }
}