package frp.exercises.exercise2_4

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.Flow
import frp.exercises.Common.{DefaultActorSystem, Measurement}
import frp.exercises.Common.MeasureUtil.measure
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationInt}

@main def WeatherStationStreamApp(): Unit = {
  println("==================== WeatherStation with Akka Streams ==========================")

  val NR_MESSAGES = 200
  val MESSAGES_PER_SECOND = 100
  val PARALLELISM = Runtime.getRuntime.availableProcessors
  val BULK_SIZE = 10
  val TIME_WINDOW = 200.millis

  val actorSystem: ActorSystem[Nothing] = DefaultActorSystem()

  given ExecutionContext = actorSystem.executionContext

  def measurementsServiceWithStream(parallelism: Int = PARALLELISM): Flow[String, String, NotUsed] = {
    Flow[String]
      .mapAsync(parallelism) { json =>
        Future {
          Measurement.fromJson(json)
        }
      }
      .collect { case Right(meas) => meas }
      .map(meas => {
        s"Acknowledged: ${meas.id}"
      })
  }

  val weatherStation = WeatherStationSimulator(NR_MESSAGES, MESSAGES_PER_SECOND).withTracing()

  val done = measure(weatherStation.handleMessages(measurementsServiceWithStream())) { time =>
    println(s"Throughput of MeasurementService: ${NR_MESSAGES / time} msg/s")
  }

  Await.ready(done, Duration.Inf)

  Await.ready(DefaultActorSystem.terminate(), Duration.Inf)

  println("============================== System Terminated ===============================")
}
