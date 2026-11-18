package com.leagueplans.maprender

import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.*
import io.circe.{Codec, Decoder, Encoder, KeyDecoder, KeyEncoder}

import java.nio.file.{Files, Path}
import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.*
import scala.util.Using

object TileIndex {
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

  private val tileFile = """(-?\d+)_(-?\d+)\.png""".r

  /** Lists the tiles under the output directory, from the files themselves, so that the index
    * always matches what's published. */
  def read(out: Path): List[TileKey] =
    for {
      plane <- subdirectories(out).flatMap(_.toIntOption)
      zoom <- subdirectories(out.resolve(plane.toString)).flatMap(_.toIntOption)
      file <- files(out.resolve(s"$plane/$zoom"))
      tile <- file match {
        case tileFile(x, y) => Some(TileKey(plane, zoom, x.toInt, y.toInt))
        case _ => None
      }
    } yield tile

  def write(out: Path): Int = {
    val tiles = read(out)
    Files.writeString(out.resolve(fileName), TileIndex.of(tiles).asJson.noSpaces)
    tiles.size
  }

  private def subdirectories(dir: Path): List[String] =
    entries(dir).filter(Files.isDirectory(_)).map(_.getFileName.toString)

  private def files(dir: Path): List[String] =
    entries(dir).filter(Files.isRegularFile(_)).map(_.getFileName.toString)

  private def entries(dir: Path): List[Path] =
    if (!Files.isDirectory(dir)) List.empty
    else Using.resource(Files.list(dir))(_.iterator().asScala.toList)
}

/** Every tile that exists, so that the map never asks for one that doesn't.
  *
  * Most of the map's area is void, with no tile. A request for a missing tile is a 404
  * that the CDN in front of the tiles doesn't cache, so without the index each one would be a
  * round trip to GitHub.
  *
  * Published as `index.json`:
  * {{{
  * {"tiles": {"<plane>/<zoom>": ["<x>_<y>", ...], ...}}
  * }}}
  * The app reads this, so it's a published format: fields may be added, never renamed or removed.
  * An object at the top leaves room for that.
  */
final case class TileIndex(tiles: Map[TileIndex.Layer, List[TileIndex.Position]])
