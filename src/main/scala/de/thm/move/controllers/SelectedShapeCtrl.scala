/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.controllers

import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import de.thm.move.Global.*
import de.thm.move.views.anchors.SharedAnchor

import de.thm.move.Roads.RoadManager
import de.thm.move.controllers.factorys.ShapeFactory
import de.thm.move.types.*
import de.thm.move.views.*
import de.thm.move.views.panes.{DrawPanel, SnapLike}
import de.thm.move.views.shapes.*
import de.thm.move.lotOperations.checkLandLotsOutsideZone
import de.thm.move.controllers.drawing.lotContained

/** Controller for selected shapes. Selected shapes are highlighted by a dotted
  * black border around the bounding-box.
  */
class SelectedShapeCtrl(changeLike: ChangeDrawPanelLike, grid: SnapLike)
    extends SelectionCtrlLike
    with SelectedTextCtrl
    with SelectedMoveCtrl
    with ColorizeSelectionCtrl
    with checkLandLotsOutsideZone
    with lotContained:

  val addSelectedShapeProperty = new SimpleBooleanProperty(false)

  private var selectedShapes: List[ResizableShape] = Nil

  /** The list of currently selected anchors. */
  private var selectedAnchors: List[SharedAnchor] = Nil

  override def getSelectedShapes: List[ResizableShape] = selectedShapes


  /**
   * Returns the currently selected anchors.
   *
   * @return A list of `SharedAnchor` objects currently selected.
   */
  def getSelectedAnchors:List[SharedAnchor] = selectedAnchors
  override def getSnapLike: SnapLike = grid

  private def getSelectionGroups: List[GroupLike] =
    def findGroups(xs: List[ResizableShape]): List[GroupLike] =
      xs flatMap {
        case g: GroupLike => g :: findGroups(g.childrens)
        case _            => Nil
      }

    findGroups(selectedShapes)

  /**
   * Sets the given anchor as the selected anchor, replacing any previously selected shapes or anchors.
   *
   * @param anchor The `SharedAnchor` to select.
   */
  def setSelectedAnchor(anchor: SharedAnchor): Unit =
    unselectShapes() // Unselect any currently selected shapes
    unselectAnchors() // Unselect any currently selected anchors
    selectedAnchors = List(anchor)

    anchor.showSelectionRectangle() // Show the selection rectangle`
    changeLike.addNode(anchor.selectionRectangle) // Add it to the DrawPanel

  /**
   * Deselects all currently selected anchors.
   *
   * This method hides the selection rectangles of all selected anchors and
   * removes them from the drawing panel.
   */
  def unselectAnchors(): Unit =
    for anchor <- selectedAnchors do
      anchor.hideSelectionRectangle() // Hide the rectangle
      changeLike.remove(anchor.selectionRectangle) // Remove from DrawPanel
      anchor.setStyle("") // Remove visual indication
    selectedAnchors = Nil

  /**
   * Deletes the currently selected anchor(s) along with their connected elements.
   *
   * This method:
   *  - Removes any traffic lights associated with the anchor.
   *  - Deletes lines connected to the anchor.
   *  - Unregisters associated land lots.
   *  - Removes the anchor itself.
   */
  def deleteSelectedAnchor(): Unit =
    if selectedAnchors.nonEmpty then
      val anchorCopy = selectedAnchors
      val connectedLines = anchorCopy.flatMap(_.connectedLines.map(_._1)) // Get lines connected to the SharedAnchors

      connectedLines.foreach((line: ResizableLine) =>
        RoadManager.getRoad(line.getId).get.getLots.foreach((landLot: ResizablePolygon) =>
          ResizableLandLot.unregisterLandLot(landLot)
        )
      )
      // Remove connected lines
      connectedLines.foreach { line =>
          changeLike.removeShape(line) // Remove the line, which manages anchor connections automatically
      }

      // Remove anchors TrafficLights
      anchorCopy.foreach { anchor =>
        anchor.getTrafficLight.foreach { trafficLight =>
          changeLike.remove(trafficLight) // Remove traffic light from DrawPanel
          anchor.removeTrafficLight() // Remove traffic light reference in the anchor
        }
        changeLike.remove(anchor.selectionRectangle) // Remove selection rectangle of the anchor

      }

      selectedAnchors = Nil

  def setSelectedShape(shape: ResizableShape): Unit =
    if addSelectedShapeProperty.get then addToSelectedShapes(shape)
    else replaceSelectedShape(shape)

    shape.getAnchors.foreach(_.setVisible(true))
    shape.rotationAnchors.map(_.getId).foreach(changeLike.removeById)

  private def addSelectionRectangle(shape: ResizableShape): Unit =
    if !changeLike.contains(shape.selectionRectangle) then
      changeLike.addNode(shape.selectionRectangle)

  private def replaceSelectedShape(shape: ResizableShape): Unit =
    unselectShapes()
    unselectAnchors()
    selectedShapes = List(shape)
    addSelectionRectangle(shape)

  private def addToSelectedShapes(shape: ResizableShape): Unit =
    // each item only 1 time in the selection
    if !selectedShapes.contains(shape) then
      selectedShapes = shape :: selectedShapes
      addSelectionRectangle(shape)

  def rotationMode(): Unit =
    selectedShapes.foreach { shape =>
      shape.getAnchors.foreach(_.setVisible(false))
      changeLike.addNode(shape.rotationAnchors)
    }

  def unselectShapes(): Unit =
    for shape <- selectedShapes do changeLike.remove(shape.selectionRectangle)
    selectedShapes = Nil

  def deleteSelectedShape(): Unit =
    if !selectedShapes.isEmpty then
      val shapeCopy = selectedShapes


      selectedShapes.foreach((shape: ResizableShape) =>
        if (shape.isInstanceOf[ResizableLine])
          then
            val roadId = shape.asInstanceOf[ResizableLine].getId
            val road = RoadManager.getRoad(roadId).get
            road.getLots.foreach((lot: ResizablePolygon) =>
              changeLike.remove(lot)
              ResizableLandLot.unregisterLandLot(lot)
              road.unregisterLandLot(lot)
            )

        else if(shape.isInstanceOf[ResizablePolygon])
          then
            ResizablePolygon.unregisterPolygon(shape.asInstanceOf[ResizablePolygon])
            ResizableLandLot.getLots.foreach((lot: ResizablePolygon) =>
              if(landLotInZone(lot, List(shape.asInstanceOf[ResizablePolygon])))
                then
                  ResizableLandLot.unregisterLandLot(lot)
                  lot.asInstanceOf[ResizableLandLot].getRoad.unregisterLandLot(lot)
                  changeLike.remove(lot)
            )
      )

      history.execute {
        shapeCopy foreach { shape =>
          changeLike.remove(shape.selectionRectangle)
          shape.rotationAnchors.map(_.getId).foreach(changeLike.removeById)
          changeLike.removeShape(shape)
        }
      } {
        shapeCopy foreach { shape =>
          changeLike.addNode(shape)
          changeLike.addNode(shape.getAnchors: _*)
        }
      }

      selectedShapes = List()

    if selectedAnchors.nonEmpty then
      deleteSelectedAnchor()

  def groupSelectedElements(): Unit =
    selectedShapes foreach { x =>
      changeLike.remove(x.selectionRectangle)
    }

    val group = new SelectionGroup(selectedShapes)
    changeLike.addShape(group)

    selectedShapes = List(group)

  def ungroupSelectedElements(): Unit =
    getSelectionGroups foreach { group =>
      changeLike.remove(group)
      changeLike.remove(group.selectionRectangle)
      group.childrens.foreach { shape =>
        changeLike.addNode(shape)
        changeLike.addNode(shape.getAnchors: _*)
      }
    }


  /**
   * Highlights and selects all resizable shapes within a specified bounding box.
   *
   * This method first determines which shapes fall within the specified start and end bounding points,
   * then deselects any currently selected shapes and anchors. It finally highlights the newly selected shapes
   * by adding their selection rectangles to the `DrawPanel`.
   *
   * ==Side Effect==
   * Clicking somewhere else in the `DrawPanel` where no shapes are present results in all previously selected shapes
   * and anchors being deselected, as this method clears the selection as a part of its functionality.
   *
   * @param startBounding The top-left corner of the bounding box.
   * @param endBounding   The bottom-right corner of the bounding box.
   */
  private def highlightGroupedElements(
      startBounding: Point,
      endBounding: Point
  ): Unit =
    val shapesInBox = changeLike.getElements filter {
      case shape: ResizableShape =>
        // only the elements thar are ResizableShapes and placed inside the bounding
        val shapeBounds = shape.getBoundsInParent
        shapeBounds.getMinX > startBounding.x &&
        shapeBounds.getMaxX < endBounding.x &&
        shapeBounds.getMinY > startBounding.y &&
        shapeBounds.getMaxY < endBounding.y
      case _ => false
    } map (_.asInstanceOf[ResizableShape])

    unselectShapes()
    unselectAnchors()
    for shape <- shapesInBox do changeLike.addNode(shape.selectionRectangle)

    selectedShapes = shapesInBox.toList

  def getGroupSelectionHandler: MouseEvent => Unit =
    var mouseP = (0.0, 0.0)
    // highlight the currently selection-space
    val groupRectangle =
      ShapeFactory.newRectangle((0, 0), 0.0, 0.0)(Color.BLACK, Color.BLACK, 1)
    groupRectangle.getStyleClass.addAll("selection-rectangle")
    groupRectangle.setId(DrawPanel.tmpShapeId)

    def groupHandler(mv: MouseEvent): Unit = mv.getEventType match
      case MouseEvent.MOUSE_PRESSED =>
        changeLike.addNode(groupRectangle)
        mouseP = (mv.getX, mv.getY)
        groupRectangle.setXY(mouseP)
        groupRectangle.setWidth(0)
        groupRectangle.setHeight(0)
      case MouseEvent.MOUSE_DRAGGED =>
        // adjust selection highlighting
        val w = mv.getX - mouseP.x
        val h = mv.getY - mouseP.y

        groupRectangle.setWidth(w)
        groupRectangle.setHeight(h)
      case MouseEvent.MOUSE_RELEASED =>
        val delta = (mv.getX, mv.getY) - mouseP
        val start = mouseP
        val end = start + delta
        changeLike.remove(groupRectangle)
        highlightGroupedElements(start, end)
      case _ => // ignore other events
    groupHandler
