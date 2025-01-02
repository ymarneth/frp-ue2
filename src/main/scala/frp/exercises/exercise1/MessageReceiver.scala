package frp.exercises.exercise1

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import java.time.LocalDateTime

object MessageReceiver {
  import Messages._

  def apply(): Behavior[PrintMessage] = Behaviors.setup { context =>
    context.log.info("Receiver actor started and ready to handle messages.")

    Behaviors.receiveMessage { message =>
      context.log.info(s"Received message with ID: ${message.id}, Text: '${message.text}' at ${message.arrivalTime.toString}")
      println(s"Received message with ID: ${message.id}, Text: '${message.text}' at ${message.arrivalTime.toString}")
      Behaviors.same
    }
  }
}
