package de.thm.move.controllers

import de.thm.move.Global.CurrentSelectedShape
import de.thm.move.Roads.{OneWayLabel, RoadManager, RoadType, RoadTypeManager}
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.ChoiceBox
import de.thm.move.Roads.{RoadManager, RoadType, RoadTypeManager}
import de.thm.move.views.shapes.ResizableLine
import javafx.collections.FXCollections
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.{ChoiceBox, Label}
import javafx.scene.paint.Paint

import scala.jdk.CollectionConverters.*
import java.net.URL
import java.util.ResourceBundle
import scala.jdk.CollectionConverters.*

class RoadToolbarCtrl extends Initializable {
  @FXML
  var oneWayDirectionLabel: Label = _
  @FXML
  var roadTypeChooser: ChoiceBox[RoadType] = _
  private var selectionCtrl: SelectedShapeCtrl = _

  /** Simple initialization that calls setupRoadTypeChooser
    */
  override def initialize(location: URL, resources: ResourceBundle): Unit = {
    setupRoadTypeChooser()
  }

  /** Set up the road type chooser, and set Normal as the default value.
      Adds a listener for handleRoadTypeChange so the road type can be changed.
    */
  def setupRoadTypeChooser(): Unit =
    roadTypeChooser.setItems(FXCollections.observableArrayList(RoadType.values.toSeq.asJava))
    roadTypeChooser.setValue(RoadType.Normal)
    roadTypeChooser.getSelectionModel.selectedItemProperty().addListener((_, _, newRoadType) => handleRoadTypeChange(newRoadType))

  /** Handles the road-type choice box by setting the right label + visually changing the line
   *
   * @param newRoadType the new road type we want to use for the current selected line
   * */
  def handleRoadTypeChange(newRoadType: RoadType): Unit = {
    RoadTypeManager.roadTypeProperties(newRoadType).headOption match {
      case Some((fill: Paint, stroke: Paint, width: Int)) =>
        CurrentSelectedShape match {
          case shape: ResizableLine =>
            RoadManager.getRoadProperties(shape.getId).roadType = newRoadType
            shape.setStrokeColor(stroke)
            shape.setFillColor(fill)
            if (RoadManager.getRoadProperties(shape.getId).one_way == OneWayLabel.None) then shape.setStrokeWidth(width)
          case _ =>
        }
      case None =>
    }
  }

  def selectedRoadType: RoadType =
    roadTypeChooser.getValue

  /**
   * Calls ToggleOneWay with the test parameter set to false. Called whenever the user clicks on the OneWayButton
   */
  def OneWayAction(): Unit = {
    ToggleOneWay(false)
  }
  /** Handles the one-way button by toggling between the three one-way labels whenever the button is clicked. The label is changed + the stroke width.
   * @param test indicates if the function is used in a test or not. If it is true then setOneWay does not get called, since this causes errors in
   *             the test and it unnecessary for them anyway.
   */
  def ToggleOneWay(test: Boolean): Unit = {
    CurrentSelectedShape match {
      case road: ResizableLine =>
        RoadManager.getRoadProperties(road.getId).one_way match {
          case OneWayLabel.None =>
            RoadManager.getRoadProperties(road.getId).one_way = OneWayLabel.Front
            road.setStrokeWidth(RoadManager.oneWayThickness)
          case OneWayLabel.Front => RoadManager.getRoadProperties(road.getId).one_way = OneWayLabel.Back
            road.setStrokeWidth(RoadManager.oneWayThickness)
          case OneWayLabel.Back =>
            RoadManager.getRoadProperties(road.getId).one_way = OneWayLabel.None
            road.setStrokeWidth(RoadTypeManager.roadTypeProperties(RoadManager.getRoadProperties(road.getId).roadType).head._3)
          case _ =>
        }
        if !test then setOneWay()
      case _ =>
    }
  }

  /**
   Changes the One Way label in the GUI. It is automatically called by ToggleOneWay if its "test" parameter is set to False.
   **/
  def setOneWay(): Unit = {
    CurrentSelectedShape match {
      case road: ResizableLine =>
        oneWayDirectionLabel.setText(RoadManager.getRoadProperties(road.getId).one_way.toString)
      case _ =>
        oneWayDirectionLabel.setText("N/A")
    }
  }
}



