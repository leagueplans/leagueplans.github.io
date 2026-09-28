package com.leagueplans.scraper.wiki.model

import io.circe.{Decoder, Encoder}

object PageDescriptor {
  object ID {
    given Decoder[ID] = Decoder.decodeInt
    given Encoder[ID] = Encoder.encodeInt
    given Ordering[ID] = Ordering.Int
  }

  opaque type ID <: Int = Int

  enum Name(val wikiName: String) {
    case Category(raw: String) extends Name(s"Category:$raw")
    case File(raw: String, extension: String) extends Name(s"File:$raw.$extension")
    case Template(raw: String) extends Name(s"Template:$raw")
    case Other(raw: String) extends Name(raw)
  }

  object Name {
    // MediaWiki matches namespaces without regard to case, so the wiki happily renders a
    // link written as [[FIle:…]].
    def from(wikiName: String): Name =
      wikiName.split(":", /* limit = */ 2) match {
        case Array(namespace, raw) if namespace.equalsIgnoreCase("Category") => Category(raw)
        case Array(namespace, raw) if namespace.equalsIgnoreCase("Template") => Template(raw)
        case Array(namespace, full) if namespace.equalsIgnoreCase("File") =>
          val Array(name, extension) = full.split("(\\.)(?!.*\\.)", /* limit = */ 2)
          File(name, extension)
        case _ => Other(wikiName)
      }

    given Decoder[Name] = Decoder[String].map(from)
    given Ordering[Name] = Ordering.by(_.wikiName)
  }

  given Ordering[PageDescriptor] = Ordering.by(p => (p.name, p.id))
}

final case class PageDescriptor(id: PageDescriptor.ID, name: PageDescriptor.Name)
