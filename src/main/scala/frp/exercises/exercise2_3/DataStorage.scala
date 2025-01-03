package frp.exercises.exercise2_3

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Materializer, SystemMaterializer}
import slick.jdbc.H2Profile.api.*

import scala.collection.mutable
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object DataStorage {

  sealed trait Command

  final case class InsertMeasurement(measurement: Measurement, replyTo: ActorRef[Acknowledgement]) extends Command

  private case object Timeout extends Command

  private case class Acknowledged(measurement: Measurement) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers { timers =>
      new DataStorage(context, timers)
    }
  }
}

class DataStorage(context: ActorContext[DataStorage.Command], timers: TimerScheduler[DataStorage.Command])
  extends AbstractBehavior[DataStorage.Command](context) {

  import DataStorage._

  private implicit val ec: ExecutionContext = context.executionContext
  private implicit val mat: Materializer = SystemMaterializer(context.system).materializer

  private val db = Database.forConfig("slick-h2")
  private val measurements = TableQuery[Measurements]

  private val buffer = mutable.Queue[(Measurement, ActorRef[Acknowledgement])]()
  private val bulkSize = 5
  private val timeWindow = 200.millis
  private var isPersisting = false

  timers.startTimerAtFixedRate(Timeout, timeWindow)

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case InsertMeasurement(measurement, replyTo) =>
        buffer.enqueue((measurement, replyTo))
        if (buffer.size >= bulkSize && !isPersisting) {
          persistMeasurements()
        }
        Behaviors.same

      case Timeout =>
        if (buffer.nonEmpty && !isPersisting) {
          persistMeasurements()
        }
        Behaviors.same

      case Acknowledged(measurement) =>
        buffer.dequeueFirst(_._1 == measurement)
        isPersisting = false
        Behaviors.same
    }
  }

  private def persistMeasurements(): Unit = {
    isPersisting = true
    val measurementsToPersist = buffer.take(bulkSize)
    val insertAction = DBIO.seq(measurementsToPersist.toSeq.map(_._1).map(m => measurements += m) *)

    val future = db.run(insertAction)
    future.onComplete {
      case Success(_) =>
        measurementsToPersist.foreach { case (measurement, replyTo) => replyTo ! Acknowledgement(measurement.id) }
        measurementsToPersist.map(_._1).foreach(m => context.self ! Acknowledged(m))
      case Failure(exception) =>
        context.log.error("Failed to persist measurements", exception)
    }
  }
}
