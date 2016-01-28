package actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import play.libs.Akka
import UserActor.{UID, User}

object FieldActor {
  lazy val field = Akka.system().actorOf(Props[FieldActor])
  case class Result(isCorrect: Boolean)
  case class Subscribe(uid: UID)
}

class FieldActor extends Actor {
  import FieldActor.{Result, Subscribe}

  var users = Map[ActorRef, User]()

  def receive = {
    case Result(isCorrect) => {
      val user = users(sender)
      val updateUser = user.copy(continuationCorrect = if(isCorrect) user.continuationCorrect + 1 else 0)
      val finish = updateUser.continuationCorrect >= 5
      users -= sender
      users += (sender -> updateUser)
      users.keys foreach { _ ! UserActor.UpdateUser(updateUser, finish) }
    }
    case Subscribe(uid) => {
      users += (sender -> User(uid, 0))
      context watch sender
      val updateUsers = UserActor.UpdateUsers(users.values.toSet)
      users.keys foreach { _ ! updateUsers }
    }
    case Terminated(user) => {
      users -= user
      val updateUsers = UserActor.UpdateUsers(users.values.toSet)
      users.keys foreach { _ ! updateUsers }
    }
  }
}
