/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.views.shapes

import javafx.scene.shape.Ellipse
import de.thm.move.types.*
import de.thm.move.util.GeometryUtils
import javafx.scene.paint.Color
import upickle.default.*

/** A circle represented by it's '''middle-point''' and it's width + height */
class ResizableCircle(point: Point, width: Double, height: Double)
    extends Ellipse(point._1, point._2, width, height)
    with ResizableShape
    with RectangleLike
    with ColorizableShape:
  private val (x, y) = point

  override val adjustCoordinates = false

  override def setX(x: Double): Unit = setCenterX(x)

  override def setY(y: Double): Unit = setCenterY(y)

  override def setWidth(w: Double): Unit =
    setRadiusX(w / 2)

  override def setHeight(h: Double): Unit =
    setRadiusY(h / 2)

  override def getX: Double = getCenterX

  override def getY: Double = getCenterY

  override def getWidth: Double = getRadiusX * 2
  override def getHeight: Double = getRadiusY * 2
  override def copy: ResizableCircle =
    val duplicate = new ResizableCircle(point, getRadiusX, getRadiusY)
    duplicate.copyPosition(this)
    duplicate.copyColors(this)
    duplicate

/** Companion object for ResizableCircle providing JSON serialization and deserialization support.
 */
object ResizableCircle {

  /** Implicit JSON serializer and deserializer for Point, represented as a JSON array.
   */
  implicit val pointRW: ReadWriter[Point] = readwriter[ujson.Value].bimap[Point](
    p => ujson.Arr(p._1, p._2),
    json => (json(0).num, json(1).num)
  )

  /** Implicit JSON serializer and deserializer for ResizableCircle.
   * Serializes as a JSON object containing type, point, width, and height.
   *
   * @example Serialization:
   * {{{
   *   val circle = new ResizableCircle((10, 20), 50, 50)
   *   val json = write(circle)
   *    }}}
   * @example Deserialization:
   *   {{{
   *   val jsonStr = """{"type": "ResizableCircle", "point": [10, 20], "width": 50, "height": 50}"""
   *   val circle = read[ResizableCircle](jsonStr)
   * }}}
   */

  implicit val rw: ReadWriter[ResizableCircle] = readwriter[ujson.Value].bimap[ResizableCircle](
    // Serialization
    circle => {
      val fillColor = circle.getFillColor match {
        case color: Color => writeJs(SerializableColor.colorToSerializableColor(color))
        case _ => writeJs(SerializableColor(0, 0, 0, 0)) // default transparent
      }
      val strokeColor = circle.getStrokeColor match {
        case color: Color => writeJs(SerializableColor.colorToSerializableColor(color))
        case _ => writeJs(SerializableColor(0, 0, 0, 0)) // default transparent
      }

      ujson.Obj(
        "type" -> "ResizableCircle",
        "point" -> List(circle.getX, circle.getY),
        "width" -> circle.getWidth,
        "height" -> circle.getHeight,
        "fillColor" -> fillColor,
        "strokeColor" -> strokeColor
      )
    },
    // Deserialization
    json => json("type").str match {
      case "ResizableCircle" =>
        val circle = new ResizableCircle(
          (json("point")(0).num, json("point")(1).num),
          json("width").num/2,
          json("height").num/2
        )

        val fillColor = read[SerializableColor](json("fillColor"))
        val strokeColor = read[SerializableColor](json("strokeColor"))

        circle.setFillColor(fillColor)
        circle.setStrokeColor(strokeColor)

        circle
      case _ => throw new IllegalArgumentException("Unexpected shape type")
    }
  )
}