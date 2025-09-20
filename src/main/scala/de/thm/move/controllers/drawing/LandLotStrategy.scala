package de.thm.move.controllers.drawing

import de.thm.move.Roads.RoadManager
import de.thm.move.controllers.ChangeDrawPanelLike
import de.thm.move.views.anchors.Anchor
import de.thm.move.views.shapes.*
import javafx.scene.input.MouseEvent
import de.thm.move.Roads.Road
import de.thm.move.history.History
import de.thm.move.util.JFxUtils.withConsumedEvent
class LandLotStrategy(changeLike: ChangeDrawPanelLike) extends PolygonStrategy(changeLike) with lotContained with snapToRoad:
  private var deltaX = -1.0
  private var deltaY = -1.0


  override def registerPolygon(polygon: ResizablePolygon): Unit =
    ResizableLandLot.registerLandLot(polygon)

  ResizableLandLot.setOnUnregisterCallback((landLot: ResizablePolygon) =>
    changeLike.removeShape(landLot)
  )
  ResizableLandLot.setonRegisterCallback((landLot: ResizablePolygon) =>
    changeLike.addShapeWithAnchors(landLot)
  )


  override def dispatchEvent(mouseEvent: MouseEvent): Unit =
    mouseEvent.getEventType match
      case MouseEvent.MOUSE_CLICKED
        if matchesStartPoint(mouseEvent.getX -> mouseEvent.getY) =>
          val lot = ResizableLandLot(tmpFigure.getPoints.flatMap((point: (Double, Double)) => List(point._1, point._2)))
          //land lots have to be rectangular, so they can only have 4 points and all four points have to be in a zone, and they can not overlap with another lot.
          if(tmpFigure.getPoints.length == 4 &&
            landLotInZone(lot, ResizablePolygon.polygons) &&
            checkOverlappingPolygonsDeprecated(ResizableLandLot.getLots, lot).isEmpty &&
            !checkOverlappingWithOwnEdges(lot)
            )
            then  // create a polygon from the tmpFigure path & copy the colors
              val roads: List[Road] = RoadManager.roads.map((f: (String, Road)) => List(f._2)).toList.flatten
              val snapped = checkCloseEnough(lot, ResizableLine.allLines.toList)
              if(snapped._1)
                then
                  val road = snapped._2
                  road.registerLandLot(lot)
                  lot.registerRoad(road)

                  lot.copyColors(tmpFigure)
                  lot.setFill(this.fill)
                  //register the new polygon.
                  registerPolygon(lot)
                  reset()
              else
                reset()
          else
            reset()

      case MouseEvent.MOUSE_CLICKED =>
        pointBuffer += (mouseEvent.getX -> mouseEvent.getY)
        updatePath(pointBuffer.toList)
      case _ => // ignore
