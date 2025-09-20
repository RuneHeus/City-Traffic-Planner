/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.views.anchors

import javafx.scene.input.MouseEvent
import javafx.scene.shape.Ellipse

import de.thm.move.types._
import de.thm.move.Global._
import de.thm.move.history.History
import de.thm.move.history.History.Command

import de.thm.move.util.JFxUtils._

/** Makes an anchor/ellipse movable when dragging the anchor
  * @note
  *   This trait adjusts the Ellipse's centerX/centerY coordinates
  */
trait MovableAnchor:
  self: Ellipse =>

  private var deltaX = -1.0
  private var deltaY = -1.0

  // undo-/redo command
  private var command: (=> Unit) => Command = x => { History.emptyAction }

  self.setOnMousePressed(withConsumedEvent { (me: MouseEvent) =>
    val oldX = self.getCenterX
    val oldY = self.getCenterY

    deltaX = oldX - me.getX
    deltaY = oldY - me.getY

    command = History.partialAction {
      self.setCenterX(oldX)
      self.setCenterY(oldY)
    }
  })

  self.setOnMouseReleased(withConsumedEvent { (_: MouseEvent) =>
    val x = self.getCenterX
    val y = self.getCenterY
    history.save(command {
      self.setCenterX(x)
      self.setCenterY(y)
    })
  })

  self.setOnMouseDragged(withConsumedEvent { (me: MouseEvent) =>
    self.setCenterX(deltaX + me.getX)
    self.setCenterY(deltaY + me.getY)
  })

  def move(delta: Point): Unit =
    val (x, y) = delta
    self.setCenterX(self.getCenterX + x)
    self.setCenterY(self.getCenterY + y)
