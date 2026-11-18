package com.leagueplans.common.model

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder, KeyDecoder, KeyEncoder}

import scala.collection.immutable.ListMap

object TileIndex {
  /** Where the map tiles publish their index, relative to the tiles' root */
  val fileName = "index.json"

  /** One plane at one zoom, published as `<plane>/<zoom>` */
  final case class Layer(plane: Int, zoom: Int)

  object Layer {
    given KeyEncoder[Layer] = KeyEncoder.instance(layer => s"${layer.plane}/${layer.zoom}")
    given KeyDecoder[Layer] = KeyDecoder.instance {
      case s"$plane/$zoom" => plane.toIntOption.zip(zoom.toIntOption).map(Layer(_, _))
      case _ => None
    }
  }

  /** A tile's place within its layer, published as `<x>_<y>` */
  final case class Position(x: Int, y: Int)

  object Position {
    given Encoder[Position] = Encoder[String].contramap(position => s"${position.x}_${position.y}")
    given Decoder[Position] = Decoder[String].emap(name =>
      (name match {
        case s"${x}_$y" => x.toIntOption.zip(y.toIntOption).map(Position(_, _))
        case _ => None
      }).toRight(s"Not a tile position: $name")
    )
  }

  given Codec[TileIndex] = deriveCodec

  /** Planes in order, then zooms from the most detailed, then tiles by x and y, so that the
    * published file only changes when the tiles do */
  def of(tiles: Iterable[TileKey]): TileIndex =
    TileIndex(ListMap.from(
      tiles
        .groupBy(tile => Layer(tile.plane, tile.zoom))
        .toList
        .sortBy { case (layer, _) => (layer.plane, -layer.zoom) }
        .map { case (layer, group) => layer -> group.toList.map(tile => Position(tile.x, tile.y)).sortBy(p => (p.x, p.y)) }
    ))
}

/** Every tile that exists, so that the map never asks for one that doesn't.
  *
  * Most of the map's area is void, with no tile. A request for a missing tile is a 404
  * that the CDN in front of the tiles doesn't cache, so without the index each one would be a
  * round trip to GitHub.
  *
  * The map tiles publish this as [[TileIndex.fileName]]:
  * {{{
  * {"tiles": {"<plane>/<zoom>": ["<x>_<y>", ...], ...}}
  * }}}
  * The app reads it, so it's a published format: fields may be added, never renamed or removed.
  * An object at the top leaves room for that.
  */
final case class TileIndex(tiles: Map[TileIndex.Layer, List[TileIndex.Position]])
