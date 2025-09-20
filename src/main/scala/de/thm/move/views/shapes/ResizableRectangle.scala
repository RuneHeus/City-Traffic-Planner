/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.views.shapes

import javafx.scene.shape.Rectangle
import de.thm.move.types.*
import javafx.scene.paint.Color
import upickle.default.*

class ResizableRectangle(startPoint: Point, width: Double, height: Double)
    extends Rectangle(startPoint._1, startPoint._2, width, height)
    with ResizableShape
    with RectangleLike
    with ColorizableShape:
  private val (x, y) = startPoint

  override def getTopLeft: Point = (getX, getY)
  override def getTopRight: Point = (getX + getWidth, getY)
  override def getBottomLeft: Point = (getX, getY + getHeight)
  override def getBottomRight: Point = (getX + getWidth, getY + getHeight)
  override def copy: ResizableRectangle =
    val duplicate = new ResizableRectangle(startPoint, width, height)
    duplicate.copyColors(this)
    duplicate.copyPosition(this)
    duplicate

/** Companion object for ResizableRectangle that provides JSON serialization and deserialization support.
 */
object ResizableRectangle {

  /** Implicit JSON serializer and deserializer for Point, representing it as a JSON array.
   */
  implicit val pointRW: ReadWriter[Point] = readwriter[ujson.Value].bimap[Point](
    p => ujson.Arr(p._1, p._2),
    json => (json(0).num, json(1).num)
  )

  /** Implicit JSON serializer and deserializer for ResizableRectangle.
   * Serializes as a JSON object containing type, point, width, and height.
   *
   * @example Serialization:
   * {{{
   *   val rect = new ResizableRectangle((10, 20), 50, 100)
   *   val json = write(rect)
   *    }}}
   * @example Deserialization:
   *   {{{
   *   val jsonStr = """{"type": "ResizableRectangle", "point": [10, 20], "width": 50, "height": 100}"""
   *   val rect = read[ResizableRectangle](jsonStr)
   * }}}
   */

  implicit val rw: ReadWriter[ResizableRectangle] = readwriter[ujson.Value].bimap[ResizableRectangle](
    // Serialization
    rect => {
      val fillColor = rect.getFillColor match {
        case color: Color => writeJs(SerializableColor.colorToSerializableColor(color))
        case _ => writeJs(SerializableColor(0, 0, 0, 0)) // default transparent
      }
      val strokeColor = rect.getStrokeColor match {
        case color: Color => writeJs(SerializableColor.colorToSerializableColor(color))
        case _ => writeJs(SerializableColor(0, 0, 0, 0)) // default transparent
      }

      ujson.Obj(
        "type" -> "ResizableRectangle",
        "point" -> List(rect.getX, rect.getY),
        "width" -> rect.getWidth,
        "height" -> rect.getHeight,
        "fillColor" -> fillColor,
        "strokeColor" -> strokeColor
      )
    },
    // Deserialization
    json => json("type").str match {
      case "ResizableRectangle" =>
        val rect = new ResizableRectangle(
          (json("point")(0).num, json("point")(1).num),
          json("width").num,
          json("height").num
        )

        val fillColor = read[SerializableColor](json("fillColor"))
        val strokeColor = read[SerializableColor](json("strokeColor"))

        rect.setFillColor(fillColor)
        rect.setStrokeColor(strokeColor)
        rect
      case _ => throw new IllegalArgumentException("Unexpected shape type")
    }
  )
}