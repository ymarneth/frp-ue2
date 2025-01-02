package frp.exercises.exercise1

import scala.concurrent.duration.FiniteDuration

case class MessageSenderConfig(maxRetries: Int, messageCount: Int, retryInterval: FiniteDuration)
