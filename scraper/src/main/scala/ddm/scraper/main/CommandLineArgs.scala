package ddm.scraper.main

import zio.{RIO, Trace, ZIO, ZIOAppArgs}

import scala.util.{Failure, Success, Try}

object CommandLineArgs {
  def parse(using Trace): RIO[ZIOAppArgs, CommandLineArgs] =
    ZIOAppArgs.getArgs.flatMap(args =>
      ZIO.foreach(args.map(_.span(_ != '='))) {
        case (key, s"=$value") => ZIO.succeed(key -> value)
        case other => ZIO.fail(IllegalArgumentException(
          s"Unexpected argument format: [$other]\n" +
            "Arguments should contain a '=' separating a key-value pair."
        ))
      }
    ).map(pairs => CommandLineArgs(pairs.toMap))
}

final class CommandLineArgs(args: Map[String, String]) {
  def get[T](arg: String)(decode: String => Try[T]): Try[T] =
    getOpt[T](arg)(decode).flatMap {
      case Some(value) => Success(value)
      case None => Failure(IllegalArgumentException(s"No value set for key [$arg]"))
    }
  
  def getOpt[T](arg: String)(decode: String => Try[T]): Try[Option[T]] =
    args.get(arg) match {
      case Some(encoded) => 
        decode(encoded) match {
          case Success(value) => Success(Some(value))
          case Failure(cause) => Failure(IllegalArgumentException(s"Unexpected value for key [$arg]", cause))
        }

      case None =>
        Success(None)
    }
}
