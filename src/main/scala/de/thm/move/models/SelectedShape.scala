package de.thm.move.models

object SelectedShape extends Enumeration {
  type SelectedShape = Value
  val Rectangle, Path, Circle, Polygon, Zone, LandLot, Text, RoadNormal, RoadDouble, RoadUnpaved = Value
}