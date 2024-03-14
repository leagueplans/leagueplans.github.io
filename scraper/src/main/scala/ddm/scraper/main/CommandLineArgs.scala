package ddm.scraper.main

import scala.util.chaining.scalaUtilChainingOps

object CommandLineArgs {
  def parse(args: Seq[String]): CommandLineArgs =
    args
      .map(_.span(_ != '='))
      .map {
        case (key, s"=$value") => key -> value
        case other => throw IllegalArgumentException(
          s"Unexpected argument format: [$other]\n" +
            "Arguments should contain a '=' separating a key-value pair."
        )
      }
      .toMap
      .pipe(CommandLineArgs(_))
}

final class CommandLineArgs(args: Map[String, String]) {
  def get[T](arg: String)(decode: PartialFunction[String, T]): T =
    getOpt[T](arg)(decode)
      .getOrElse(throw IllegalArgumentException(s"No value set for key [$arg]"))

  def getOpt[T](arg: String)(decode: PartialFunction[String, T]): Option[T] =
    args
      .get(arg)
      .map(value =>
        decode
          .lift(value)
          .getOrElse(throw IllegalArgumentException(s"Unexpected value for key [$arg]"))
      )
}
