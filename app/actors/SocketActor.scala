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
    case FieldActor.Participation(uids) =>
      println(uids)
      val handler = context.actorOf(UsersHandler.props(uids.size, replyTo = self))
      uids.foreach { uid =>
        userRouter.tell(UserWorker.Get(uid), handler)
      }
    case u:UserWorker.UpdateUser =>
      println(u)
      val js = Json.obj("type" -> "updateUser", "user" -> u)
      out ! js
    case UsersHandler.UpdateUsers(users) =>
      println(users)
      val js = Json.obj("type" -> "updateUsers", "users" -> users)
      out ! js
    case msg =>
      println(msg)
  }
}


object UsersHandler {
  def props(userSize: Int, replyTo: ActorRef) = Props(new UserHandler(userSize, replyTo))
  case class UpdateUsers(users: Set[UserWorker.UpdateUser])
  case object UserGetTimeout
}

class UserHandler(userSize: Int, replyTo: ActorRef) extends Actor {
  import akka.actor.ReceiveTimeout
  import scala.concurrent.duration._
  import UsersHandler._

  context.setReceiveTimeout(5 seconds)

  var users: Set[UserWorker.UpdateUser] = Set()

  def receive = {
    case user: UserWorker.UpdateUser =>
      context.setReceiveTimeout(1 second)
      users += user
      if(users.size == userSize) {
        println(users)
        replyTo ! UpdateUsers(users)
        context.stop(self)
      }
    case e: ReceiveTimeout =>
      replyTo ! UserGetTimeout
      context.stop(self)

  }
}