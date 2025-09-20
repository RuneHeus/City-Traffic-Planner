/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/.
 */

package de.thm.move.views.shapes

import javafx.geometry.Bounds
import javafx.scene.input.MouseEvent
import de.thm.move.Global
import de.thm.move.implicits.FxHandlerImplicits.*
import de.thm.move.history.History
import de.thm.move.util.JFxUtils.*
import de.thm.move.types.*
import de.thm.move.views.anchors.Anchor
import de.thm.move.controllers.drawing.LineStrategy
import de.thm.move.controllers.drawing.ShrinkStrategy
import de.thm.move.controllers.drawing.lotContained
import scala.jdk.CollectionConverters._
import de.thm.move.lotOperations.checkLandLotsOutsideZone
import de.thm.move.zoneOperations.overlapAfterMove
/** An element that is represented by a path.
 *
 * This trait adds moving/resizing the shape for free.
 *
 * @note
 *   Due to initialization problems overwrite edgeCount as a lazy val!
 */
trait PathLike extends lotContained with checkLandLotsOutsideZone with overlapAfterMove:
  self: ResizableShape =>
  /** Count of the edges of this shape
   *
   * @note
   *   Overwrite this field as a lazy val to avoid initialization problems! If
   *   you don't use a lazy val this field is 0 and getAnchors will return an
   *   empty list!
   *
   * @see
   *   [[https://github.com/scala/scala.github.com/blob/master/tutorials/FAQ/initialization-order.md Scala - Init order]]
   */
  def edgeCount: Int

  /* Overwrite as lazy val in order to initialize this field AFTER edgeCount is initialized!
   *
   * If this is a strict val it will be initialized before edgeCount and result in an empty list
   * */
  override lazy val getAnchors: List[Anchor] = genAnchors

  protected def makeAnchor(x: Double, y: Double): Anchor =
    new Anchor(x,y)







  protected def genAnchors = List.tabulate(edgeCount) { idx =>
    val (x, y) = getEdgePoint(idx)
    val anchor = makeAnchor(x, y)
    var startP = (0.0, 0.0)
    var mouseP = startP

    anchor.setOnMousePressed(withConsumedEvent { (me: MouseEvent) =>
      startP = (me.getSceneX, me.getSceneY)
      mouseP = startP
    })
    anchor.setOnMouseDragged(withConsumedEvent { (mv: MouseEvent) =>
      val delta = (mv.getSceneX - mouseP.x, mv.getSceneY - mouseP.y)
      resizeWithAnchor(idx, delta)
      mouseP = (mv.getSceneX, mv.getSceneY)
    })
    anchor.setOnMouseReleased(withConsumedEvent { (mv: MouseEvent) =>
      // calculate delta (offset from original position) for un-/redo
      val deltaRedo = (mv.getSceneX - startP.x, mv.getSceneY - startP.y)
      val deltaUndo = deltaRedo.map(_ * (-1))
      checkEditedZones(this.getAnchors)
      checkLandLotsOutside()



      val cmd = History.newCommand(
        resizeWithAnchor(idx, deltaRedo),
        resizeWithAnchor(idx, deltaUndo)
      )
      Global.history.save(cmd)
    })
    anchor
  }

  boundsInLocalProperty().addListener { (_: Bounds, _: Bounds) => // TODO: this causes the inf loop with the snapping, need to fix this
    boundsChanged()
  }

  rotateProperty().addListener { (_: Number, newV: Number) =>
    boundsChanged()
  }

  protected def boundsChanged(): Unit = //changed to protected to be able to override it
    indexWithAnchors.foreach { case (idx, anchor) =>
      val (x, y) = localToParentPoint(getEdgePoint(idx))
      anchor.setCenterX(x)
      anchor.setCenterY(y)
    }

  /** The indexes of the anchors */
  private lazy val indexes: List[Int] = (0 until edgeCount).toList

  /** Indexes together with it's anchors */
  private lazy val indexWithAnchors = indexes.zip(getAnchors)

  /** Returns the point of this shape at the edge identified by idx. */
  def getEdgePoint(idx: Int): Point

  /** Resizes the edge identified by idx with the given delta and adjusts the
   * corresponding anchor.
   */
  def resizeWithAnchor(idx: Int, delta: Point): Unit =
    val anchor = getAnchors(idx)
    anchor.setCenterX(anchor.getCenterX + delta.x)
    anchor.setCenterY(anchor.getCenterY + delta.y)
    resize(idx, delta)

  /** Resizes the edge identified by idx with the given delta. */
  def resize(idx: Int, delta: Point): Unit
  override def move(delta: Point): Unit = indexWithAnchors.foreach {
    case (idx, anchor) =>
      resizeWithAnchor(idx, delta)
  }