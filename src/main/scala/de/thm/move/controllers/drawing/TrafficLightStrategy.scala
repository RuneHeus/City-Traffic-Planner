package de.thm.move.controllers.drawing

import de.thm.move.Global
import de.thm.move.views.anchors.SharedAnchor
import de.thm.move.views.panes.DrawPanel
import javafx.scene.text.{Font, Text}
import javafx.scene.paint.Color
import de.thm.move.Roads.RoadManager
import scala.collection.mutable

/**
 * Manages traffic lights associated with anchors in the drawing panel.
 *
 * @param drawPanel The drawing panel where traffic lights are displayed.
 */
class TrafficLightStrategy(drawPanel: DrawPanel):

  /** A map to store listeners for each anchor that has a TrafficLight. */
  private val listenerMap:mutable.Map[SharedAnchor, () => Unit] = mutable.Map.empty


  /**
   * Adds a traffic light to the specified anchor.
   *
   * This method creates a traffic light icon, displays it on the draw panel, and associates it with the anchor.
   * A listener is also added to remove the traffic light if the anchor no longer meets the required conditions.
   * Updates the Anchor in the RoadManager.
   *
   * @param anchor The anchor to which the traffic light is to be added.
   * @throws IllegalArgumentException if the anchor is null, does not meet the conditions
   *                                  (e.g., fewer than 3 connections), or already has a traffic light.
   */
  def addTrafficLight(anchor: SharedAnchor): Unit =
    require(anchor != null, "Anchor must not be null")
    require(canHaveTrafficLight(anchor), s"Anchor ${anchor.getId} does not meet the conditions for a traffic light")
    require(anchor.getTrafficLight.isEmpty, s"Anchor ${anchor.getId} already has a traffic light")

    val icon = createTrafficLightIcon()
    drawPanel.getChildren.add(icon)
    anchor.setTrafficLight(icon)
    bindTrafficLightToAnchor(anchor, icon)
    RoadManager.updateTrafficLightStatus(anchor.getId, trafficLight = true)

    // Create and store the listener
    val listener: () => Unit = () => {
      if anchor.connectedLines.size < 3 && anchor.getTrafficLight.isDefined then
        removeTrafficLight(anchor)
    }
    listenerMap(anchor) = listener
    anchor.addConnectionChangeListener(listener)

    assert(drawPanel.getChildren.contains(icon), "Traffic light icon not added to draw panel")
    assert(anchor.getTrafficLight.contains(icon), s"Traffic light not correctly set on the anchor ${anchor.getId}")
    assert(listenerMap.contains(anchor), "Listener was not added for the anchor")

  /**
   * Removes the traffic light from the specified anchor.
   *
   * This method removes the traffic light icon from the draw panel, detaches associated listeners,
   * and updates the traffic light status in the `RoadManager`.
   *
   * @param anchor The anchor from which the traffic light is to be removed.
   * @throws IllegalArgumentException if the anchor is null.
   */
  def removeTrafficLight(anchor: SharedAnchor): Unit =
    require(anchor != null, "Anchor must not be null")

    val trafficLightOpt = anchor.getTrafficLight

    // Remove the traffic light icon if present
    trafficLightOpt.foreach { trafficLight =>
      drawPanel.getChildren.remove(trafficLight)
      anchor.removeTrafficLight()
    }

    // Always remove the listener from the anchor if there is one
    listenerMap.get(anchor).foreach { listener =>
      anchor.removeConnectionChangeListener(listener)
      listenerMap -= anchor
      println(s"Listener removed for anchor: ${anchor.getId}")
    }

    RoadManager.updateTrafficLightStatus(anchor.getId, trafficLight = false)
    assert(anchor.getTrafficLight.isEmpty, s"Traffic light was not removed from anchor ${anchor.getId}")
    assert(!listenerMap.contains(anchor), s"Listener was not removed for anchor ${anchor.getId}")
    assert(trafficLightOpt.forall(icon => !drawPanel.getChildren.contains(icon)),
      s"Traffic light was not removed from DrawPanel for anchor ${anchor.getId}")


  /**
   * Checks if a traffic light can be added to the specified anchor.
   *
   * @param anchor The anchor to check.
   * @return True if the anchor has at least 3 connections, false otherwise.
   * @throws IllegalArgumentException if the anchor is null.
   */
  def canHaveTrafficLight(anchor: SharedAnchor): Boolean =
    require(anchor != null, "Anchor must not be null")
    anchor.connectedLines.size >= 3



  /**
   * Creates a traffic light icon for use in the drawing panel.
   *
   * @return A new Text object representing the traffic light icon.
   */
  private def createTrafficLightIcon(): Text =
    val iconUnicode = Global.fontBundle.getString("fa.traffic-light")
    val icon = new Text(iconUnicode)
    icon.setFont(Font.font("FontAwesome", 20))
    icon.setFill(Color.PEACHPUFF)
    icon.setStroke(Color.RED)
    icon.setStrokeWidth(1)

    icon

  /**
   * Binds the position of the traffic light icon to the position of the anchor.
   *
   * @param anchor The anchor to which the traffic light is bound.
   * @param icon   The traffic light icon to bind.
   */
  private def bindTrafficLightToAnchor(anchor: SharedAnchor, icon: Text): Unit =
    def updatePosition(): Unit =
      val bounds = anchor.localToParent(anchor.getBoundsInLocal)
      icon.setLayoutX(bounds.getMinX + 10)
      icon.setLayoutY(bounds.getMinY + 10)

    updatePosition()
    anchor.addDynamicListener(() => updatePosition())