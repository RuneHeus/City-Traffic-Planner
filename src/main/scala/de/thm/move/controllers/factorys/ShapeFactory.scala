/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.controllers.factorys

import de.thm.move.Roads.RoadTypeManager

import java.net.URI
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.shape.{LineTo, MoveTo}
import de.thm.move.types.*
import de.thm.move.models.SelectedShape
import de.thm.move.views.anchors.Anchor
import de.thm.move.views.shapes.*

object ShapeFactory:
  def newImage(imgUri: URI): ResizableImage =
    val img = new Image(imgUri.toString)
    ResizableImage(imgUri, img)

  private def setDefaultColor[T <: ColorizableShape](
      shape: T
  )(fillColor: Color, strokeColor: Color, strokeWidth: Int): T =
    shape.colorizeShape(fillColor, strokeColor)
    shape.setStrokeWidth(strokeWidth)
    shape

  def newRectangle(
      point: Point,
      width: Double,
      height: Double
  ): (Color, Color, Int) => ResizableRectangle =
    val rectangle = new ResizableRectangle(point, width, height)
    setDefaultColor(rectangle)

  def newLine(
      start: Point,
      end: Point,
      strokeSize: Int
  ): (Color, Color, Int) => ResizableLine =
    val line = new ResizableLine(start, end, strokeSize)
    setDefaultColor(line)

  def newCircle(
      point: Point,
      width: Double,
      height: Double
  ): (Color, Color, Int) => ResizableCircle =
    val circle = new ResizableCircle(point, width, height)
    setDefaultColor(circle)

  def newAnchor(point: Point): Anchor =
    val (x, y) = point
    new Anchor(x, y)

  def newPolygon(
      points: List[Point]
  ): (Color, Color, Int) => ResizablePolygon =
    val polygon = ResizablePolygon(points)
    setDefaultColor(polygon)

  def newPath(points: List[Point]): (Color, Color, Int) => ResizablePath =
    val (hdX, hdY) = points.head
    val startpoint = new MoveTo(hdX, hdY)
    val pathElements = for (x, y) <- points.tail yield new LineTo(x, y)

    val path = new ResizablePath(startpoint, pathElements)
    (setDefaultColor(path) _)

  def createTemporaryShape(shape: SelectedShape.SelectedShape, p: Point)(
      stroke: Color
  ): ResizableShape =
    (shape match {
      case SelectedShape.Rectangle => newRectangle(p, 2, 2)
      case SelectedShape.Circle    => newCircle(p, 2, 2)
      case _ if RoadTypeManager.allRoadTypes.exists(_._1 == shape) => newLine(p, p, 2) // If it is any type of road
    })(null, stroke, 2)
