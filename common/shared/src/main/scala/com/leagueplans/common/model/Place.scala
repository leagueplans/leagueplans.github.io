package com.leagueplans.common.model

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

import scala.util.Try

object Place {
  /** Where the map tiles publish the places, relative to the tiles' root */
  val fileName = "places.json"

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
  }
}

/** A named place from the game's own world map, for the map's labels and place search.
  *
  * The map tiles publish a list of these as [[Place.fileName]], with `kind` the name of a
  * [[Place.Kind]]. The app reads it, so it's a published format: fields and kinds may be added,
  * never renamed or removed. A reader should skip a place whose kind it doesn't know.
  */
final case class Place(name: String, x: Int, y: Int, plane: Int, kind: Place.Kind)
