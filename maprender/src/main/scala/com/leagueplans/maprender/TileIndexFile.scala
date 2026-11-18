package com.leagueplans.maprender

import com.leagueplans.common.model.{TileIndex, TileKey}
import io.circe.syntax.*

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*
import scala.util.Using

/** Writes the [[TileIndex]] for a render, from the tiles on disk */
object TileIndexFile {
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
    Files.writeString(out.resolve(TileIndex.fileName), TileIndex.of(tiles).asJson.noSpaces)
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
