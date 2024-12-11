package frp.basics.actors.advanced

import scala.concurrent.duration.DurationInt
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import frp.basics.actors.RangeUtil
import frp.basics.actors.advanced.PrimeCalculator.{Result, WrappedPrimeFinderReply}
import frp.basics.actors.advanced.PrimeFinder.PartialResult

import scala.collection.mutable

object PrimeCalculator:
  sealed trait Command

  private object Resend extends Command

  private final case class WrappedPrimeFinderReply(reply: PrimeFinder.Reply) extends Command

  sealed trait Reply

  final case class Result(lower: Int, upper: Int, primes: Seq[Int]) extends Reply

  final case class Failed(lower: Int, upper: Int, reason: String) extends Reply

  def apply(lower: Int, upper: Int, replyTo: ActorRef[Reply],
            workerPool: ActorRef[PrimeFinder.Command]): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.withTimers {
        timer => new PrimeCalculator(context, lower, upper, replyTo, workerPool, timer)
      }
    }
  end apply
end PrimeCalculator

class PrimeCalculator(context: ActorContext[PrimeCalculator.Command],
                      lower: Int, upper: Int, replyTo: ActorRef[PrimeCalculator.Reply],
                      workerPool: ActorRef[PrimeFinder.Command],
                      timer: TimerScheduler[PrimeCalculator.Command])
  extends AbstractBehavior[PrimeCalculator.Command](context):

  private val MAX_WORKERS: Int = Runtime.getRuntime.availableProcessors

  private val RESEND_INTERVAL = 40.millis

  private val messageAdapter: ActorRef[PrimeFinder.Reply] = context.messageAdapter(reply => WrappedPrimeFinderReply(reply))

  private val unfinishedTasks = mutable.Set.empty[(Int, Int)]

  val primes: mutable.SortedSet[Int] = mutable.SortedSet.empty[Int]

  createTasks()
  sendUnfinishedTasks()

  timer.startTimerAtFixedRate(PrimeCalculator.Resend, RESEND_INTERVAL)

  private def createTasks(): Unit =
    var intervals = RangeUtil.splitIntoIntervals(lower, upper, MAX_WORKERS)
    for ((lower, upper) <- RangeUtil.splitIntoIntervals(lower, upper, MAX_WORKERS))
      unfinishedTasks += ((lower, upper))

  private def sendUnfinishedTasks(): Unit =
    for ((lower, upper) <- unfinishedTasks)
      workerPool ! PrimeFinder.Find(lower, upper, messageAdapter)

  override def onMessage(msg: PrimeCalculator.Command): Behavior[PrimeCalculator.Command] = msg match
    case WrappedPrimeFinderReply(PartialResult(lower, upper, primes)) =>
      unfinishedTasks -= ((lower, upper))
      this.primes ++= primes
      if unfinishedTasks.isEmpty then
        replyTo ! Result(lower, upper, primes)
        Behaviors.stopped
      else
        Behaviors.same
    case PrimeCalculator.Resend =>
      sendUnfinishedTasks()
      Behaviors.same

end PrimeCalculator
