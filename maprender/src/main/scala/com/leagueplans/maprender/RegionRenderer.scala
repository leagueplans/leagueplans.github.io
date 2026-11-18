package com.leagueplans.maprender

import net.runelite.cache.MapImageDumper
import net.runelite.cache.fs.Store
import net.runelite.cache.region.{Region, RegionLoader}
import net.runelite.cache.util.XteaKeyManager

import java.awt.image.BufferedImage
import java.lang.reflect.Method
import scala.jdk.CollectionConverters.*

object RegionRenderer {
  // Private in the library. If an upgrade renames it, this fails as soon as rendering starts.
  private val drawMapIcons: Method = {
    val method = classOf[MapImageDumper].getDeclaredMethod(
      "drawMapIcons", classOf[BufferedImage], classOf[Int], classOf[Int], classOf[Region], classOf[Int]
    )
    method.setAccessible(true)
    method
  }
}

/** Draws game regions as images at 4 px per game tile, using RuneLite's map dumper.
  *
  * The game's map regions have needed no decryption keys since revision 237, so none are given.
  */
final class RegionRenderer(store: Store) {
  private val loader = RegionLoader(store, XteaKeyManager())
  private val dumper = MapImageDumper(store, loader)
  dumper.setLabelRegions(false)
  dumper.setRenderLabels(false)
  dumper.setTransparency(true)
  dumper.load()

  def regions: List[Region] =
    loader.getRegions.asScala.toList

  /** Draws one plane of a region.
    *
    * The dumper draws the neighbouring regions' objects where they overlap this one, but only
    * this region's own icons, so an icon on a border would be cut in half. The neighbours' icons
    * are drawn here too, at the same offsets the dumper uses for their objects.
    */
  def render(region: Region, plane: Int): BufferedImage = {
    val image = dumper.drawRegion(region, plane)
    for (dx <- -1 to 1; dy <- -1 to 1 if dx != 0 || dy != 0)
      Option(loader.findRegionForRegionCoordinates(region.getRegionX + dx, region.getRegionY + dy)).foreach(neighbour =>
        RegionRenderer.drawMapIcons.invoke(dumper, image, Int.box(64 * dx), Int.box(64 * -dy), neighbour, Int.box(plane))
      )
    Pixels.fixAlpha(image)
  }
}
