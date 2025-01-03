package frp.basics.iot

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.mutable
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Success

object DatabaseActor:
  private val BULK_SIZE = 5
  private val TIME_WINDOW = 200.millis
  private val PARALLELISM = 4

  sealed trait Command
  final case class Insert(item: Measurement, replyTo: ActorRef[Acknowledgement]) extends Command

  def apply(bulkSize: Int = BULK_SIZE,
            timeWindow: FiniteDuration = TIME_WINDOW,
            parallelism: Int = PARALLELISM): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.withTimers { (timer: TimerScheduler[Command]) =>
        new DatabaseActor(context, timer, bulkSize, timeWindow, parallelism)
      }
    }
end DatabaseActor

class DatabaseActor(context: ActorContext[DatabaseActor.Command],
                    timer: TimerScheduler[DatabaseActor.Command],
                    bulkSize: Int, timeWindow: FiniteDuration, parallelism: Int)
  extends AbstractBehavior[DatabaseActor.Command](context):

  import DatabaseActor.{Command, Insert}

  private case object Timeout extends Command
  private case class SendAcks(m: Seq[Measurement]) extends Command

  private val repository   = Repository().withTracing()
  private var measurements = Seq.empty[Measurement]
  private var outstanding  = 0
  private val sender = mutable.Map.empty[Measurement, ActorRef[Acknowledgement]]

  timer.startTimerAtFixedRate(Timeout, timeWindow)

  private def tryBulkInsert() =
    if measurements.nonEmpty && outstanding < parallelism then
      outstanding += 1
      val (bulk, remaining) = measurements.splitAt(bulkSize)
      measurements = remaining

      context.pipeToSelf[Seq[Measurement]](repository.bulkInsertAsync(bulk)) {
        case Success(messages) => SendAcks(messages)
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

end DatabaseActor