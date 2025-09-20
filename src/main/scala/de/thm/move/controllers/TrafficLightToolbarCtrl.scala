package de.thm.move.controllers

import de.thm.move.controllers.drawing.TrafficLightStrategy
import de.thm.move.views.anchors.SharedAnchor
import javafx.fxml.FXML
import javafx.event.ActionEvent
import javafx.scene.control.Button

/**
 * Controller for the Traffic Light toolbar.
 *
 * This class manages the interaction between the UI button and the TrafficLightStrategy,
 * allowing users to add or remove traffic lights on selected anchors.
 */
class TrafficLightToolbarCtrl:
  /** The strategy responsible for managing traffic lights. */
  private var trafficLightStrategy: TrafficLightStrategy = _

  /** The controller responsible for managing selected shapes or SharedAnchors. */
  private var selectionCtrl: SelectedShapeCtrl = _

  /** The button used for adding or removing traffic lights. */
  @FXML
  private var addRemoveTrafficLightButton: Button = _

  /**
   * Sets the TrafficLightStrategy to be used by this controller.
   *
   * @param strategy The TrafficLightStrategy instance.
   */
  def setTrafficLightStrategy(strategy: TrafficLightStrategy): Unit =
    trafficLightStrategy = strategy

  /**
   * Sets the SelectedShapeCtrl to manage selected anchors.
   *
   * @param selecCtrl The SelectedShapeCtrl instance.
   */
  def setSelectionCtrl(selecCtrl: SelectedShapeCtrl): Unit =
    selectionCtrl = selecCtrl

  /**
   * Updates the state and behavior of the traffic light button based on the selected anchors.
   *
   * This method checks the current selection and updates the button's text,
   * enabled/disabled state, and action accordingly:
   *
   *  - If a traffic light exists on the selected anchor, the button allows removal.
   *  - If a traffic light can be added, the button allows addition.
   *  - If no anchor is selected or the selected anchor does not meet the conditions,
   *    the button is disabled.
   *
   * @note This method dynamically updates the button based on the '''first selected anchor'''.
   *       If multiple anchors are selected, only the first anchor is considered.
   */
  def updateTrafficLightButton(): Unit =
    selectionCtrl.getSelectedAnchors.headOption match
      case Some(anchor) if anchor.getTrafficLight.isDefined =>  // Traffic light exists; allow removal
        addRemoveTrafficLightButton.setText("Remove Traffic Light")
        addRemoveTrafficLightButton.setDisable(false)
        addRemoveTrafficLightButton.setOnAction { _ =>
          selectionCtrl.getSelectedAnchors.foreach(trafficLightStrategy.removeTrafficLight)
          updateTrafficLightButton()
        }

      case Some(anchor) if trafficLightStrategy.canHaveTrafficLight(anchor) => // No traffic light exists but valid for addition
        addRemoveTrafficLightButton.setText("Add Traffic Light")
        addRemoveTrafficLightButton.setDisable(false)
        addRemoveTrafficLightButton.setOnAction { _ =>
          selectionCtrl.getSelectedAnchors.foreach(trafficLightStrategy.addTrafficLight)
          updateTrafficLightButton()
        }

      case Some(_) => // Less than 3 connections; cannot add traffic light
        addRemoveTrafficLightButton.setText("Cannot Add Traffic Light (Needs 3 Connections)")
        addRemoveTrafficLightButton.setDisable(true)

      case None => // No anchor selected
        addRemoveTrafficLightButton.setText("No Anchor Selected")
        addRemoveTrafficLightButton.setDisable(true)