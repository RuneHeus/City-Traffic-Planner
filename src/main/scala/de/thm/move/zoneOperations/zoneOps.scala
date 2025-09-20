package de.thm.move.zoneOperations


import de.thm.move.views.shapes.{ResizableLandLot, ResizablePolygon, ResizableLine, Line}
import de.thm.move.views.anchors.Anchor
import de.thm.move.controllers.drawing.ShrinkStrategy

/**
* Trait that delivers functions that are used for zones.
* */
trait overlapAfterMove extends ShrinkStrategy:

/**
 * @param zoneAnchors
 * anchors of the zone
 * @return
 * A zone if the zone is found based on the given anchors, otherwise it returns nil.
 *
 *
 * */
  private def lookForZoneBasedOnAnchors(zoneAnchors: List[Anchor]): Option[ResizablePolygon] =
    var resultPolygon: Option[ResizablePolygon] = None
    ResizablePolygon.polygons.foreach((zone: ResizablePolygon) =>
      if zone.getAnchors == zoneAnchors then resultPolygon = Some(zone)
    )
    resultPolygon

  /**
   * @param zoneAnchors
   * anchors of the zone
   * @return
   * Unit. This function will draw shrink zones when a zone that is moved overlaps with other zones.
   *
   *
   * */
  def checkEditedZones(zoneAnchors: List[Anchor]): Unit =
    lookForZoneBasedOnAnchors(zoneAnchors) match{
      case Some(currentZone: ResizablePolygon) => {
        val otherZones = ResizablePolygon.polygons.filter((otherPolygon: ResizablePolygon) =>
          otherPolygon != currentZone
        )
        otherZones.foreach((otherZone: ResizablePolygon) =>
          val overlappingPolygons = checkOverlappingPolygonsDeprecated(List(otherZone), currentZone)
          overlappingPolygons.foreach((overlappedZone: ResizablePolygon) =>
            constructNewOldPolygon(overlappedZone, currentZone)
          )
        )
      }
      case None => //if the current zone is not found do nothing.
    }