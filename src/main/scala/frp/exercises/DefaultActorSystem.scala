package frp.exercises

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

import scala.concurrent.Future

object DefaultActorSystem:
  private final var actorSystem: ActorSystem[Nothing] = null

  def apply(): ActorSystem[Nothing] =
    if actorSystem == null then
      actorSystem = akka.actor.typed.ActorSystem(Behaviors.ignore, "default-system")
    actorSystem

  def apply[T](behavior: Behavior[T]): ActorSystem[T] =
    if actorSystem == null then
      actorSystem = akka.actor.typed.ActorSystem(behavior, "default-system")
    actorSystem.asInstanceOf[ActorSystem[T]]

  def terminate(): Future[Done] =
    if actorSystem != null then
      actorSystem.terminate()
      actorSystem.whenTerminated
    else
      Future.successful(Done)

end DefaultActorSystem
