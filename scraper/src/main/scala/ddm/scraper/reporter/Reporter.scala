package ddm.scraper.reporter

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Flow
import ddm.scraper.dumper.Cache
import ddm.scraper.wiki.model.Page

object Reporter {
  def flow[T](reporter: ActorRef[Cache.Message.NewEntry[(Page, Throwable)]]): Flow[Either[(Page, Throwable), T], T, _] =
    Flow[Either[(Page, Throwable), T]].mapConcat {
      case Left((page, error)) =>
        reporter ! Cache.Message.NewEntry((page, error))
        Nil

      case Right(t) =>
        List(t)
    }

  def pageFlow[T](reporter: ActorRef[Cache.Message.NewEntry[(Page, Throwable)]]): Flow[(Page, Either[Throwable, T]), (Page, T), _] =
    Flow[(Page, Either[Throwable, T])].mapConcat {
      case (page, Left(error)) =>
        reporter ! Cache.Message.NewEntry((page, error))
        Nil

      case (page, Right(t)) =>
        List((page, t))
    }
}
