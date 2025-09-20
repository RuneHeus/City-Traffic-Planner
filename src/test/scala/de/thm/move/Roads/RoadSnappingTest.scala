package de.thm.move.Roads

import de.thm.move.MoveSpec
import de.thm.move.views.shapes.ResizableLine
import de.thm.move.views.anchors.SharedAnchor

class RoadSnappingTest extends MoveSpec():
  val snapThreshold = 30 // snapThreshold hardcoded because it's a private val

  "A ResizableLine" should "snap to another ResizableLine" in {

    val line1 = new ResizableLine((10, 10), (100, 100), 2)
    line1.getAnchors
    ResizableLine.addLine(line1)

    // Create a second line close to the first line's start anchor
    val line2 = new ResizableLine((10 + snapThreshold - 1, 10), (200, 300), 2)
    line2.getAnchors
    ResizableLine.addLine(line2)

    // Verify snapping: Line2 should snap to Line1's start anchor
    line2.getStartX shouldEqual line1.getStartX
    line2.getStartY shouldEqual line1.getStartY
  }

  it should "not snap to a distant line that is further than the snapThreshold" in {

    val line3 = new ResizableLine((10, 10), (100, 100), 2)
    val line4 = new ResizableLine((10 + snapThreshold + 1, 10 + snapThreshold + 1), (100 + snapThreshold + 1, 100 + snapThreshold + 1), 2)

    val initialStartX4 = line4.getStartX
    val initialStartY4 = line4.getStartY

    line3.getAnchors
    ResizableLine.addLine(line3)
    line4.getAnchors
    ResizableLine.addLine(line4)

    // Verify no snapping occurred
    line4.getStartX shouldEqual initialStartX4
    line4.getStartY shouldEqual initialStartY4
  }

  it should "snap only the closest anchor when both anchors are within threshold of another line" in {
    val line1 = new ResizableLine((10, 10), (100, 100), 2)
    line1.getAnchors
    ResizableLine.addLine(line1)

    val line2 = new ResizableLine((10 + snapThreshold - 5, 10), (10 + snapThreshold - 1, 10), 2)
    line2.getAnchors
    ResizableLine.addLine(line2)

    // Verify snapping: Only the closest anchor of Line2 should snap to Line1
    line2.getStartX shouldEqual line1.getStartX
    line2.getStartY shouldEqual line1.getStartY
    line2.getEndX should not equal line1.getEndX
    line2.getEndY should not equal line1.getEndY
  }

  it should "not allow both anchors to snap to the same line" in {
    val line1 = new ResizableLine((10, 10), (100, 100), 2)
    line1.getAnchors
    ResizableLine.addLine(line1)

    val line2 = new ResizableLine((10, snapThreshold - 1), (100, 100 + snapThreshold - 1), 2)
    line2.getAnchors
    ResizableLine.addLine(line2)

    // Verify only one anchor snaps
    (line2.getStartX, line2.getStartY) shouldEqual (line1.getStartX, line1.getStartY)
    (line2.getEndX, line2.getEndY) should not equal (line1.getEndX, line1.getEndY)
  }


  it should "move a snapped line's anchors when the snapped anchor is moved" in {
    val line1 = new ResizableLine((10, 10), (100, 100), 2)
    line1.getAnchors
    ResizableLine.addLine(line1)

    val line2 = new ResizableLine((10 + snapThreshold - 1, 10), (200, 300), 2)
    line2.getAnchors
    ResizableLine.addLine(line2)

    // Move line1's start anchor which should also move both lines
    val moveDelta = (20.0, 20.0)
    line1.resizeWithAnchor(0, moveDelta)

    // Verify that line2's start moved with line1's start
    line1.getStartX shouldEqual line2.getStartX
    line1.getStartY shouldEqual line2.getStartY

    // Verify that end points remain unaffected by the movement
    line1.getEndX shouldNot equal(line2.getEndX)
    line1.getEndY shouldNot equal(line2.getEndY)
  }