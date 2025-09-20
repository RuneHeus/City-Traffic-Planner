/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.views.panes

import javafx.scene.Node
import javafx.scene.layout.Pane
import de.thm.move.types.*
import de.thm.move.views.SelectionGroup
import de.thm.move.views.anchors.{Anchor, SharedAnchor}
import de.thm.move.views.shapes.*

import scala.jdk.CollectionConverters.*

/** The main-panel which holds all drawn shapes */
class DrawPanel() extends Pane:
  private var shapes = List[Node]()

  getStyleClass.add("draw-pane")

  /** Draws/Adds the given Node n to this panel */
  def drawShape[T <: Node](n: T): Unit =
    super.getChildren.add(n)

    shapes = n :: shapes

  /** Removes the given shape. If the shape is a ResizableShape the
    * corresponding anchors get removed too.
    */
  def remove[A <: Node](shape: A): Unit =
    shapes = shapes.filterNot(_ == shape)
    shape match {
      case rs: ResizableShape =>
        rs.getAnchors.foreach {
          case shared: SharedAnchor =>
            shared.removeConnection(rs.asInstanceOf[ResizableLine]) // Remove the connection to the shape
            if (!shared.hasConnections) then{
              getChildren.remove(shared) // Remove anchor only if it has no more connections
            }
          case anchor =>
            getChildren.remove(anchor) // For regular anchors, remove directly
        }
        getChildren.remove(shape)
      case _ =>
        getChildren.remove(shape)
    }

  /** Removes all elements as long as the predicate returns true */
  def removeWhile(pred: Node => Boolean): Unit =
    val removingShapes = shapes.takeWhile(pred)
    shapes = shapes.dropWhile(pred)

    getChildren.removeAll(removingShapes: _*)

  /** Removes all elements as long as the predicate returns true */
  def removeWhileIdx(pred: (Node, Int) => Boolean): Unit =
    val shapeWithidx = shapes.zipWithIndex
    val removingShapes = shapeWithidx
      .takeWhile { case (n, idx) =>
        pred(n, idx)
      }
      .map(_._1)

    shapes = shapeWithidx
      .dropWhile { case (n, idx) =>
        pred(n, idx)
      }
      .map(_._1)

    getChildren.removeAll(removingShapes: _*)

  def setSize(p: Point): Unit =
    val (x, y) = p
    setPrefSize(x, y)
    setMinSize(x, y)
    setMaxSize(x, y)

  def setSize(w: Double, h: Double): Unit = setSize((w, h))

  /** Returns all shapes that should be '''converted into modelica'''-source
    * code.
    *
    * This method '''doesn't include all''' shapes, use getChildren for getting
    * all nodes.
    */
  def getShapes: List[Node] = getChildren.asScala.flatMap {
    case x: SelectionGroup                          => x.childrens
    case (_: Anchor | _: SelectionRectangle)        => Nil
    case x: Node if x.getId == DrawPanel.tmpShapeId => Nil
    case x: Node                                    => List(x)
  }.toList
object DrawPanel:

  /** Identifies temporary shapes. These shapes shouldn't get included into the
    * final Modelica-code.
    */
  val tmpShapeId = "temporary-shape"
