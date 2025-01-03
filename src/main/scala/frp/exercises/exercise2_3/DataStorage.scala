package frp.exercises.exercise2_3

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import frp.exercises.Common.{Acknowledgement, Measurement, Repository}
import slick.jdbc.H2Profile.api.*

import scala.collection.mutable
import scala.concurrent.duration.*
import scala.util.Success

object DataStorage {
  private val BULK_SIZE = 5
  private val TIME_WINDOW = 200.millis
  private val PARALLELISM = 4

  sealed trait Command

  final case class Insert(measurement: Measurement, replyTo: ActorRef[Acknowledgement]) extends Command

  def apply(bulkSize: Int = BULK_SIZE,
            timeWindow: FiniteDuration = TIME_WINDOW,
            parallelism: Int = PARALLELISM): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new DataStorage(context, timers, bulkSize, timeWindow, parallelism)
      }
    }
}

class DataStorage(context: ActorContext[DataStorage.Command],
                  timer: TimerScheduler[DataStorage.Command],
                  bulkSize: Int, timeWindow: FiniteDuration, parallelism: Int)
  extends AbstractBehavior[DataStorage.Command](context) {

  import DataStorage._

  private case object Timeout extends Command

  private case class SendAcks(m: Seq[Measurement]) extends Command

  private val repository = Repository().withTracing()
  private var measurements = Seq.empty[Measurement]
  private var outstanding = 0
  private val sender = mutable.Map.empty[Measurement, ActorRef[Acknowledgement]]

  timer.startTimerAtFixedRate(Timeout, timeWindow)

  private def tryBulkInsert(): Unit =
    if measurements.nonEmpty && outstanding < parallelism then
      outstanding += 1
      val (bulk, remaining) = measurements.splitAt(bulkSize)
      measurements = remaining

      context.pipeToSelf[Seq[Measurement]](repository.bulkInsertAsync(bulk)) {
        case Success(measurements) => SendAcks(measurements)
      }
    end if
  end tryBulkInsert

  override def onMessage(msg: Command): Behavior[Command] = msg match
    case Insert(m, replyTo) =>
      measurements = measurements :+ m
      sender(m) = replyTo
      if measurements.size >= bulkSize then
        tryBulkInsert()
      Behaviors.same

    case Timeout =>
      tryBulkInsert()
      Behaviors.same

    case SendAcks(mess) =>
      mess.foreach(m => sender(m) ! m.ack())
      outstanding -= 1
      if measurements.size >= bulkSize then
        tryBulkInsert()
      Behaviors.same
  end onMessage
}
