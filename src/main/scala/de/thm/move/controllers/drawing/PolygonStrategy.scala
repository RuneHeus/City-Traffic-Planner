package de.thm.move.controllers.drawing

import javafx.scene.input.MouseEvent
import javafx.scene.paint.{Color, Paint}
import de.thm.move.controllers.ChangeDrawPanelLike
import de.thm.move.types.*
import de.thm.move.views
import de.thm.move.views.shapes.ResizablePolygon
import de.thm.move.controllers.drawing.ShrinkStrategy




abstract class PolygonStrategy(changeLike: ChangeDrawPanelLike)
    extends PathStrategy(changeLike)
    with ShrinkStrategy:




  var fill: Paint = Color.BLACK
  


  def matchesStartPoint(p: Point): Boolean =
    val (startX, startY) = tmpFigure.getPoints.head
    Math.abs(startX - p.x) <= 10 && Math.abs(startY - p.y) <= 10

  override def setColor(
      fill: Paint,
      stroke: Paint,
      strokeThickness: Int
  ): Unit =
    super.setColor(fill, stroke, strokeThickness)
    // save fill color because a path (the tmpFigure) doesn't have one
    this.fill = fill

  //land lots and zones will do their own registering.
  def registerPolygon(polygon: ResizablePolygon): Unit



  override def dispatchEvent(mouseEvent: MouseEvent): Unit =
    mouseEvent.getEventType match
      case MouseEvent.MOUSE_CLICKED
          if matchesStartPoint(mouseEvent.getX -> mouseEvent.getY) =>
        // create a polygon from the tmpFigure path & copy the colors
        val polygon = ResizablePolygon(tmpFigure.getPoints)
        polygon.copyColors(tmpFigure)
        polygon.setFill(this.fill)
        changeLike.addShapeWithAnchors(polygon)
        //register the new polygon.
        registerPolygon(polygon)
        reset()
      case MouseEvent.MOUSE_CLICKED =>
        pointBuffer += (mouseEvent.getX -> mouseEvent.getY)
        updatePath(pointBuffer.toList)
      case _ => // ignore



