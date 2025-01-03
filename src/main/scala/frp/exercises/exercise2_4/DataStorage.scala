package frp.exercises.exercise2_4

import akka.stream.scaladsl.Flow
import akka.actor.typed.ActorRef
import akka.NotUsed
import frp.exercises.Common.{Acknowledgement, Measurement, Repository}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.*

given ExecutionContext = scala.concurrent.ExecutionContext.global

object DataStorage {

  private val BULK_SIZE = 5
  private val TIME_WINDOW = 200.millis
  private val PARALLELISM = 4

  def createFlow(bulkSize: Int = BULK_SIZE,
                 timeWindow: FiniteDuration = TIME_WINDOW,
                 parallelism: Int = PARALLELISM): Flow[(Measurement, ActorRef[Acknowledgement]), Acknowledgement, NotUsed] = {

    Flow[(Measurement, ActorRef[Acknowledgement])]
      .throttle(1, timeWindow)
      .groupBy(parallelism, _ => 0)
      .mapAsyncUnordered(parallelism) { case (m, replyTo) =>
        val repository = Repository().withTracing()
        var measurements = Seq(m)

        def tryBulkInsert(): Future[Seq[Measurement]] = {
          if (measurements.size >= bulkSize) {
            val (bulk, remaining) = measurements.splitAt(bulkSize)
            measurements = remaining
            repository.bulkInsertAsync(bulk).map { _ =>
              bulk
            }
          } else {
            Future.successful(Seq())
          }
        }

        tryBulkInsert().map { measurements =>
          measurements.foreach(m => replyTo ! Acknowledgement(m.id))
          Acknowledgement(m.id)
        }
      }
      .mergeSubstreams
  }
}
