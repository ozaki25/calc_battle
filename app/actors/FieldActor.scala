package actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import play.libs.Akka

object FieldActor {
  lazy val field = Akka.system().actorOf(Props[FieldActor])
  def apply() = field
}

case class Result(uid: String, isCorrect: Boolean)
case class Subscribe(uid: String)
case class User(uid: String, continuationCorrect: Int, userActor: ActorRef)

class FieldActor extends Actor {
  var users = Set[User]()

  def receive = {
    case Result(uid, isCorrect) => {
      val user = (users filter(_.uid == uid)).head
      val updateUser = user.copy(continuationCorrect = if(isCorrect) user.continuationCorrect + 1 else 0)
      val result = updateUser.uid -> updateUser.continuationCorrect
      val finish = updateUser.continuationCorrect >= 5
      users -= user
      users += updateUser
      users foreach { _.userActor ! UpdateUser(result, finish) }
    }
    case Subscribe(uid: String) => {
      users += User(uid, 0, sender)
      context watch sender
      val results = (users map { u => u.uid -> u.continuationCorrect }).toMap[String, Int]
      users foreach { _.userActor ! UpdateUsers(results) }
    }
    case Terminated(user) => {
      users.foreach { u => if(u.userActor == user) users -= u }
      val results = (users map { u => u.uid -> u.continuationCorrect }).toMap[String, Int]
      users foreach { _.userActor ! UpdateUsers(results) }
    }
  }
}
