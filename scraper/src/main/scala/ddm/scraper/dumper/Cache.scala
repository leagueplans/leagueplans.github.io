package ddm.scraper.dumper

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.util.Try

object Cache {
  sealed trait Message[+T]

  object Message {
    final case class NewEntry[T](entry: T) extends Message[T]
    final case class Complete(runStatus: Try[_]) extends Message[Nothing]
  }

  def init[T](onComplete: (Try[_], List[T]) => Unit): Behavior[Message[T]] =
    behavior[T](onComplete, acc = List.empty)

  private def behavior[T](
    onComplete: (Try[_], List[T]) => Unit,
    acc: List[T]
  ): Behavior[Message[T]] =
    Behaviors.receiveMessage {
      case Message.NewEntry(entry) =>
        behavior(onComplete, acc :+ entry)

      case Message.Complete(runStatus) =>
        onComplete(runStatus, acc)
        Behaviors.stopped
    }
}
