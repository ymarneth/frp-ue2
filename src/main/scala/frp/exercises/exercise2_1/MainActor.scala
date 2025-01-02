package frp.exercises.exercise2_1

import akka.actor.typed.{Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration.DurationInt

object MainActor {

  import MessageSender.{MessageDelivered, MessageDeliveryFailed}

  private val defaultMessageSenderConfig = MessageSenderConfig(maxRetries = 3, messageCount = 20, retryInterval = 40.millis)

  def apply(config: MessageSenderConfig = defaultMessageSenderConfig): Behavior[MessageSender.Reply] = Behaviors.setup { context =>
    val worker = context.spawn(MessageReceiver(), "message-receiver")

    context.spawn(MessageSender(context.self, worker, config), "message-sender")

    context.children.foreach(c => context.watch(c))

    Behaviors.receiveMessage[MessageSender.Reply] {
        case MessageDelivered(id, message, arrivalTime) =>
          println(s"Message $id: $message delivered at $arrivalTime")
          Behaviors.same
        case MessageDeliveryFailed(id, message, reason) =>
          println(s"Message $id: $message could not be delivered. Reason: $reason")
          Behaviors.same
      }
      .receiveSignal {
        case (context, Terminated(_)) =>
          if context.children.size <= 1 then
            context.log.info("All message senders have stopped. Shutting down the system.")
            context.system.terminate()
            Behaviors.stopped
          else
            Behaviors.same
      }
  }
}
