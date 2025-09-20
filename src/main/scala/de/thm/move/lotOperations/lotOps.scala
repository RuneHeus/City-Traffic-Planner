package de.thm.move.lotOperations

import de.thm.move.views.shapes.{ResizableLandLot, ResizablePolygon, ResizableLine, Line}
import de.thm.move.controllers.drawing.lotContained
import de.thm.move.Roads.RoadManager
import de.thm.move.views.anchors.Anchor


trait checkLandLotsOutsideZone extends lotContained:

/**
 * @return
 * Boolean that signals if a land lot is outside a zone. 
 * */
  def checkLandLotsOutside(): Boolean =
    var deleted = false
    ResizableLandLot.getLots.foreach((lot: ResizablePolygon) =>
      if(!landLotInZone(lot, ResizablePolygon.polygons))
      then
        ResizableLandLot.unregisterLandLot(lot)
        lot.asInstanceOf[ResizableLandLot].getRoad.unregisterLandLot(lot)
        val lotAnchors = lot.getAnchors
        for(i <- lotAnchors.indices)
          lot.resize(i, (-lotAnchors(i).getCenterX, -lotAnchors(i).getCenterY))
          deleted = true
    )
    deleted



trait moveLotWithRoad:
  /**
   * @param lots
   * All land lots connected to the road.
   * @param delta
   * Delta x and y values of the change in the grid of the road.
   * @param line
   * The Resizable:ine of the road.
   * @return
   * Nothing. It snaps the land lots to the roads when the road moves.
   * */
  def moveWithLine(lots: List[ResizablePolygon], delta: (Double, Double), line: ResizableLine): Unit =

    lots.foreach((lot: ResizablePolygon) => {

      val anchors = lot.getAnchors
      var amountSnapped = 0;
      for (i <- lot.getAnchors.indices)
        val x1 = line.getStartX
        val y1 = line.getStartY
        val x2 = line.getEndX
        val y2 = line.getEndY


        //new y coordinate of the line at the x coordinate of the current anchor. Used if the anchor is currently snapped to the road.
        val lineCopy = new Line(x1, y1, x2, y2)
        val y = anchors(i).getCenterX * lineCopy.slope + lineCopy.b


        //if the anchor is snapped to the road,  use the y coordinate to snap it back. Else do not change in y value, only in x value.
        if (Math.abs(anchors(i).getCenterY - y) <= 50 && amountSnapped < 2)
        then
          lot.resize(i, (delta._1, y - anchors(i).getCenterY))
          amountSnapped += 1
        else
          lot.resize(i, (delta._1, 0.0))
    }
    )



trait assignLotsToSplitRoad:

  /**
   * @param landLots
   * All land lots connected to the road.
   * @param oldRoadLine
   * Line of the road that got split in two.
   * @param newRoadLine1
   * Line of the new left part of the intersection.
   * @param newRoadLine2
   * Line of the new right part of the intersection.
   * @param split
   * Coordinates of the intersection point.
   * @return
   * Nothing. It assigns the new roads their correct land lots.
   * */
  def assignLots(landLots: List[ResizablePolygon], oldRoadLine: ResizableLine, newRoadLine1: ResizableLine, newRoadLine2: ResizableLine, split: (Double, Double)): Unit =
    //bases on x coordinate, assign land lots to new roads.
    landLots.foreach((lot: ResizablePolygon) =>
      var toTheLeft = 0
      val oldRoad = RoadManager.getRoad(oldRoadLine.getId)
      val newRoad1 = RoadManager.getRoad(newRoadLine1.getId)
      val newRoad2 = RoadManager.getRoad(newRoadLine2.getId)
      lot.getAnchors.foreach((anchor: Anchor) =>




        //get the line equation to determine if the anchor is close to the road at a certain x coordinate.
        val x1 = oldRoadLine.getAnchors.head.getCenterX
        val y1 = oldRoadLine.getAnchors.head.getCenterY
        val x2 = oldRoadLine.getAnchors.tail.head.getCenterX
        val y2 = oldRoadLine.getAnchors.tail.head.getCenterY
        val line = new Line(x1, y1, x2, y2)

        val y = line.slope * anchor.getCenterX + line.b
        //if the two anchors that are snapped to the road are to the left of the split, assign the land lot to the left split.
        if (Math.abs(anchor.getCenterY - y) <= 20 && anchor.getCenterX < split._1)
        then toTheLeft += 1
      )
      if (toTheLeft == 2)
      then
        Console.println("assigned to the left")
        oldRoad.get.unregisterLandLot(lot)
        newRoad1.get.registerLandLot(lot)
      else
        Console.println("assigned to the right")
        oldRoad.get.unregisterLandLot(lot)
        newRoad2.get.registerLandLot(lot)
    )

