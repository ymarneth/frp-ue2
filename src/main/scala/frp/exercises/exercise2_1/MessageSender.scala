package frp.exercises.exercise2_1

import scala.concurrent.duration.DurationInt
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import frp.exercises.exercise2_1.MessageSender.WrappedMessageReceived

import java.time.LocalDateTime

object MessageSender {
  sealed trait Command

  private object SendMessages extends Command
  private final case class WrappedMessageReceived(reply: MessageReceiver.Reply) extends Command

  sealed trait Reply

  final case class MessageDelivered(id: Int, message: String, arrivalTime: LocalDateTime) extends Reply
  final case class MessageDeliveryFailed(id: Int, message: String, reason: String) extends Reply

  def apply(replyTo: ActorRef[Reply], messageReceiver: ActorRef[MessageReceiver.Command], config: MessageSenderConfig): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers(timer => new MessageSender(context, replyTo, messageReceiver, timer, config))
  }
}

class MessageSender(context: ActorContext[MessageSender.Command],
                    replyTo: ActorRef[MessageSender.Reply],
                    worker: ActorRef[MessageReceiver.Command],
                    timer: TimerScheduler[MessageSender.Command],
                    config: MessageSenderConfig)
  extends AbstractBehavior[MessageSender.Command](context):

  private val messageAdapter: ActorRef[MessageReceiver.Reply] = context.messageAdapter(reply => WrappedMessageReceived(reply))

  private var unsentMessages = Map.empty[Int, (String, Int, Long)]

  generateMessages()
  sendUnsentMessages()

  timer.startTimerAtFixedRate(MessageSender.SendMessages, config.retryInterval)

  private def generateMessages(): Unit = {
    unsentMessages = (1 to config.messageCount).map(i => i -> (s"Message $i", 0, System.currentTimeMillis())).toMap
  }

  private def sendUnsentMessages(): Unit = {
    val currentTime = System.currentTimeMillis()
    for((id, (message, retries, lastSentTime)) <- unsentMessages) {
      if (currentTime - lastSentTime >= config.retryInterval.toMillis) {
        if (retries < config.maxRetries) {
          if (retries < 1) {
            context.log.info(s"Sending message $id: $message")
            println(s"Sending message $id: $message")
          } else {
            context.log.info(s"Retrying message $id: $message (Retry #$retries)")
            println(s"Retrying message $id: $message (Retry #$retries)")
          }
          worker ! MessageReceiver.PrintMessage(id, message, messageAdapter)
          unsentMessages += (id -> (message, retries + 1, currentTime))
        } else {
          replyTo ! MessageSender.MessageDeliveryFailed(id, message, s"Exceeded ${config.maxRetries} retries")
          unsentMessages -= id
        }
      }
    }
  }

  override def onMessage(msg: MessageSender.Command): Behavior[MessageSender.Command] = msg match
    case MessageSender.WrappedMessageReceived(reply) =>
      reply match
        case MessageReceiver.MessageReceived(id, _, arrivalTime) =>
          context.log.info(s"Message $id delivered at $arrivalTime")
          replyTo ! MessageSender.MessageDelivered(id, unsentMessages(id)._1, arrivalTime)
          unsentMessages -= id
          if unsentMessages.isEmpty then
            Behaviors.stopped
          else
            Behaviors.same
    case MessageSender.SendMessages =>
      sendUnsentMessages()
      Behaviors.same
end MessageSender