package frp.exercises.exercise1

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop, PreRestart}
import frp.basics.actors.PrimeUtil.isPrime

import java.time.LocalDateTime

object MessageSender {
  import Messages._

  def apply(): Behavior[SendMessage] = Behaviors.setup { context =>
    context.log.info("Sender actor started and ready to send messages.")

    val receiver = context.spawn(MessageReceiver(), "receiverActor")

    Behaviors.receiveMessage { message =>
      val currentTime = LocalDateTime.now()
      receiver ! PrintMessage(message.id, message.text, currentTime)
      context.log.info(s"Sent message with ID: ${message.id}, Text: '${message.text}'")
      Behaviors.same
    }
  }
}
