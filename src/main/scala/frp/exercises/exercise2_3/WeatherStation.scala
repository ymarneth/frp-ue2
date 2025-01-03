package frp.exercises.exercise2_3

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.Done
import java.time.LocalDateTime
import scala.concurrent.duration.DurationInt
import scala.util.Random

object WeatherStation {

  sealed trait Command

  private case object GenerateMessage extends Command

  final case class Measurement(id: Int, temperature: Double, timestamp: LocalDateTime)

  def apply(nrMessages: Int = Int.MaxValue,
            messagesPerSecond: Int = 20,
            processingActor: ActorRef[Measurement]): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new WeatherStation(context, timers, nrMessages, messagesPerSecond, processingActor).idle()
      }
    }
}

class WeatherStation(context: ActorContext[WeatherStation.Command],
                     timers: TimerScheduler[WeatherStation.Command],
                     nrMessages: Int,
                     messagesPerSecond: Int,
                     processingActor: ActorRef[WeatherStation.Measurement]) {

  import WeatherStation._

  private def idle(): Behavior[Command] = Behaviors.receiveMessage {
    case GenerateMessage =>
      timers.startTimerAtFixedRate(GenerateMessage, 1.second / messagesPerSecond)
      active(1)
  }

  private def active(messageCount: Int): Behavior[Command] =
    Behaviors.receiveMessage {
      case GenerateMessage if messageCount <= nrMessages =>
        val measurement = Measurement(
          id = messageCount,
          temperature = Random.between(-20.0, 40.0),
          timestamp = LocalDateTime.now()
        )
        processingActor ! measurement
        active(messageCount + 1)

      case GenerateMessage =>
        Behaviors.stopped
    }
}
