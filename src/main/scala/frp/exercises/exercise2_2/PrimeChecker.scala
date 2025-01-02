package frp.exercises.exercise2_2

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object PrimeChecker:

  sealed trait Command

  final case class CheckIfPrime(number: Int, replyTo: ActorRef[Reply]) extends Command

  private case class SendReply(reply: Reply, replyTo: ActorRef[Reply]) extends Command

  sealed trait Reply

  final case class PrimeResult(number: Int, factors: List[Int], isPrime: Boolean) extends Reply

  final case class FactorizationFailure(number: Int, reason: String) extends Reply

  private def factor(n: Int)(implicit ec: scala.concurrent.ExecutionContext): Future[Seq[Int]] = Future {
    (2 to n / 2).filter(n % _ == 0)
  }

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val ec: ExecutionContextExecutor = context.executionContext

      Behaviors.receiveMessage {
        case CheckIfPrime(number, replyTo) =>
          context.pipeToSelf(factor(number)) {
            case Success(factors) =>
              SendReply(PrimeResult(number, factors.toList, isPrime = factors.isEmpty), replyTo)
            case Failure(exception) =>
              SendReply(FactorizationFailure(number, exception.getMessage), replyTo)
          }
          Behaviors.same

        case SendReply(reply, replyTo) =>
          replyTo ! reply
          Behaviors.same
      }
    }
end PrimeChecker
