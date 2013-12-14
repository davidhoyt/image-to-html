package org.github.davidhoyt

import java.io._
import scala.io.Source
import java.awt.image.{DataBufferByte, BufferedImage}
import javax.imageio.ImageIO

object Main extends App {
  args match {
    case Array(readFrom, writeTo) =>
      run(readFrom, writeTo)
    case _ =>
      help()
  }

  def help(): Int = {
    println("image-to-html <path-to-image> <path-to-file-output>")
    -1
  }

  def run(readFrom: String, writeTo: String): Int = {
    val f = new File(readFrom)
    var input: InputStream = null
    var output: PrintStream = null
    try {
      require(f.exists(), s"$readFrom does not exist")

      input = new FileInputStream(f)
      output =
        if (writeTo != "-")
          new PrintStream(new FileOutputStream(writeTo))
        else
          System.out

      process(input, output)
      0
    } catch {
      case t: Throwable =>
        println(t.getMessage)
        -1
    } finally {
      if (input ne null)
        input.close()
      if (output ne null)
        output.close()
    }
  }

  def process(input: InputStream, output: PrintStream): Unit = {
    //Courtesy http://stackoverflow.com/questions/6524196/java-get-pixel-array-from-image

    val img = ImageIO.read(input)
    val pixels = (img.getRaster.getDataBuffer.asInstanceOf[DataBufferByte]).getData
    val width = img.getWidth()
    val height = img.getHeight()
    val hasAlpha = img.getAlphaRaster ne null

    val result = Array.ofDim[Int](height, width)

    if (hasAlpha) {
      val pixelLength = 4
      var pixel, row, col = 0

      while(pixel < pixels.length) {
        var argb = 0

        argb += (pixels(pixel + 0).toInt & 0xff) << 24 //alpha
        argb += (pixels(pixel + 1).toInt & 0xff)       //blue
        argb += (pixels(pixel + 2).toInt & 0xff) <<  8 //green
        argb += (pixels(pixel + 3).toInt & 0xff) << 16 //red

        result(row)(col) = argb

        col += 1
        if (col == width) {
          col = 0
          row += 1
        }
        pixel += pixelLength
      }
    } else {
      val pixelLength = 3
      var pixel, row, col = 0

      while(pixel < pixels.length) {
        var argb = 0

        argb += -16777216                              // 255 alpha
        argb += (pixels(pixel + 0).toInt & 0xff)       //blue
        argb += (pixels(pixel + 1).toInt & 0xff) <<  8 //green
        argb += (pixels(pixel + 2).toInt & 0xff) << 16 //red

        result(row)(col) = argb

        col += 1
        if (col == width) {
          col = 0
          row += 1
        }
        pixel += pixelLength
      }
    }


    output.println("<html>\n<body>\n\n")

    for {
      row <- 0 until result.length
      col <- 0 until result(row).length
      isFirstRow = row == 0
      startOfNewRow = col == 0
      argb = result(row)(col)
      b = ((argb >>  0) & 0xff).toShort //blue
      g = ((argb >>  8) & 0xff).toShort //green
      r = ((argb >> 16) & 0xff).toShort //red
      a = ((argb >> 24) & 0xff).toShort //alpha
      o = a.toDouble / 255.0            //opacity
    } {
      output.print(s"<div style='width: 1px; height: 1px; background-color: rgba($r, $g, $b, $o); float: left;'></div>")
      if (startOfNewRow)
        output.println(s"<div style='clear:both;'></div>")
    }

    output.println("\n\n</body>\n</html>")
  }
}
