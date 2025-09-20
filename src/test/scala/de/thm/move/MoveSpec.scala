/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move

import org.scalatest.*
import flatspec.*
import matchers.*
import de.thm.move.Roads.*
import de.thm.move.views.shapes.ResizableLine

abstract class MoveSpec extends AnyFlatSpec with should.Matchers with BeforeAndAfterEach:
  override def beforeEach(): Unit =
    super.beforeEach()
    RoadManager.roads.clear()
    RoadManager.nodes.clear()
    ResizableLine.allLines.clear()

  override def afterEach(): Unit =
    RoadManager.roads.clear()
    RoadManager.nodes.clear()
    ResizableLine.allLines.clear()
    super.afterEach()
