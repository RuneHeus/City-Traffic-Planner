/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.thm.move.controllers

import javafx.scene.Node
import javafx.scene.input.MouseEvent
import de.thm.move.Global
import de.thm.move.history.History
import de.thm.move.util.JFxUtils.*
import de.thm.move.types.*
import de.thm.move.views.panes.SnapLike
import de.thm.move.views.shapes.{MovableShape, ResizableLandLot, ResizableLine, ResizablePolygon}
import de.thm.move.Roads.Road
import de.thm.move.Roads.RoadManager

import de.thm.move.controllers.drawing.snapToRoad
import de.thm.move.views.anchors.SharedAnchor
import de.thm.move.views.anchors.Anchor
import de.thm.move.lotOperations.checkLandLotsOutsideZone
import de.thm.move.zoneOperations.overlapAfterMove

/** Behaviour for moving selected ResizableShapes. */
trait SelectedMoveCtrl extends snapToRoad with checkLandLotsOutsideZone with overlapAfterMove:
  this: SelectionCtrlLike =>

  def getSnapLike: SnapLike

  def getMoveHandler: (MouseEvent => Unit) =
    var mouseP = (0.0, 0.0)
    var startP = mouseP

    def moveElement(mv: MouseEvent): Unit =
      (mv.getEventType, mv.getSource) match
        case (MouseEvent.MOUSE_PRESSED, shape: MovableShape) =>
          mouseP = (mv.getSceneX, mv.getSceneY)
          startP = mouseP // save start-point for undo
        case (MouseEvent.MOUSE_DRAGGED, node: Node with MovableShape) =>
          // translate from original to new position
          val delta = (mv.getSceneX - mouseP.x, mv.getSceneY - mouseP.y)
          // if clicked shape is in selection:
          // move all selected
          // else: move only clicked shape
          withParentMovableElement(node) { shape =>
            val allShapes =
              if(shape.isInstanceOf[ResizableLine])
              then
                getSelectedShapes.appendedAll(RoadManager.getRoad(shape.asInstanceOf[ResizableLine].getId).get.getLots).appendedAll(List(shape))
              else if getSelectedShapes.contains(shape) then getSelectedShapes
              else List(shape)
            if(allShapes.forall((shape: MovableShape) =>
              shape.isInstanceOf[ResizableLandLot]
            )) then null
            else
              allShapes.foreach(_.move(delta))
            // don't forget to use the new mouse-point as starting-point
            mouseP = (mv.getSceneX, mv.getSceneY)
          }
        case (MouseEvent.MOUSE_RELEASED, node: Node with MovableShape) =>
          withParentMovableElement(node) { shape =>
            val movedShapes =
              //of a line is moved, the land lots have to be snapped to the road after the road is snapped.
              if(shape.isInstanceOf[ResizableLine])
              then
                val road = RoadManager.getRoad(shape.asInstanceOf[ResizableLine].getId).get
                val lots = road.getLots

                SnapLike.applySnapToGrid(getSnapLike, node)

                lots.foreach((landLot: ResizablePolygon) =>
                  landLot.getAnchors.foreach((anchor: Anchor) =>
                    closeEnough(landLot, shape.asInstanceOf[ResizableLine])
                  )
                )
                getSelectedShapes.appendedAll(List(shape))

              if(shape.isInstanceOf[ResizablePolygon] && !shape.isInstanceOf[ResizableLandLot])
              then
                SnapLike.applySnapToGrid(getSnapLike, node)
                checkEditedZones(shape.asInstanceOf[ResizablePolygon].getAnchors)
                List(shape)

              else if getSelectedShapes.contains(shape) then getSelectedShapes
              else List(shape)

            //check that after moving a shape all land lots are still inside a zone.
            checkLandLotsOutside()

            if (movedShapes.forall((shape: MovableShape) =>
              !shape.isInstanceOf[ResizableLandLot]
            )) then SnapLike.applySnapToGrid(getSnapLike, node)



            // calculate delta (offset from original position) for un-/redo
            val deltaRedo = (mv.getSceneX - startP.x, mv.getSceneY - startP.y)
            val deltaUndo = deltaRedo.map(_ * (-1))
            val cmd = History.newCommand(
              movedShapes.foreach(_.move(deltaRedo)),
              movedShapes.foreach(_.move(deltaUndo))
            )
            Global.history.save(cmd)
          }
        case _ => // unknown event
    moveElement

  def move(p: Point): Unit = getSelectedShapes.foreach(_.move(p))