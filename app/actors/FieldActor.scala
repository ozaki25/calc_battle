package actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import play.libs.Akka

object FieldActor {
  lazy val field = Akka.system().actorOf(Props[FieldActor])
  case class Result(isCorrect: Boolean)
  case class Subscribe(uid: String)
  case class User(uid: String, continuationCorrect: Int)
}


class FieldActor extends Actor {
  import FieldActor.{Result, User, Subscribe}

  var users = Map[ActorRef, User]()

  def receive = {
    case Result(isCorrect) => {
      print(users)
      val user = users(sender)
      val updateUser = user.copy(continuationCorrect = if(isCorrect) user.continuationCorrect + 1 else 0)
      val result = updateUser.uid -> updateUser.continuationCorrect
      val finish = updateUser.continuationCorrect >= 5
      users -= sender
      users += (sender -> updateUser)
      users.keys foreach { _ ! UserActor.UpdateUser(result, finish) }
    }
    case Subscribe(uid) => {
      users += (sender -> User(uid, 0))
      context watch sender
      val results = (users.values map { u => u.uid -> u.continuationCorrect }).toMap[String, Int]
      users.keys foreach { _ ! UserActor.UpdateUsers(results) }
    }
    case Terminated(user) => {
      users -= user
      val results = (users.values map { u => u.uid -> u.continuationCorrect }).toMap[String, Int]
      users.keys foreach { _ ! UserActor.UpdateUsers(results) }
    }
  }
}
