package ddm.scraper.wiki.model

import io.circe.{Decoder, Encoder}

object Page {
  object ID {
    implicit val decoder: Decoder[ID] = Decoder[Int].map(ID.apply)
    implicit val encoder: Encoder[ID] = Encoder[Int].contramap(_.raw)
    implicit val ordering: Ordering[ID] = Ordering[Int].on(_.raw)
  }

  final case class ID(raw: Int) {
    override def toString: String =
      raw.toString
  }

  sealed trait Name {
    def wikiName: String
  }

  object Name {
    def from(wikiName: String): Name =
      wikiName match {
        case s"Category:$raw" => Category(raw)
        case s"File:$raw.$extension" => File(raw, extension)
        case s"Template:$raw" => Template(raw)
        case _ => Other(wikiName)
      }

    final case class Category(raw: String) extends Name {
      val wikiName: String = s"Category:$raw"
    }

    final case class File(raw: String, extension: String) extends Name {
      val wikiName: String = s"File:$raw.$extension"
    }

    final case class Template(raw: String) extends Name {
      val wikiName: String = s"Template:$raw"
    }

    final case class Other(wikiName: String) extends Name

    implicit val decoder: Decoder[Name] = Decoder[String].map(from)
  }
}

final case class Page(id: Page.ID, name: Page.Name)
