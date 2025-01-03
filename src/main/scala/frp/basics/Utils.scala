package frp.basics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object LogUtil:

  def trace[T](expr: => T, tracingEnabled: Boolean = true): T =
    val result = expr
    if (tracingEnabled) println(s"$result")
    result

  def tracef[T](expr: => T, formatString: String = "%s", tracingEnabled: Boolean = true): Unit =
    val result = expr
    if (tracingEnabled) println(formatString.format(result))

  def traceWithThreadId[T](expr: => T, tracingEnabled: Boolean = true): Unit =
    val result = expr
    if (tracingEnabled) println(s"$result (thread=${Thread.currentThread.threadId})")

  def tracefWithThreadId[T](expr: => T, formatString: String = "%s", tracingEnabled: Boolean = true): Unit =
    val result = expr
    if (tracingEnabled)
      println((formatString + " (thread=%d)").format(result, Thread.currentThread.threadId))

end LogUtil

object PrimeUtil:
  def isPrime(n: Int): Boolean =
    val upper = math.sqrt(n + 0.5).toInt
    (2 to upper) forall (i => n % i != 0)
end PrimeUtil

object MeasureUtil:
  def measure[T](f: => Future[T])(processTiming: Double => Unit): Future[T] =
    val start = System.nanoTime()
    f andThen (value => {
      val end = System.nanoTime()
      processTiming((end - start)/1e9)
    })
end MeasureUtil
