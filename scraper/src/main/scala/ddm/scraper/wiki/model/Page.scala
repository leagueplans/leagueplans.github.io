package ddm.scraper.wiki.model

import io.circe.{Decoder, Encoder}

object Page {
  object ID {
    given Decoder[ID] = Decoder[Int].map(ID.apply)
    given Encoder[ID] = Encoder[Int].contramap(_.raw)
    given Ordering[ID] = Ordering[Int].on(_.raw)
  }

  final case class ID(raw: Int) extends AnyVal {
    override def toString: String =
      raw.toString
  }

  enum Name(val wikiName: String) {
    case Category(raw: String) extends Name(s"Category:$raw")
    case File(raw: String, extension: String) extends Name(s"File:$raw.$extension")
    case Template(raw: String) extends Name(s"Template:$raw")
    case Other(raw: String) extends Name(raw)
  }

  object Name {
    def from(wikiName: String): Name =
      wikiName match {
        case s"Category:$raw" => Category(raw)
        case s"Template:$raw" => Template(raw)
        case s"File:$full" =>
          val Array(name, extension) = full.split("(\\.)(?!.*\\.)", /* limit = */ 2)
          File(name, extension)
        case _ => Other(wikiName)
      }

    given Decoder[Name] = Decoder[String].map(from)
  }
}

final case class Page(id: Page.ID, name: Page.Name)
