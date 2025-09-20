/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.views.shapes

import de.thm.move.types._

/** A shape that is movable.
  *
  * @note
  *   Implementations should move an element by moving shapes specific points
  *   directly. They shouldn't use transformations like setLayout, setTranslate!
  */
trait MovableShape:

  /** Moves this shape by the given delta
    * @note
    *   if delta.x < 0 => element moves left if delta.x > 0 => element moves
    *   right if delta.y < 0 => element moves up if delta.y > 0 => element moves
    *   down
    */
  def move(delta: Point): Unit
