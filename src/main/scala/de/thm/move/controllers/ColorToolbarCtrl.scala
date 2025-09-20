package de.thm.move.controllers

import java.net.URL
import java.util.ResourceBundle
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.*
import javafx.collections.FXCollections
import javafx.scene.paint.Color
import de.thm.move.config.ValueConfig
import de.thm.move.Global
import de.thm.move.models.LinePattern.LinePattern
import de.thm.move.models.FillPattern.FillPattern
import de.thm.move.views.dialogs.Dialogs
import de.thm.move.models.{FillPattern, LinePattern, SelectedShape}

import scala.jdk.CollectionConverters.*
import de.thm.move.Roads.RoadType
import de.thm.move.util.JFxUtils.onChoiceboxChanged
import javafx.event.ActionEvent

import de.thm.move.controllers.zones.zoneTypeManager

/** Controller voor de kleurwerkbalk die zones vertegenwoordigt. */
class ColorToolbarCtrl extends Initializable:

  private val residentialColor: Color = Color.BLUE
  private val commercialColor: Color = Color.RED
  private val industrialColor: Color = Color.GRAY

  @FXML var zoneChooser: ChoiceBox[String] = _

  @FXML
  var roadTypeChooser: ChoiceBox[RoadType] = _

  @FXML
  var strokeColorLabel: Label = _
  @FXML
  var fillColorPicker: ColorPicker = _
  @FXML
  var fillColorLabel: Label = _
  @FXML
  var strokeColorPicker: ColorPicker = _
  @FXML
  var linePatternChooser: ChoiceBox[LinePattern] = _
  @FXML
  var fillPatternChooser: ChoiceBox[FillPattern] = _
  @FXML
  var borderThicknessChooser: ChoiceBox[Int] = _

  private val fillColorConfig = new ValueConfig(Global.fillColorConfigURI)
  private val strokeColorConfig = new ValueConfig(Global.strokeColorConfigURI)
  private var selectionCtrl: SelectedShapeCtrl = _


  override def initialize(location: URL, resources: ResourceBundle): Unit =
    //setupDefaultColors()
    setupPattern()
    val sizesList: java.util.List[Int] = (1 until 20).asJava //asJava so the GuiTest in the pipeline work.
    borderThicknessChooser.setItems(
      FXCollections.observableArrayList(sizesList)
    )
    setupZoneOptions()
    setupBorderThicknessOptions() // Stel de border-thickness-opties in

  def postInitialize(selectionCtrl: SelectedShapeCtrl): Unit =
    this.selectionCtrl = selectionCtrl

    onChoiceboxChanged(borderThicknessChooser)(
      this.selectionCtrl.setStrokeWidth
    )
    onChoiceboxChanged(linePatternChooser)(this.selectionCtrl.setStrokePattern)
    onChoiceboxChanged(fillPatternChooser)(this.selectionCtrl.setFillPattern)

  def shutdown(): Unit =
    fillColorConfig.saveConfig()
    strokeColorConfig.saveConfig()

  @FXML def colorPickerChanged(ae: ActionEvent): Unit =
    val src = ae.getSource

    if src == strokeColorPicker then
      selectionCtrl.setStrokeColor(withCheckedColor(strokeColorPicker.getValue))
    else if src == fillColorPicker then
      selectionCtrl.setFillColor(withCheckedColor(fillColorPicker.getValue))

  /** Checks that the color has a valid opacity and if not warns the user. */
  private def withCheckedColor(c: Color): Color =
    val opacity = c.getOpacity()
    val opacityPc = opacity * 100
    if opacity != 1.0 && opacity != 0.0 then
      Dialogs
        .newWarnDialog(
          f"The given color has a opacity of $opacityPc%2.0f which modelica can't display.\n" +
            "Colors in modelica can have 2 opacitys: either 100% or 0%"
        )
        .showAndWait()

    c
  


  private def setupPattern(): Unit =
    val linePatterns = LinePattern.values.toList.asJava
    linePatternChooser.setItems(FXCollections.observableList(linePatterns))
    linePatternChooser.setValue(LinePattern.Solid) // Standaard keuze

    // Voeg fill patterns toe aan fillPatternChooser
    val fillPatterns = FillPattern.values.toList.asJava
    fillPatternChooser.setItems(FXCollections.observableList(fillPatterns))
    fillPatternChooser.setValue(FillPattern.Solid) // Standaard keuze

    linePatternChooser.getSelectionModel.selectedItemProperty().addListener((_, _, newPattern) => {
      selectionCtrl.setStrokePattern(newPattern)
    })

    fillPatternChooser.getSelectionModel.selectedItemProperty().addListener((_, _, newPattern) => {
      selectionCtrl.setFillPattern(newPattern)
    })



  private def setupZoneOptions(): Unit = {
    zoneChooser.setItems(
      FXCollections.observableArrayList("Residential", "Commercial", "Industrial")
    )
    zoneChooser.setValue("Residential")

    zoneChooser.getSelectionModel.selectedItemProperty().addListener((_, _, newValue) => {
      newValue match {
        case "Residential" => {
          selectionCtrl.setFillColor(residentialColor)
          zoneTypeManager.changeZoneType("residential")
        }
        case "Commercial" => {
          selectionCtrl.setFillColor(residentialColor)
          zoneTypeManager.changeZoneType("commercial")
        }
        case "Industrial" => {
          selectionCtrl.setFillColor(industrialColor)
          zoneTypeManager.changeZoneType("industrial")
        }
        case _ =>
      }
    })
  }



  private def setupBorderThicknessOptions(): Unit = {
    val thicknessOptions = FXCollections.observableArrayList((1 to 20).asJava)
    borderThicknessChooser.setItems(thicknessOptions)
    borderThicknessChooser.setValue(1) // Stel een standaardwaarde in

    borderThicknessChooser.getSelectionModel.selectedItemProperty().addListener((_, _, newThickness) => {
      selectionCtrl.setStrokeWidth(newThickness)
    })
  }

  def getFillColor: Color = zoneChooser.getValue match {
    case "Residential" => residentialColor
    case "Commercial" => commercialColor
    case "Industrial" => industrialColor
    case _ => Color.GRAY
  }

  def selectedThickness: Int = borderThicknessChooser.getSelectionModel.getSelectedItem