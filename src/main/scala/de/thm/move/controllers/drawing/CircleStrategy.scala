package de.thm.move.controllers.drawing

import de.thm.move.controllers.ChangeDrawPanelLike
import de.thm.move.types._
import de.thm.move.util.GeometryUtils
import de.thm.move.views.shapes.ResizableCircle

class CircleStrategy(changeLike: ChangeDrawPanelLike)
    extends RectangularStrategy(changeLike, new ResizableCircle((0, 0), 0, 0)):
  private var startPoint: Point = (0, 0)

  override protected def setStartXY(p: Point): Unit =
    startPoint = p
    tmpFigure.setXY(startPoint)

  override protected def setBounds(point: Point): Unit =
    super.setBounds(point)
    val (middleX, middleY) = GeometryUtils.middleOfLine(startPoint, point)
    tmpFigure.setX(middleX)
    tmpFigure.setY(middleY)

  override def reset(): Unit =
    super.reset()
    startPoint = (0, 0)

  override protected def calculateBounds(point: Point): Point =
    val (width, height) = point - startPoint
    if drawConstraintProperty.get then
      val tmpDelta = width min height
      (tmpDelta, tmpDelta)
    else (width, height)
