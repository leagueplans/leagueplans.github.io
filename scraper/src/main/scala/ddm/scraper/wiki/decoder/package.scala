package ddm.scraper.wiki

import ddm.scraper.wiki.parser.Term
import ddm.scraper.wiki.parser.Term._

import scala.annotation.tailrec
import scala.reflect.ClassTag

package object decoder {
  type DecoderResult[T] = Either[DecoderException, T]

  implicit final class RichTemplateObject(val self: Template.Object) extends AnyVal {
    def decode[T](name: String)(f: List[Term] => DecoderResult[T]): DecoderResult[T] =
      decodeOpt[T](name)(f).flatMap(
        _.toRight(left = new DecoderException(s"Parameter [$name] not defined"))
      )

    def decodeOpt[T](name: String)(f: List[Term] => DecoderResult[T]): DecoderResult[Option[T]] =
      self.namedParams.get(name) match {
        case None => Right(None)
        case Some(terms) =>
          f(terms) match {
            case Right(t) => Right(Some(t))
            case Left(error) => Left(new DecoderException(s"Decoding [$name] failed: $error"))
          }
      }
  }

  implicit final class RichTerms(val self: List[Term]) extends AnyVal {
    def as[T <: Term : ClassTag]: DecoderResult[T] =
      asOpt[T].flatMap(
        _.toRight(left = new DecoderException("No terms defined"))
      )

    def asOpt[T <: Term : ClassTag]: DecoderResult[Option[T]] =
      self match {
        case Nil => Right(None)
        case (t: T) :: Nil => Right(Some(t))
        case _ :: Nil => Left(new DecoderException("Unexpected term type"))
        case _ => Left(new DecoderException("More than one term found"))
      }

    def asBoolean: DecoderResult[Boolean] =
      as[Unstructured].flatMap(_.asBoolean)

    /** Transforms terms, and ensures that any consecutive blobs after
      * filtering are then combined into a single blob */
    def collapse(transform: Structured => Option[Term]): List[Term] =
      collapseHelper(
        transform,
        focus = None,
        remaining = self,
        acc = List.empty
      )

    @tailrec
    private def collapseHelper(
      transform: Structured => Option[Term],
      focus: Option[Unstructured],
      remaining: List[Term],
      acc: List[Term],
    ): List[Term] =
      remaining match {
        case Nil =>
          acc ++ focus

        case (blob: Unstructured) :: tail =>
          // Concatenating blobs with a space might not always be the right choice
          val simplifiedBlob = focus match {
            case Some(blob0) => Unstructured(s"${blob0.raw} ${blob.raw}")
            case None => blob
          }

          collapseHelper(
            transform,
            focus = Some(simplifiedBlob),
            remaining = tail,
            acc = acc
          )

        case (term: Structured) :: tail =>
          transform(term) match {
            case Some(blob: Unstructured) =>
              collapseHelper(
                transform,
                focus = focus,
                remaining = blob +: tail,
                acc = acc
              )

            case Some(newTerm: Structured) =>
              collapseHelper(
                transform,
                focus = None,
                remaining = tail,
                acc = acc ++ focus :+ newTerm
              )

            case None =>
              collapseHelper(
                transform,
                focus = focus,
                remaining = tail,
                acc = acc
              )
          }
      }
  }

  implicit final class RichUnstructured(val self: Unstructured) extends AnyVal {
    def asBoolean: DecoderResult[Boolean] =
      self.raw.toLowerCase match {
        case "yes" => Right(true)
        case "no" => Right(false)
        case other => Left(new DecoderException(s"Expected boolean but found [$other]"))
      }
  }
}
