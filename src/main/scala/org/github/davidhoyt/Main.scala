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
    //Outlook has max # of columns per table: 63
    //But Outlook will try and merge everything after 62 into the 63rd column,
    //so we effectively have only 62 columns per slice.
    //
    //Unfortunately this increases the size of the output since we can only
    //merge up to the size of a slice.
    val COLS_PER_SLICE = 62

    //Since the output is a table of tables, the max width cannot exceed
    //the max # of columns per table in Outlook.
    val MAX_WIDTH = COLS_PER_SLICE * COLS_PER_SLICE

    val img = ImageIO.read(input)
    val pixels = (img.getRaster.getDataBuffer.asInstanceOf[DataBufferByte]).getData
    val width = img.getWidth()
    val height = img.getHeight()
    val hasAlpha = img.getAlphaRaster ne null

    require(width < MAX_WIDTH, s"The width of the image must be less than $MAX_WIDTH")

    val result = Array.ofDim[Int](height, width)

    //Courtesy http://stackoverflow.com/questions/6524196/java-get-pixel-array-from-image
    //
    //Could be cleaned up a bit but don't really care that much.

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

    val blend = 0xffffff
    //val blend_a = ((blend >> 24) & 0xff).toShort //alpha
    val blend_r = ((blend >> 16) & 0xff).toShort //red
    val blend_g = ((blend >>  8) & 0xff).toShort //green
    val blend_b = ((blend >>  0) & 0xff).toShort //blue

    def blendColors(alpha: Double, color: Short, blend_color: Short): Short =
      ((blend_color * (1.0D - alpha)) + (color * alpha)).toShort

    def hexFor(argb: Int): String = {
      var b, g, r, a: Short = 0
      var o: Double = 0.0

      a = ((argb >> 24) & 0xff).toShort //alpha
      o = a.toDouble / 255.0            //opacity

      r = ((argb >> 16) & 0xff).toShort //red
      g = ((argb >>  8) & 0xff).toShort //green
      b = ((argb >>  0) & 0xff).toShort //blue

      f"${blendColors(o, r, blend_r)}%02x${blendColors(o, g, blend_g)}%02x${blendColors(o, b, blend_b)}%02x"
    }

    def processSlice(slice: Int): Unit = {
      import scala.math._

      val COL_RESET = slice * COLS_PER_SLICE
      val MAX_COL_RESET = ((slice + 1) * COLS_PER_SLICE)
      var MAX_COL = 0
      var row = 0
      var col = COL_RESET
      var argb = 0
      var colWidth = 0
      var colRight = 0
      var isFirstRow = false
      var startOfNewRow = false
      var hex: String = ""

      output.println("      <table border='0' cellpadding='0' cellspacing='0' style='border-collapse:collapse;border:none;font-size:0;line-height:0;mso-margin-top-alt:1px;'>")

      while(row < result.length) {
        MAX_COL = min(result(row).length, MAX_COL_RESET)
        isFirstRow = row == 0

        output.print("        <tr style='font-size:1px;mso-line-height-alt:0;mso-margin-top-alt:1px;mso-height-source:0;'>")

        while(col < MAX_COL) {
          argb = result(row)(col)
          startOfNewRow = col == 0
          colWidth = 1
          colRight = col

          //Attempt to aggregate pixels of neighboring columns of the same color.
          while (colRight < MAX_COL - 1 && result(row)(colRight + 1) == argb)
            colRight += 1
          colWidth = colRight - col + 1

          hex = hexFor(result(row)(col))

          output.print(s"<td valign='top' colspan='${colWidth}' style='width:${colWidth}px;background:#$hex;height:1px;font-size:0px;line-height:0;mso-line-height-rule:exactly;mso-width-source:1px;-webkit-text-size-adjust:none;'></td>")
          col += colWidth
        }

        output.println(s"</tr>")

        col = COL_RESET
        row += 1
      }

      output.println(s"      </table>")
    }

    //Output a table that contains a cell for every slice.
    output.println(s"<table border='0' cellpadding='0' cellspacing='0' style='border-collapse:collapse;border:none;font-size:1px;line-height:0; mso-margin-top-alt:1px;'>")
    output.println(s"  <tr>")
    val slices = width / COLS_PER_SLICE + (if (width % COLS_PER_SLICE != 0) 1 else 0)
    var slice = 0
    while(slice < slices) {
      output.println(s"    <td valign='top'>")
      processSlice(slice)
      output.println(s"    </td>")
      slice += 1
    }
    output.println(s"  </tr>")
    output.println(s"</table>")
  }
}
