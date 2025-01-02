package frp.exercises.exercise1

import akka.actor.typed.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration.Duration

@main
def messageExchangeMain(): Unit = {
  println("==================== MessageSenderApp ==========================")

  val system = ActorSystem(MessageSender(), "message-exchange-system")

  val sender = system
  
  sender ! Messages.SendMessage(1, "Hello, Receiver!")
  sender ! Messages.SendMessage(2, "How are you?")
  sender ! Messages.SendMessage(3, "Goodbye!")

  Thread.sleep(3000)
  system.terminate()
  Await.ready(system.whenTerminated, Duration.Inf)

  println("==================== System Terminated ==========================")
}
