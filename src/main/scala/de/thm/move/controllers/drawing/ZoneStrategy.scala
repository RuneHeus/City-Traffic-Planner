package de.thm.move.controllers.drawing

import de.thm.move.controllers.ChangeDrawPanelLike
import de.thm.move.types.Point
import de.thm.move.views.shapes.ResizablePolygon
import de.thm.move.views.shapes.ResizableLandLot
import javafx.scene.input.MouseEvent
import javafx.scene.paint.{Color, Paint}
import de.thm.move.controllers.ChangeDrawPanelLike
import de.thm.move.types.*
import de.thm.move.views
import de.thm.move.views.shapes.ResizablePolygon
import de.thm.move.controllers.drawing.ShrinkStrategy
import de.thm.move.controllers.zones.zoneTypeManager

class ZoneStrategy(changeLike: ChangeDrawPanelLike) extends PolygonStrategy(changeLike) with lotContained:

  private def removeLot(lot: ResizablePolygon): Unit =
    ResizableLandLot.unregisterLandLot(lot)



  override def registerPolygon(polygon: ResizablePolygon): Unit =
    ResizablePolygon.registerPolygon(polygon)

  ResizablePolygon.setOnUnregisterCallback((polygon: ResizablePolygon) =>
    changeLike.removeShape(polygon)
  )
  ResizablePolygon.setonRegisterCallback((polygon: ResizablePolygon) =>
    changeLike.addShapeWithAnchors(polygon)
  )

  override def dispatchEvent(mouseEvent: MouseEvent): Unit =
    mouseEvent.getEventType match
      case MouseEvent.MOUSE_CLICKED
        if matchesStartPoint(mouseEvent.getX -> mouseEvent.getY) =>
        // create a polygon from the tmpFigure path & copy the colors
        val polygon = ResizablePolygon(tmpFigure.getPoints)
        if(!checkOverlappingWithOwnEdges(polygon))
        then {
          //check for overlapping polygons, but do this before constructing the new one so that it is not included in the check.
          val overlappingPolygons: List[ResizablePolygon] = checkOverlappingPolygonsDeprecated(ResizablePolygon.polygons, polygon)

          overlappingPolygons.foreach((oldPolygon: ResizablePolygon) =>
            constructNewOldPolygon(oldPolygon, polygon)
          )

          //check if after shrinking all land lots are still fully contained in a zone, but they can not be in two zones at once.
          ResizableLandLot.getLots.foreach((lot: ResizablePolygon) =>
            if!(landLotInZone(lot, ResizablePolygon.polygons))
            then removeLot(lot)
          )
          polygon.copyColors(tmpFigure)
          polygon.setFill(this.fill)
          registerPolygon(polygon)
          //register the new polygon.

          polygon.toBack()
          reset()
        }
        else reset()
      case MouseEvent.MOUSE_CLICKED =>
        pointBuffer += (mouseEvent.getX -> mouseEvent.getY)
        updatePath(pointBuffer.toList)
      case _ => // ignore