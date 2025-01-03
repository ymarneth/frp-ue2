package frp.basics.iot

import akka.Done
import frp.basics.DefaultActorSystem

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.Success

object Repository:
  def apply(insertDuration: FiniteDuration = 100.millis) =
    new Repository(insertDuration)
end Repository

class Repository(private val insertDuration: FiniteDuration):

  private val system = DefaultActorSystem()
  private var tracingEnabled = false

  def totalBulkInsertDuration(blockSize: Int): FiniteDuration =
    val timePerRow =
      blockSize match
        case n if n<1    => 0.millis
        case n if n<=1   => insertDuration
        case n if n<10   => insertDuration + (insertDuration/10-insertDuration)/(10-1)*(n-1)
        case n if n<=100 => insertDuration/10
        case n           => insertDuration/10 + (insertDuration-insertDuration/10)/(1000-100)*(n-100)
    timePerRow * blockSize

  private def completeAfter(delay: FiniteDuration): Future[Done] =
    val promise = Promise[Done]()
    system.scheduler.scheduleOnce(delay, () => { promise.complete(Success(Done))})
    promise.future

  private def traceInsert[T<:Entity](item: T) =
    if (tracingEnabled) println(s"==> insertAsync(${item.id})")
    item

  private def traceBulkInsert[T<:Entity](items: Seq[T]) =
    if (tracingEnabled)
      println(s"==> bulkInsertAsync([${items.map(item => item.id).mkString(",")}])")
    items

  def withTracing(enabled: Boolean = true): Repository =
    tracingEnabled = enabled
    this

  def insertAsync[T<:Entity](item: T): Future[T] =
    completeAfter(insertDuration) map (_ => traceInsert(item))

  def bulkInsertAsync[T<:Entity](items: Seq[T]): Future[Seq[T]] =
    completeAfter(totalBulkInsertDuration(items.size)) map (_ => traceBulkInsert(items))

end Repository