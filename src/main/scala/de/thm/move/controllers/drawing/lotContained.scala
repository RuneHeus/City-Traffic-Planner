package de.thm.move.controllers.drawing

import scala.jdk.CollectionConverters._
import de.thm.move.views.shapes.ResizablePolygon
import de.thm.move.views.anchors.Anchor

/**
 * Trait that delivers a function to check if a land lot is inside a zone.
 * */
trait lotContained extends ShrinkStrategy:
  /**
   * @param
   * land lot for which will be checked if it is inside a zone or not.
   * @param
   * all the existing zones.
   * @return
   * A boolean that signals if a land lot present in one zone.
   * */
  def landLotInZone(landLot: ResizablePolygon, zones: List[ResizablePolygon]): Boolean =
    val lotPoints = landLot.getAnchors
    var allInside = false
    for(zone <- zones)
      if(!(allInside))
        then
        /*
          for each point check if it is in the lot. This is done by counting how many points are in the lot, if that amount if the same as
          the total amount of points the condition is true.
        */
          val amountOfPoints = lotPoints.length
          var amountOfInternalPoints = 0
          lotPoints.foreach((point: Anchor) =>
            if(pointInPolygon(getEdges(zone.getAnchors), point))
              then amountOfInternalPoints += 1
          )
          if(amountOfInternalPoints == amountOfPoints)
            then
              allInside = true

    allInside


