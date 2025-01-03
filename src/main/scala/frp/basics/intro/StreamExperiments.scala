package frp.basics.intro

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl.{Flow, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}
import frp.basics.DefaultActorSystem
import frp.basics.LogUtil.traceWithThreadId
import frp.basics.PrimeUtil.isPrime

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

@main
def streamingExperiments(): Unit =

  given system: ActorSystem[Nothing] = DefaultActorSystem()

  given ExecutionContext = system.executionContext

  def computePrimes(): Unit =
    val source: Source[Int, NotUsed] = Source(2 to 200)
    val flow: Flow[Int, Int, NotUsed] = Flow[Int].filter(isPrime)
    val sink: Sink[Int, Future[Done]] = Sink.foreach(n => print(s"$n "))

    //var s: Source[Int, NotUsed] = source via flow
    //var f: Flow[Int, Int, NotUsed] = flow via flow
    //var s2: Sink[Int, NotUsed] = flow to sink
    //var g: RunnableGraph[NotUsed] = source to sink

    val graph: RunnableGraph[NotUsed] = source via flow to sink
    graph.run()

    Thread.sleep(100)
    println()
  end computePrimes

  def computePrimesMat(): Unit =
    val source: Source[Int, NotUsed] = Source(2 to 200)

    val flow: Flow[Int, Int, NotUsed] = Flow[Int].filter(isPrime)
    val sink: Sink[Int, Future[Done]] = Sink.foreach(n => print(s"$n "))

    //val graph: RunnableGraph[Future[Done]] = source.via(flow).toMat(sink)(Keep.right)
    //val done: Future[Done] = graph.run()

    val done = source.via(flow).runWith(sink)

    Await.ready(done, Duration.Inf)
    println()
  end computePrimesMat

  val itemSource = Source(1 to 10)
  val itemSink = Sink.foreach[Int](i => print(s"$i "))

  def produce(item: Int, time: FiniteDuration, trace: Boolean = false) =
    if (trace) traceWithThreadId(s"produce($item)")
    Thread.sleep(time.toMillis)
    item

  def consume(item: Int, time: FiniteDuration, trace: Boolean = false) =
    if (trace) traceWithThreadId(s"consume($item)")
    Thread.sleep(time.toMillis)
    item

  def sequentialStream(): Unit =
    val done: Future[Done] = itemSource
      .map(produce(_, 500.millis))
      .map(consume(_, 500.millis))
      .runWith(itemSink)

    Await.ready(done, Duration.Inf)
    println()
  end sequentialStream

  def asyncBoundaryStream(): Unit =
    val done: Future[Done] = itemSource
      .map(produce(_, 500.millis))
      .async
      .map(consume(_, 500.millis))
      .runWith(itemSink)

    Await.ready(done, Duration.Inf)
    println()
  end asyncBoundaryStream

  def asyncBoundaryStreamTraced(): Unit =
    val done: Future[Done] = itemSource
      .map(produce(_, 500.millis, trace = true))
      .async
      .map(consume(_, 500.millis, trace = true))
      .runWith(Sink.ignore)

    Await.ready(done, Duration.Inf)
    println()
  end asyncBoundaryStreamTraced

  def mapAsyncStream(): Unit =
    val done: Future[Done] = itemSource
      .mapAsync(3)(item => Future {
        produce(item, 500.millis)
      })
      .mapAsync(3)(item => Future {
        consume(item, 500.millis)
      })
      .runWith(itemSink)

    Await.ready(done, Duration.Inf)
    println()
  end mapAsyncStream

  def mapAsyncStreamTraced(): Unit =
    val done: Future[Done] = itemSource
      .mapAsync(3)(item => Future {
        produce(item, 500.millis, trace = true)
      })
      .mapAsync(3)(item => Future {
        consume(item, 500.millis, trace = true)
      })
      .runWith(Sink.ignore)

    Await.ready(done, Duration.Inf)
    println()
  end mapAsyncStreamTraced

  def computePrimesMapAsync1(): Unit =
    val done = Source(2 to 200)
      .mapAsync(3)(n => Future {
        (n, isPrime(n))
      })
      .filter { case (_, isPrime) => isPrime }
      .map { case (n, _) => n }
      .runForeach(n => print(s"$n "))

    Await.ready(done, Duration.Inf)
    println()
  end computePrimesMapAsync1

  def computePrimesMapAsync2(): Unit =
    val done = Source(2 to 200)
      .mapAsync(3)(n => Future {
        (n, isPrime(n))
      })
      .collect({ case (n, true) => n })
      .runForeach(n => print(s"$n "))

    Await.ready(done, Duration.Inf)
    println()
  end computePrimesMapAsync2

  println("================ computePrimes ===============")
  computePrimes()

  println("============== computePrimesMat ==============")
  computePrimesMat()

  println("============= sequentialStream ===============")
  sequentialStream()

  println("============ asyncBoundaryStream =============")
  asyncBoundaryStream()

  println("========= asyncBoundaryStreamTraced ==========")
  asyncBoundaryStreamTraced()

  println("============== mapAsyncStream ================")
  mapAsyncStream()

  println("=========== mapAsyncStreamTraced =============")
  mapAsyncStreamTraced()

  println("========== computePrimesMapAsync1 =============")
  computePrimesMapAsync1()

  println("========== computePrimesMapAsync2 =============")
  computePrimesMapAsync2()

  Await.ready(DefaultActorSystem.terminate(), Duration.Inf)

end streamingExperiments
