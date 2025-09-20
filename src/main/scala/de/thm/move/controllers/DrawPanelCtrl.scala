/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.controllers

import de.thm.move.views.anchors.SharedAnchor
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.input.InputEvent
import de.thm.move.views.panes.DrawPanel
import de.thm.move.views.shapes.ResizableLine.removeLine
import de.thm.move.views.shapes.{ResizableLine, ResizableShape}

import scala.jdk.CollectionConverters.*

class DrawPanelCtrl(
    val drawPanel: DrawPanel,
    shapeInputHandler: InputEvent => Unit
) extends ChangeDrawPanelLike:
  private val contextMenuCtrl = new ContextMenuCtrl(drawPanel, this)

  override def addShape(shape: ResizableShape*): Unit =
    shape foreach { x =>
      x.addEventHandler(
        InputEvent.ANY,
        new EventHandler[InputEvent]() {
          override def handle(event: InputEvent): Unit =
            shapeInputHandler(event)
        }
      )
      contextMenuCtrl.setupContextMenu(x)
      drawPanel.drawShape(x)
    }

  /**
   * Adds one or more nodes to the DrawPanel. If the node is a `SharedAnchor`,
   * an event handler is attached to allow it to respond to user interactions
   * (e.g., mouse clicks). Other types of nodes are added without attaching
   * event handlers.
   *
   * @param node one or more nodes to add to the DrawPanel.
   */
  override def addNode(node: Node*): Unit =
    node foreach { n =>
      n match
        case sharedAnchor: SharedAnchor =>
          sharedAnchor.addEventHandler(
            InputEvent.ANY,
            new EventHandler[InputEvent]() {
              override def handle(event: InputEvent): Unit =
                shapeInputHandler(event)
            }
          )
        case _ => // No handler for other nodes
      drawPanel.drawShape(n)
    }

  /**
   * Removes the given shape along with its associated anchors from the DrawPanel.
   * If the shape is a ResizableLine then we also remove it from its companion object.
   * Shared anchors are only removed if they no longer have any connections.
   *
   * @param shape the shape to be removed from the DrawPanel
   */
  override def removeShape(shape: ResizableShape): Unit =
    drawPanel.remove(shape)
    shape match // Only remove the line if the shape is a ResizableLine
      case line: ResizableLine =>
        removeLine(line) // Remove the line from the companion object
      case _ => // Do nothing if the shape is not a ResizableLine

    // Handle anchors associated with the shape
    shape.getAnchors.foreach {
      case shared: SharedAnchor => // Remove the connection to the shape from the shared anchor
        shared.removeConnection(shape.asInstanceOf[ResizableLine])
        if (!shared.hasConnections) then { // Remove the shared anchor only if it has no remaining connections

          shared.getTrafficLight.foreach{ trafficLight => // Remove the associated traffic light, if present
          drawPanel.remove(trafficLight) // Remove traffic light from the DrawPanel
            shared.removeTrafficLight() // Clear the traffic light reference from the anchor
          }

          drawPanel.remove(shared)
        }
      case anchor => // For normal anchors, simply remove them
        drawPanel.remove(anchor)
    }

  override def getElements: List[Node] = drawPanel.getChildren.asScala.toList
  override def remove(n: Node): Unit = drawPanel.remove(n)
