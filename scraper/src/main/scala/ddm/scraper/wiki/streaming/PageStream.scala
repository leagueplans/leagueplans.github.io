package ddm.scraper.wiki.streaming

import ddm.scraper.wiki.model.{Page, PageDescriptor}
import zio.{Chunk, RIO, Trace, URIO, ZIO}
import zio.http.Request
import zio.stream.ZStream

import scala.util.Try

type PageStream[+Out] = PageStream.R[Any, Out]

object PageStream {
  type Error = (PageDescriptor | Request, Throwable)
  type R[-R, +Out] = ZStream[R, Nothing, Either[Error, Page[Out]]]
}

extension [R, In] (self: PageStream.R[R, In]) {
  def pageExtend(using Trace): PageStream.R[R, (PageDescriptor, In)] =
    self.map {
      case Right((page, in)) => Right((page, (page, in)))
      case Left(error) => Left(error)
    }
  
  def pageMap[Out](f: In => Out)(using Trace): PageStream.R[R, Out] =
    self.map {
      case Right((page, in)) => Right((page, f(in)))
      case Left(error) => Left(error)
    }
    
  def pageMapEither[R1 <: R, Out](f: In => Either[Throwable, Out])(using Trace): PageStream.R[R1, Out] =
    pageMapZIO(in => ZIO.fromEither(f(in)))
    
  def pageMapTry[R1 <: R, Out](f: In => Try[Out])(using Trace): PageStream.R[R1, Out] =
    pageMapZIO(in => ZIO.fromTry(f(in)))

  def pageMapZIO[R1 <: R, Out](f: In => RIO[R1, Out])(using Trace): PageStream.R[R1, Out] =
    self.mapZIO {
      case Right((page, in)) =>
        withPageAnnotation(page)(f(in)).either.map {
          case Right(out) => Right((page, out))
          case Left(error) => Left((page, error))
        }
      case Left(error) =>
        ZIO.succeed(Left(error))
    }

  def pageMapZIOPar[R1 <: R, Out](
    n: => Int,
    bufferSize: => Int = 16
  )(f: In => RIO[R1, Out])(using Trace): PageStream.R[R1, Out] =
    self.mapZIOPar(n, bufferSize) {
      case Right((page, in)) =>
        withPageAnnotation(page)(f(in)).either.map {
          case Right(out) => Right((page, out))
          case Left(error) => Left((page, error))
        }
      case Left(error) =>
        ZIO.succeed(Left(error))
    }
    
  def pageFlatMap[R1 <: R, Out](f: In => PageStream.R[R1, Out])(using Trace): PageStream.R[R1, Out] =
    self.flatMap {
      case Right((page, in)) =>
        withPageStreamAnnotation(page)(f(in))
      case Left(error) =>
        ZStream.succeed(Left(error))
    }
    
  def pageFlatMapPar[R1 <: R, Out](
    n: => Int, 
    bufferSize: => Int = 16
  )(f: In => PageStream.R[R1, Out])(using Trace): PageStream.R[R1, Out] =
    self.flatMapPar(n, bufferSize) {
      case Right((page, in)) =>
        withPageStreamAnnotation(page)(f(in))
      case Left(error) =>
        ZStream.succeed(Left(error))
    }

  def pageFlattenIterables[Out](using In <:< Iterable[Out], Trace): PageStream.R[R, Out] =
    self.map {
      case Right((page, in)) => in.map(out => Right(page, out))
      case Left(error) => List(Left(error))
    }.flattenIterables
    
  def pageFilter(f: In => Boolean)(using Trace): PageStream.R[R, In] =
    self.filter {
      case Right((_, in)) => f(in) 
      case Left(_) => true
    }
    
  def pageCollect[Out](f: PartialFunction[In, Out])(using Trace): PageStream.R[R, Out] =
    self.collect(Function.unlift {
      case Right((page, in)) => f.lift(in).map(out => Right((page, out)))
      case Left(error) => Some(Left(error))
    })
    
  def pageRun(using Trace): URIO[R, (Chunk[PageStream.Error], Chunk[Page[In]])] = 
    self.runFold((Chunk.empty[PageStream.Error], Chunk.empty[Page[In]])) {
      case ((errorsAcc, outAcc), Right(out)) => (errorsAcc, outAcc :+ out)
      case ((errorsAcc, outAcc), Left(error)) => (errorsAcc :+ error, outAcc)
    }
}
