package de.thm.move.controllers.zones


/**
 * Object to handle current selection of zones. When a user clicks on the type of zone, the current type is updated.
 * The current type is used when a new zone is constructed.
 * */
object zoneTypeManager:
  private var currentType = "residential"

  def changeZoneType(newType: String): Unit =
    currentType = newType

  def getZoneType: String =
    currentType