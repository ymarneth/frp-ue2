package frp.exercises.exercise2_1

import akka.actor.typed.{ActorRef, Behavior, PostStop, PreRestart}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import java.time.LocalDateTime
import scala.util.Random

object MessageReceiver {

  sealed trait Command
  final case class PrintMessage(id: Int, text: String, replyTo: ActorRef[Reply]) extends Command

  sealed trait Reply
  final case class MessageReceived(id: Int, text: String, arrivalTime: LocalDateTime) extends Reply

  private def loseMessageOrConfirmation(probability: Double): Boolean =
    Random.nextDouble() < probability

  def apply(): Behavior[Command] =
    Behaviors
      .receive[Command] {
        case (context, PrintMessage(id, text, replyTo)) =>
          if (loseMessageOrConfirmation(0.3)) {
            context.log.info(s"Message $id: $text lost")
          } else {
            context.log.info(s"Message $id: $text received at ${LocalDateTime.now()}")
            replyTo ! MessageReceived(id, text, LocalDateTime.now())
          }
          Behaviors.same
      }
      .receiveSignal {
        case (context, PreRestart) =>
          context.log.info(s"${context.self.path.name} restarted")
          Behaviors.same
        case (context, PostStop) =>
          context.log.info(s"${context.self.path.name} stopped")
          Behaviors.same
      }
}
