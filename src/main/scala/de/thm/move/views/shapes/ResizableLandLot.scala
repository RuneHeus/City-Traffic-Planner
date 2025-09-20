package de.thm.move.views.shapes

import de.thm.move.Roads.{Road, RoadNode}
import de.thm.move.types.Point
import de.thm.move.views.anchors.Anchor


/***.
 *
 * @param points
 * points used to construct the land lot.
 * @inheritdoc
 * Subclass of ResizablePolygon. Will only be allowed to have 4 anchors in the drawing process.
 */
class ResizableLandLot(override val points: List[Double]) extends ResizablePolygon(points):
  private var typeOfLandLot: String = "none"
  private var road: Road = new Road("none", new RoadNode("null"), new RoadNode("null2"))

  def changeType(newType: String): Unit =
    typeOfLandLot = newType


  def getType: String =
    typeOfLandLot

  def registerRoad(newRoad: Road): Unit =
    this.road = newRoad

  def getRoad: Road = road

/**
 * Companion object of resizable land lots. Used to manage the land lots present in the grid.
 * */
object ResizableLandLot:

  private var landLots : List[ResizablePolygon] = List()

  private var onUnregisterCallback: Option[ResizablePolygon => Unit] = None
  private var onRegisterCallback: Option[ResizablePolygon => Unit] = None

  def setOnUnregisterCallback(callback: ResizablePolygon => Unit): Unit =
    onUnregisterCallback = Some(callback)

  def setonRegisterCallback(callback: ResizablePolygon => Unit): Unit =
    onRegisterCallback = Some(callback)

  def registerLandLot(landLot: ResizablePolygon): Unit = {
    if (!isRegistered(landLot)) then // Check if the land lot is already registered
      landLots = landLots.::(landLot)
      onRegisterCallback.foreach(_.apply(landLot))
  }

  def unregisterLandLot(landLot: ResizablePolygon): Unit =
    landLots = landLots.filter((lot: ResizablePolygon) =>
      lot != landLot
    )
    onUnregisterCallback.foreach(_.apply(landLot))

  def clearLandLots(): Unit =
    this.landLots = List()

  def getLots: List[ResizablePolygon] = landLots

  def isRegistered(landLot: ResizablePolygon): Boolean =
    landLots.contains(landLot)

