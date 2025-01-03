package frp.exercises.exercise2_2

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Random}

object PrimeChecker:

  sealed trait Command

  final case class CheckIfPrime(number: Int, replyTo: ActorRef[Reply]) extends Command

  private case class SendReply(reply: Reply, replyTo: ActorRef[Reply]) extends Command

  sealed trait Reply

  final case class PrimeResult(number: Int, factors: List[Int], isPrime: Boolean) extends Reply

  final case class FactorizationFailure(number: Int, reason: String) extends Reply

  private def factor(n: Int)(implicit ec: ExecutionContext): Future[Seq[Int]] = Future {
    val delay = Random.nextInt(100)
    Thread.sleep(delay)
    (2 to n / 2).filter(n % _ == 0)
  }

  def apply(): Behavior[Command] =
    Behaviors.receive { (context, command) =>
      implicit val ec: ExecutionContextExecutor = context.executionContext

      command match
        case CheckIfPrime(number, replyTo) =>
          context.pipeToSelf(factor(number)) {
            case Success(factors) =>
              SendReply(PrimeResult(number, factors.toList, factors.isEmpty), replyTo)
            case Failure(exception) =>
              SendReply(FactorizationFailure(number, exception.getMessage), replyTo)
          }
          Behaviors.same

        case SendReply(reply, replyTo) =>
          replyTo ! reply
          Behaviors.same
    }
end PrimeChecker