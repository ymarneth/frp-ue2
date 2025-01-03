package frp.exercises.exercise2_3

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors, TimerScheduler}
import akka.stream.alpakka.slick.scaladsl.SlickSession
import akka.stream.{Materializer, SystemMaterializer}
import slick.jdbc.H2Profile.api.*
import scala.collection.immutable.Queue
import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object DataStorage {

  sealed trait Command

  final case class InsertMeasurement(measurement: Measurement, replyTo: ActorRef[Reply]) extends Command

  private final case class InsertedMeasurements(ids: Seq[Int]) extends Command

  private case object FlushBuffer extends Command

  sealed trait Reply

  final case class Acknowledged(id: Int) extends Reply

  private final case class FailureAcknowledged(id: Int, reason: String) extends Reply

  def apply(bulkSize: Int = 5,
            timeWindow: FiniteDuration = 200.millis,
            maxBufferSize: Int = 100
           ): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new DataStorage(context, timers, bulkSize, timeWindow, maxBufferSize)
      }
    }
}

class DataStorage(context: ActorContext[DataStorage.Command],
                  timers: TimerScheduler[DataStorage.Command],
                  bulkSize: Int,
                  timeWindow: FiniteDuration,
                  maxBufferSize: Int
                 ) extends AbstractBehavior[DataStorage.Command](context) {

  import DataStorage._

  private implicit val ec: ExecutionContext = context.executionContext
  private implicit val mat: Materializer = SystemMaterializer(context.system).materializer

  private val db = Database.forConfig("slick-h2.db")
  private val slickSession: SlickSession = SlickSession.forDbAndProfile(db, slick.jdbc.H2Profile)

  private val measurements = TableQuery[Measurements]

  private var buffer: Queue[(Measurement, ActorRef[Reply])] = Queue.empty
  private var isPersisting = false

  timers.startTimerAtFixedRate(FlushBuffer, timeWindow)

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case InsertMeasurement(measurement, replyTo) =>
        handleInsertMeasurement(measurement, replyTo)
        this

      case FlushBuffer =>
        handleFlushBuffer()
        this

      case InsertedMeasurements(ids) =>
        handleInsertedMeasurements(ids)
        this
    }
  }

  private def handleInsertMeasurement(measurement: Measurement, replyTo: ActorRef[Reply]): Unit = {
    if (buffer.size >= maxBufferSize) {
      context.log.error("Buffer overflow! Dropping measurement with ID: {}", measurement.id)
      replyTo ! FailureAcknowledged(measurement.id, "Buffer overflow")
    } else {
      buffer = buffer.enqueue((measurement, replyTo))
      if (buffer.size >= bulkSize && !isPersisting) {
        persistMeasurements()
      }
    }
  }

  private def handleFlushBuffer(): Unit = {
    if (buffer.nonEmpty && !isPersisting) {
      persistMeasurements()
    }
  }

  private def handleInsertedMeasurements(ids: Seq[Int]): Unit = {
    buffer = buffer.filterNot { case (measurement, _) => ids.contains(measurement.id) }
    isPersisting = false
  }

  private def persistMeasurements(): Unit = {
    isPersisting = true

    val (measurementsToPersist, remainingBuffer) = buffer.splitAt(bulkSize)
    val createTableAction = measurements.schema.createIfNotExists
    val insertAction = DBIO.seq(measurementsToPersist.map(_._1).map(m => measurements += m) *)
    val action = createTableAction.andThen(insertAction)

    val future = db.run(action)
    val self = context.self
    val log = context.log

    future.onComplete {
      case Success(_) =>
        val ids = measurementsToPersist.map(_._1.id)
        self ! InsertedMeasurements(ids)
        measurementsToPersist.foreach { case (measurement, replyTo) =>
          replyTo ! Acknowledged(measurement.id)
        }
      case Failure(exception) =>
        log.error("Failed to persist measurements: {}", exception.getMessage)
        measurementsToPersist.foreach { case (measurement, replyTo) =>
          replyTo ! FailureAcknowledged(measurement.id, exception.getMessage)
        }
        isPersisting = false // Ensure actor can retry
    }
  }
}
