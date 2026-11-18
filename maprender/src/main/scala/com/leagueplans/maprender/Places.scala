package com.leagueplans.maprender

import io.circe.syntax.*
import net.runelite.cache.definitions.loaders.WorldMapLoader
import net.runelite.cache.fs.Store
import net.runelite.cache.{AreaManager, IndexType, WorldMapManager}

import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/** Named places to search for on the map, from the game's own world map.
  *
  * Published as `places.json`: a list of `{name, x, y, plane, kind}`, with `kind` the name of a
  * [[Place.Kind]]. The app reads this, so it's a published format: fields and kinds may be added,
  * never renamed or removed.
  */
object Places {
  val fileName = "places.json"

  /** The labels on the world map, by their text size, and the named maps away from the surface,
    * at their origin. Names repeat, since many places share a name. */
  def read(store: Store): List[Place] = {
    val areas = AreaManager(store)
    areas.load()
    val worldMap = WorldMapManager(store)
    worldMap.load()
    val labels =
      for {
        element <- worldMap.getElements.asScala.toList
        area <- Option(areas.getArea(element.getAreaDefinitionId))
        name = clean(area.getName) if name.nonEmpty
        position = element.getWorldPosition
      } yield Place(name, position.getX, position.getY, position.getZ, Place.Kind.fromTextScale(area.getTextScale))

    val archive = store.getIndex(IndexType.WORLDMAP).getArchive(0)
    val maps =
      for {
        file <- archive.getFiles(store.getStorage.loadArchive(archive)).getFiles.asScala.toList
        definition = WorldMapLoader().load(file.getContents, file.getFileId) if !definition.isSurface
        name = clean(definition.getName) if name.nonEmpty
        position = definition.getPosition
      } yield Place(name, position.getX, position.getY, position.getZ, Place.Kind.Map)

    labels ++ maps
  }

  /** Map labels carry the game's markup: line breaks and colour tags */
  def clean(name: String): String =
    Option(name).getOrElse("")
      .replace("<br>", " ")
      .replaceAll("<[^>]*>", "")
      .replaceAll("\\s+", " ")
      .trim

  def write(store: Store, out: Path): Int = {
    val places = read(store)
    Files.writeString(out.resolve(fileName), places.asJson.noSpaces)
    places.size
  }
}
