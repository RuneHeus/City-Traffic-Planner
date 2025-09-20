/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.thm.move.views.shapes

import de.thm.move.models.{FillPattern, LinePattern}
import javafx.scene.shape.Polygon
import de.thm.move.types.*
import scala.jdk.CollectionConverters.*
import de.thm.move.controllers.drawing.ShrinkStrategy
import de.thm.move.controllers.ChangeDrawPanelLike
import de.thm.move.controllers.zones.zoneTypeManager

import upickle.default.*
import de.thm.move.views.shapes.ColorizableShape.{fillPatternRW, linePatternRW}

import scala.jdk.CollectionConverters.*
import de.thm.move.types.*
import javafx.scene.paint.Color

import scala.jdk.CollectionConverters.*

class ResizablePolygon(val points: List[Double])
  extends Polygon(points: _*)
    with ResizableShape
    with ColorizableShape
    with QuadCurveTransformable
    with PathLike:


  override lazy val edgeCount: Int = points.size / 2

  override def toCurvedShape: QuadCurvePolygon = QuadCurvePolygon(this)

  override def copy: ResizablePolygon =
    val duplicate = new ResizablePolygon(
      getPoints.asScala.map(_.doubleValue).toList
    )
    duplicate.copyColors(this)
    duplicate.setRotate(getRotate)
    duplicate


  private def pointIdxToListIdx(idx: Int): (Int, Int) = (idx * 2, idx * 2 + 1)

  override def resize(idx: Int, delta: (Double, Double)): Unit =
    val (xIdx, yIdx) = pointIdxToListIdx(idx)
    getPoints.set(xIdx, getPoints.get(xIdx) + delta.x)
    getPoints.set(yIdx, getPoints.get(yIdx) + delta.y)

  override def getEdgePoint(idx: Int): (Double, Double) =
    val (xIdx, yIdx) = pointIdxToListIdx(idx)
    (getPoints.get(xIdx), getPoints.get(yIdx))

  var zoneType: String = zoneTypeManager.getZoneType



/** Companion object for ResizablePolygon, providing methods for JSON serialization,
 * deserialization, and alternative constructors.
 */
object ResizablePolygon:

  private var onUnregisterCallback: Option[ResizablePolygon => Unit] = None
  private var onRegisterCallback: Option[ResizablePolygon => Unit] = None

  def setOnUnregisterCallback(callback: ResizablePolygon => Unit): Unit =
    onUnregisterCallback = Some(callback)

  def setonRegisterCallback(callback: ResizablePolygon => Unit): Unit =
    onRegisterCallback = Some(callback)

  var polygons: List[ResizablePolygon] = List()

  def registerPolygon(polygon: ResizablePolygon): Unit =
    this.polygons = this.polygons.appendedAll(List(polygon))
    onRegisterCallback.foreach(_.apply(polygon))


  def unregisterPolygon(oldPolygon: ResizablePolygon): Unit =
    this.polygons = this.polygons.filter((polygon: ResizablePolygon) => !(polygon == oldPolygon))
    onUnregisterCallback.foreach(_.apply(oldPolygon))



  def clearPolygons(): Unit =
    this.polygons = List()



  /** Implicit JSON serializer and deserializer for a list of points (doubles).
   * Each point is represented as a JSON array of numeric values.
   */
  implicit val pointListRW: ReadWriter[List[Double]] = readwriter[ujson.Value].bimap[List[Double]](
    points => ujson.Arr(points.map(ujson.Num): _*), // Convert each Double to ujson.Num
    json => json.arr.map(_.num).toList
  )

  /** Implicit JSON serializer and deserializer for ResizablePolygon.
   * Serializes as a JSON object containing type and points list.
   *
   * @example Serialization:
   * {{{
   *   val polygon = new ResizablePolygon(List(0.0, 0.0, 50.0, 50.0, 100.0, 0.0))
   *   val json = write(polygon)
   *    }}}
   * @example Deserialization:
   *   {{{
   *   val jsonStr = """{"type": "ResizablePolygon", "points": [0.0, 0.0, 50.0, 50.0, 100.0, 0.0]}"""
   *   val polygon = read[ResizablePolygon](jsonStr)
   * }}}
   */

  implicit val rw: ReadWriter[ResizablePolygon] = readwriter[ujson.Value].bimap[ResizablePolygon](
    // Serialization
    polygon => {
      val fillColor = polygon.getFillColor match {
        case color: Color => writeJs(SerializableColor.colorToSerializableColor(color))
        case _ => writeJs(SerializableColor(0, 0, 0, 0)) // default transparent
      }
      val strokeColor = polygon.getStrokeColor match {
        case color: Color => writeJs(SerializableColor.colorToSerializableColor(color))
        case _ => writeJs(SerializableColor(0, 0, 0, 0)) // default transparent
      }
      ujson.Obj(
        "type" -> "ResizablePolygon",
        "points" -> polygon.getPoints.asScala.map(_.doubleValue).toList,
        "fillColor" -> fillColor,
        "strokeColor" -> strokeColor,
        "strokeWidth" -> ujson.Num(polygon.getStrokeWidth),
        "linePattern" -> writeJs(polygon.linePattern.get),
        "fillPattern" -> writeJs(polygon.fillPatternProperty.get)
      )
    },
    // Deserialization
    json => {
      if json("type").str != "ResizablePolygon" then
        throw new IllegalArgumentException("Unexpected shape type")

      val points = json("points").arr.map(_.num).toList
      val fillColor = read[SerializableColor](json("fillColor"))
      val strokeColor = read[SerializableColor](json("strokeColor"))
      val strokeWidth = json("strokeWidth").num
      val linePattern = read[LinePattern.Value](json("linePattern"))
      val fillPattern = read[FillPattern.Value](json("fillPattern"))

      val polygon = new ResizablePolygon(points)
      polygon.setFillColor(fillColor)
      polygon.setStrokeColor(strokeColor)
      polygon.setStrokeWidth(strokeWidth)
      polygon.linePattern.set(linePattern)
      polygon.fillPatternProperty.set(fillPattern)
      polygon
    }
  )

  def apply(points: List[Point]): ResizablePolygon =
    val singlePoints = points.flatMap { case (x, y) => List(x, y) }
    new ResizablePolygon(singlePoints)

  def apply(cubed: QuadCurvePolygon): ResizablePolygon =
    val polygon = ResizablePolygon(cubed.getUnderlyingPolygonPoints)
    polygon.colorizeShape(cubed.getFillColor, cubed.getStrokeColor)
    polygon.setStrokeWidth(cubed.getStrokeWidth)
    polygon