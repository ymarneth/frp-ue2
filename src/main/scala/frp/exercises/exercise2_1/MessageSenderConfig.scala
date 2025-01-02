package frp.exercises.exercise2_1

import scala.concurrent.duration.FiniteDuration

case class MessageSenderConfig(maxRetries: Int, messageCount: Int, retryInterval: FiniteDuration)
