/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.controllers

import de.thm.move.Roads.{RoadType, RoadTypeManager}
import de.thm.move.controllers.drawing.*
import de.thm.move.controllers.factorys.ShapeFactory
import de.thm.move.models
import de.thm.move.models.SelectedShape
import de.thm.move.models.SelectedShape.*
import de.thm.move.views.panes.DrawPanel
import de.thm.move.views.shapes.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.ActionEvent
import javafx.scene.control.TextField
import javafx.scene.input.MouseEvent
import javafx.scene.paint.{Color, Paint}
import javafx.scene.text.Font

import java.net.URI

/** Controller for drawing new shapes or adding existing shapes to the
  * drawPanel.
  */
class DrawCtrl(changeLike: ChangeDrawPanelLike):


  private val drawStrategies = Map(
    SelectedShape.LandLot -> new LandLotStrategy(changeLike),
    SelectedShape.Circle -> new CircleStrategy(changeLike),
    SelectedShape.Path -> new PathStrategy(changeLike),
    SelectedShape.Zone -> new ZoneStrategy(changeLike),
  ) ++ RoadTypeManager.allRoadTypes
    .collect { // add all elements of "allRoadTypes" to the map aswell.
      case (s, t, _, _, _) =>
        s -> new LineStrategy(changeLike, Some(t))
    }

  private val tmpShapeId = DrawPanel.tmpShapeId

  /** The constraint that indicates if the drawing-element should hold their
    * proportions. E.g. a ellipse becomes a circle, a rectangle becomes a square
    */
  val drawConstraintProperty = new SimpleBooleanProperty()

  for (_, strategy) <- drawStrategies do
    strategy.drawConstraintProperty.bind(drawConstraintProperty)

  def getDrawHandler
      : (SelectedShape, MouseEvent) => (Color, Color, Int) => Unit =
    def drawHandler(
        shape: SelectedShape,
        mouseEvent: MouseEvent
    )(fillColor: Color, strokeColor: Color, selectedThickness: Int): Unit =
      //Here the two functions that draw the shape get called!!
      drawStrategies.get(shape).foreach { strategy =>
        shape match {
          case _ if RoadTypeManager.allRoadTypes.exists(_._1 == shape) =>
            val roadType: RoadType = RoadTypeManager.getRoadTypeForShape(shape)
            RoadTypeManager.roadTypeProperties(roadType).headOption match {
              case Some((fill: Paint, stroke: Paint, width: Int)) =>
                strategy.setColor(fill, stroke, width)
              case None =>
                strategy.setColor(fillColor, strokeColor, selectedThickness)
            }
          case _ => strategy.setColor(fillColor, strokeColor, selectedThickness)
        }
        strategy.dispatchEvent(mouseEvent)
      }
    drawHandler

  /** Removes all temporary shapes (identified by temporaryId) from the given
    * node.
    */
  private def removeTmpShapes(temporaryId: String): Unit =
    val removingNodes = changeLike.getElements.filter { n =>
      n.getId == temporaryId
    }
    removingNodes foreach changeLike.remove

  /** Aborts a running drawing-process */
  def abortDrawingProcess(): Unit =
    removeTmpShapes(tmpShapeId)
    drawStrategies.values.foreach(_.reset())

  def drawImage(imgUri: URI): Unit =
    val imgview = ShapeFactory.newImage(imgUri)
    changeLike.addShape(imgview)
    changeLike.addNode(imgview.getAnchors)

  def drawText(x: Double, y: Double, color: Color, font: Font): Unit =
    val text = new TextField()
    text.setId(tmpShapeId)
    text.setOnAction { (_: ActionEvent) =>
      // replace TextField with ResizableText
      changeLike.remove(text)
      val txt = new ResizableText(text.getText, x, y, font)
      txt.setFontColor(color)
      changeLike.addShape(txt)
    }
    text.setLayoutX(x)
    text.setLayoutY(y)
    changeLike.addNode(text)
    text.requestFocus()
