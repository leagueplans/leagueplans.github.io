package com.leagueplans.maprender

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

import scala.util.Try

object Place {
  given Codec[Place] = deriveCodec

  enum Kind {
    /** A world map label in the smallest text */
    case Local
    /** A world map label in the middle text size */
    case Town
    /** A world map label in the largest text */
    case Kingdom
    /** A separately mapped area away from the surface, such as a dungeon */
    case Map
  }

  object Kind {
    given Encoder[Kind] = Encoder[String].contramap(_.toString)
    given Decoder[Kind] = Decoder[String].emapTry(s => Try(Kind.valueOf(s)))

    /** The kind of world map label, from its text size. Fails on a size the game didn't use
      * when this was written, so that a change shows up when rendering rather than as
      * mislabelled places. */
    def fromTextScale(textScale: Int): Kind =
      textScale match {
        case 0 => Local
        case 1 => Town
        case 2 => Kingdom
        case other => throw IllegalArgumentException(s"Unknown world map text scale: $other")
      }
  }
}

/** A named place from the game's own world map, published in [[Places]] */
final case class Place(name: String, x: Int, y: Int, plane: Int, kind: Place.Kind)
