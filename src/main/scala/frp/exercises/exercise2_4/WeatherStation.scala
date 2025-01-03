package frp.exercises.exercise2_4

import akka.actor.typed.ActorSystem
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Flow, Sink, Source}
import frp.exercises.Common.{DefaultActorSystem, Measurement}
import frp.exercises.Common.LogUtil.tracef
import java.time.LocalDateTime
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

trait WeatherStation:
  def generateStreamSource: Source[String, NotUsed]
end WeatherStation

object WeatherStationSimulator:
  def apply(nrMeasurements: Int = Int.MaxValue, measurementsPerSecond: Int = 20, minTemp: Double = 0, maxTemp: Double = 100) =
    new WeatherStationSimulator(nrMeasurements, measurementsPerSecond, minTemp, maxTemp)
end WeatherStationSimulator

class WeatherStationSimulator(nrMeasurements: Int, measurementsPerSecond: Int, minTemp: Double, maxTemp: Double) extends WeatherStation:
  given ActorSystem[Nothing] = DefaultActorSystem()
  private var tracingEnabled = false

  def withTracing(enabled: Boolean = true): WeatherStationSimulator =
    tracingEnabled = enabled
    this

  override def generateStreamSource: Source[String, NotUsed] = {
    Source(1 to nrMeasurements)
      .throttle(measurementsPerSecond, 1.second)
      .map(id => Measurement(id, Random.nextDouble() * (maxTemp - minTemp) + minTemp, LocalDateTime.now))
      .map(meas => meas.toJson)
  }

  def handleMessages(service: Flow[String, String, NotUsed]): Future[Done] = {
    val source = generateStreamSource

    val sink = Sink.ignore

    val tapIn = Flow[String].wireTap(req => tracef(req, "--> %s", tracingEnabled))
    val tapOut = Flow[String].wireTap(res => tracef(res, "<-- %s", tracingEnabled))

    source.via(tapIn).via(service).via(tapOut).runWith(sink)
  }
end WeatherStationSimulator
