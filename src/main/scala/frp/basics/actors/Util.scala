package frp.basics.actors

object PrimeUtil:
  def isPrime(n: Int): Boolean =
    val upper = math.sqrt(n + 0.5).toInt
    (2 to upper) forall (i => n % i != 0)
end PrimeUtil

object RangeUtil:

  def splitIntoIntervals(lower: Int, upper: Int, nrIntervals: Int): Seq[(Int, Int)] =
    assert(nrIntervals > 0, "nrIntervals must be positive")

    val length = upper - lower + 1
    val smallIntervalLength = length / nrIntervals
    val largeIntervalLength = smallIntervalLength + 1;
    val nrSmallIntervals = (largeIntervalLength - length % largeIntervalLength) % largeIntervalLength
    val nrLargeIntervals = nrIntervals - nrSmallIntervals

    val largeIntervals =
      for (i <- 1 to nrLargeIntervals)
        yield ((lower - 1) + largeIntervalLength * (i - 1) + 1, (lower - 1) + largeIntervalLength * i)

    val lbSmall = (lower - 1) + nrLargeIntervals * largeIntervalLength
    val smallIntervals =
      for (i <- 1 to nrSmallIntervals)
        yield (lbSmall + smallIntervalLength * (i - 1) + 1, lbSmall + smallIntervalLength * i)

    largeIntervals ++ smallIntervals
  end splitIntoIntervals

end RangeUtil
