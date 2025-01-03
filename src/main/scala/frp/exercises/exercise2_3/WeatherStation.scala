package frp.exercises.exercise2_3

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import java.time.LocalDateTime
import scala.concurrent.duration.DurationInt
import scala.util.Random

object WeatherStation {

  sealed trait Command

  case object StartWeatherStation extends Command

  private case object GenerateMessage extends Command

  private final case class WrappedAcknowledgement(reply: DataStorage.Reply) extends Command

  def apply(nrMessages: Int = 100,
            messagesPerSecond: Int = 20,
            processingActor: ActorRef[DataStorage.Command]): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new WeatherStation(context, timers, nrMessages, messagesPerSecond, processingActor).init()
      }
    }
}

class WeatherStation(context: ActorContext[WeatherStation.Command],
                     timers: TimerScheduler[WeatherStation.Command],
                     nrMessages: Int,
                     messagesPerSecond: Int,
                     processingActor: ActorRef[DataStorage.Command]) {

  import WeatherStation._

  private val storageAdapter: ActorRef[DataStorage.Reply] = context.messageAdapter(reply => WrappedAcknowledgement(reply))

  private def init(): Behavior[Command] = Behaviors.receiveMessage {
    case StartWeatherStation =>
      context.log.info("WeatherStation started. Generating measurements.")
      timers.startTimerAtFixedRate(GenerateMessage, 1.second / messagesPerSecond)
      active(1, 0)
  }

  private def active(messageCount: Int, ackCount: Int): Behavior[Command] =
    Behaviors.receiveMessage {
      case GenerateMessage =>
        if (messageCount > nrMessages) {
          if (ackCount >= nrMessages) {
            context.log.info("All messages sent and acknowledged. Shutting down.")
            println("All messages sent and acknowledged. WeatherStation stopping.")
            Behaviors.stopped
          } else {
            Behaviors.same
          }
        } else {
          val measurement = Measurement(
            id = messageCount,
            temperature = Random.between(-20.0, 40.0),
            timestamp = LocalDateTime.now()
          )
          context.log.debug(s"Generated measurement: $measurement")
          processingActor ! DataStorage.InsertMeasurement(measurement, storageAdapter)
          active(messageCount + 1, ackCount)
        }

      case WrappedAcknowledgement(DataStorage.Acknowledged(id)) =>
        context.log.info(s"Measurement $id acknowledged.")
        println(s"Measurement $id acknowledged.")
        if (ackCount + 1 >= nrMessages) {
          context.log.info("All messages acknowledged. WeatherStation stopping.")
          println("All messages acknowledged. WeatherStation stopping.")
          Behaviors.stopped
        } else {
          active(messageCount, ackCount + 1)
        }
    }
}
