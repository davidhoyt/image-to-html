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


    //output.println("<html>\n<body>\n\n")
    output.println("<table border='0' cellpadding='0' cellspacing='0'>")


    val blend = 0xffffff
    //val blend_a = ((blend >> 24) & 0xff).toShort //alpha
    val blend_r = ((blend >> 16) & 0xff).toShort //red
    val blend_g = ((blend >>  8) & 0xff).toShort //green
    val blend_b = ((blend >>  0) & 0xff).toShort //blue

    def blendColors(alpha: Double, color: Short, blend_color: Short): Short =
      ((blend_color * (1.0D - alpha)) + (color * alpha)).toShort

    var row = 0
    var col = 0
    var argb = 0
    var colWidth = 0
    var colRight = 0
    var isFirstRow = false
    var startOfNewRow = false
    var b, g, r, a: Short = 0
    var o: Double = 0.0
    while(row < result.length) {
      isFirstRow = row == 0

      output.println("  <tr>")

      while(col < result(row).length) {
        argb = result(row)(col)
        startOfNewRow = col == 0
        colWidth = 1
        colRight = col

        while (colRight < result(row).length - 1 && result(row)(colRight + 1) == argb)
          colRight += 1
        colWidth = colRight - col + 1

        argb = result(row)(col)

        a = ((argb >> 24) & 0xff).toShort //alpha
        o = a.toDouble / 255.0            //opacity

        r = ((argb >> 16) & 0xff).toShort //red
        g = ((argb >>  8) & 0xff).toShort //green
        b = ((argb >>  0) & 0xff).toShort //blue

        val hex = f"#${blendColors(o, r, blend_r)}%02x${blendColors(o, g, blend_g)}%02x${blendColors(o, b, blend_b)}%02x"

        //output.print(s"<div style='width:${colWidth}px;height:1px;background-color:rgba($r,$g,$b,$o);float:left;'></div>")
        //output.print(s"<td style='background-color: rgba($r,$g,$b,$o); width: 1px; height: 1px;' colspan='$colWidth' width='1' height='1'></td>")
        output.println(s"    <td bgcolor='$hex' colspan='$colWidth' width='1' height='1'></td>")
        //output.println(s"    <td colspan='$colWidth' style='background-color: $hex; width: ${colWidth}px; height: 1px;'></td>")

        col += colWidth
      }

      //output.println(s"<div style='clear:both;'></div>")
      output.println(s"  </tr>")

      col = 0
      row += 1
    }

    output.println(s"</table>")
    //output.println("\n\n</body>\n</html>")
  }
}
