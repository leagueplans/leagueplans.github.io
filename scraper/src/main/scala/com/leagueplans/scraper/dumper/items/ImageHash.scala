package com.leagueplans.scraper.dumper.items

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.imageio.ImageIO

/** Identifies an icon by the picture it shows rather than by the bytes it arrived in.
  *
  * The wiki serves its images through something that re-compresses them losslessly, and
  * not always to the same bytes: the same upload has been measured arriving as different
  * files two months apart. Hashing the bytes therefore reported most of the catalogue as
  * changed on every scrape. Hashing the decoded pixels only moves when the picture does.
  */
private[items] object ImageHash {
  def of(data: Array[Byte]): String =
    Option(ImageIO.read(ByteArrayInputStream(data))) match {
      case Some(image) =>
        val width = image.getWidth
        val height = image.getHeight
        val pixels = image.getRGB(0, 0, width, height, null, 0, width)
        val buffer = ByteBuffer.allocate(8 + pixels.length * 4).putInt(width).putInt(height)
        // A fully transparent pixel shows nothing, so the colour stored behind it is
        // arbitrary and encoders differ on it.
        pixels.foreach(pixel => buffer.putInt(if ((pixel >>> 24) == 0) 0 else pixel))
        digest(buffer.array())

      // Nothing the wiki serves for items today fails to decode. Should that change, a
      // hash of the bytes is the most useful fallback: at worst the icon is reported as
      // changed when it has not, which is visible, rather than the reverse.
      case None =>
        digest(data)
    }

  /** Truncated because it is written once per image into a file measured in megabytes, and
    * 64 bits is far more than enough to tell a few tens of thousands of icons apart.
    */
  private def digest(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .take(8)
      .map(byte => f"$byte%02x")
      .mkString
}
