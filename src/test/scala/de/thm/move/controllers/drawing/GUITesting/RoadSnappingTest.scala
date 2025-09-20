package de.thm.move.controllers.drawing.GUITesting

import de.thm.move.views.shapes.ResizableLine
import javafx.geometry.Point2D
import javafx.scene.layout.StackPane
import org.junit.Assert.*
import org.junit.Test
import scala.math._
import javafx.geometry.Bounds

import scala.jdk.CollectionConverters._

class RoadSnappingTest extends GuiTest:
  private val TOLERANCE = 3.0
  private val SnappingThreshold = 30

  // Helper: Calculate Euclidean distance
  private def dist(p1: (Double, Double), p2: (Double, Double)): Double =
    sqrt(pow(p1._1 - p2._1, 2) + pow(p1._2 - p2._2, 2))


  // Test: Validate snapping behavior between two connected lines
  @Test def testLineSnappingSimple(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(2, 2), new Point2D(200, 200))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(227, 200), new Point2D(267, 240))

    val distEndToStart = dist((line1.getEndX, line1.getEndY), (line2.getStartX, line2.getStartY))
    println(f"Distance between Line1 End and Line2 Start: $distEndToStart%.2f")
    assertEquals("Line2 start should snap to Line1 end",
      (line1.getEndX, line1.getEndY),
      (line2.getStartX, line2.getStartY))

  // Test: Validate snapping with multiple lines forming a grid-like structure
  @Test def testGridLineSnapping(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(205, 100), new Point2D(300, 100)) // Connected to line1
    val line3 = drawLine(grid, gridBoundsInLocal, new Point2D(205, 105), new Point2D(200, 200)) // Perpendicular to line1

    assertEquals("Line2 start should snap to Line1 end",
      (line1.getEndX, line1.getEndY),
      (line2.getStartX, line2.getStartY))

    assertEquals("Line3 start should snap to Line1 end",
      (line1.getEndX, line1.getEndY),
      (line3.getStartX, line3.getStartY))

  // Test: Validate snapping behavior for parallel lines with overlapping anchors
  @Test def testLineConnection(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    val line3 = drawLine(grid, gridBoundsInLocal, new Point2D(300, 300), new Point2D(400, 300))
    val line4 = drawLine(grid, gridBoundsInLocal, new Point2D(300, 327), new Point2D(400, 327))

    val distStartToStart = dist((line3.getStartX, line3.getStartY), (line4.getStartX, line4.getStartY))
    println(f"Distance between Line3 Start and Line4 Start: $distStartToStart%.2f")
    assertEquals("Line3 start should snap to Line4 start",
      (line3.getStartX, line3.getStartY),
      (line4.getStartX, line4.getStartY))

  // Test: Validate anchor movement with connected lines
  @Test def testAnchorMovement(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw a baseline and a connected line
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(300, 300), new Point2D(400, 300))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(405, 300), new Point2D(500, 300))

    // Validate snapping
    val distEndToStart = dist((line1.getEndX, line1.getEndY), (line2.getStartX, line2.getStartY))
    assertEquals("Line2 start should snap to Line1 end",
      (line1.getEndX, line1.getEndY),
      (line2.getStartX, line2.getStartY))
    // Move the anchor of the first line
    val anchor = line1.getAnchors(1) // Anchor at the end of Line1
    val moveToPoint = grid.localToScreen(gridToLocal(gridBoundsInLocal, new Point2D(350, 300)))

    moveTo(anchor)
    drag()
    moveTo(moveToPoint)
    drop()

    // Validate that both lines moved correctly
    assertTrue("All lines connected to the anchor should move correctly",
      anchor.connectedLines.forall { (line, idx) =>
        val (expectedX, expectedY) = (anchor.getCenterX, anchor.getCenterY)
        if idx == 0 then (line.getStartX, line.getStartY) == (expectedX, expectedY)
        else (line.getEndX, line.getEndY) == (expectedX, expectedY)
      }
    )

  @Test def testAnchorPositionAfterLineMove(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw a line
    val line = drawLine(grid, gridBoundsInLocal, new Point2D(300, 300), new Point2D(400, 300))

    // Collect the anchors of the line
    val startAnchor = line.getAnchors.head
    val endAnchor = line.getAnchors.last

    // Save initial positions of the anchors
    val initialStartPosition = (startAnchor.getCenterX, startAnchor.getCenterY)
    val initialEndPosition = (endAnchor.getCenterX, endAnchor.getCenterY)

    // Move the line
    clickOn("#line_pointer")
    moveTo(line)
    drag()
    moveBy(20, 20) // Move the line by (20, 20)
    drop()

    // Validate that the anchors moved with the line
    assertEquals("Start anchor X position should match line start X position",
      line.getStartX, startAnchor.getCenterX, 0.1)
    assertEquals("Start anchor Y position should match line start Y position",
      line.getStartY, startAnchor.getCenterY, 0.1)

    assertEquals("End anchor X position should match line end X position",
      line.getEndX, endAnchor.getCenterX, 0.1)
    assertEquals("End anchor Y position should match line end Y position",
      line.getEndY, endAnchor.getCenterY, 0.1)


  @Test def testAnchorPositionAfterSnappedLineMove(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw two lines to create a snapping scenario
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(300, 300), new Point2D(400, 300))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(405, 300), new Point2D(500, 300))

    // Validate snapping
    assertEquals("Line2 start should snap to Line1 end",
      (line1.getEndX, line1.getEndY),
      (line2.getStartX, line2.getStartY))

    // Collect the anchors of the snapped line
    val startAnchor = line2.getAnchors.head
    val endAnchor = line2.getAnchors.last

    // Save initial positions of the anchors
    val initialStartPosition = (startAnchor.getCenterX, startAnchor.getCenterY)
    val initialEndPosition = (endAnchor.getCenterX, endAnchor.getCenterY)

    // Move the snapped line
    clickOn("#line_pointer")
    moveTo(line2)
    drag()
    moveBy(20, 20) // Move the snapped line by (20, 20)
    drop()

    // Validate that the anchors moved with the snapped line
    assertEquals("Start anchor X position should match snapped line start X position",
      line2.getStartX, startAnchor.getCenterX, 0.1)
    assertEquals("Start anchor Y position should match snapped line start Y position",
      line2.getStartY, startAnchor.getCenterY, 0.1)

    assertEquals("End anchor X position should match snapped line end X position",
      line2.getEndX, endAnchor.getCenterX, 0.1)
    assertEquals("End anchor Y position should match snapped line end Y position",
      line2.getEndY, endAnchor.getCenterY, 0.1)


  @Test def testAnchorPositionAfterMiddleSnappedLineMove(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw three lines to create a snapping scenario
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(300, 300), new Point2D(400, 300))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(405, 300), new Point2D(500, 300)) // Middle line
    val line3 = drawLine(grid, gridBoundsInLocal, new Point2D(505, 300), new Point2D(600, 300))

    // Validate snapping on both sides
    assertEquals("Line2 start should snap to Line1 end",
      (line1.getEndX, line1.getEndY),
      (line2.getStartX, line2.getStartY))

    assertEquals("Line2 end should snap to Line3 start",
      (line3.getStartX, line3.getStartY),
      (line2.getEndX, line2.getEndY))

    // Collect the anchors of the middle line
    val startAnchor = line2.getAnchors.head
    val endAnchor = line2.getAnchors.last

    // Save initial positions of the anchors
    val initialStartPosition = (startAnchor.getCenterX, startAnchor.getCenterY)
    val initialEndPosition = (endAnchor.getCenterX, endAnchor.getCenterY)

    // Move the middle line
    clickOn("#line_pointer")
    moveTo(line2)
    drag()
    moveBy(20, 20) // Move the middle line by (20, 20)
    drop()

    // Validate that the anchors moved with the middle line
    assertEquals("Start anchor X position should match middle line start X position",
      line2.getStartX, startAnchor.getCenterX, 0.1)
    assertEquals("Start anchor Y position should match middle line start Y position",
      line2.getStartY, startAnchor.getCenterY, 0.1)

    assertEquals("End anchor X position should match middle line end X position",
      line2.getEndX, endAnchor.getCenterX, 0.1)
    assertEquals("End anchor Y position should match middle line end Y position",
      line2.getEndY, endAnchor.getCenterY, 0.1)

    // Validate that the connected lines also updated correctly
    assertEquals("Line1 end should match new position of Line2 start",
      (line2.getStartX, line2.getStartY),
      (line1.getEndX, line1.getEndY))

    assertEquals("Line3 start should match new position of Line2 end",
      (line2.getEndX, line2.getEndY),
      (line3.getStartX, line3.getStartY))

  // Test: Validate no snapping when lines are out of tolerance range
  @Test def testNoSnapping(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(250, 150), new Point2D(300, 200))

    // Calculate distance and validate
    val distEndToStart = dist((line1.getEndX, line1.getEndY), (line2.getStartX, line2.getStartY))
    println(f"Distance between Line1 End and Line2 Start (No Snap Expected): $distEndToStart%.2f")
    assertTrue("Line2 should not snap to Line1 end", distEndToStart > SnappingThreshold)


    def withinTolerance(expected: Double, actual: Double): Boolean =
      Math.abs(expected - actual) <= TOLERANCE

    // Validate line2's positions with tolerance
    assertTrue("Line2 start X should remain within tolerance",
      withinTolerance(250.0, line2.getStartX))
    assertTrue("Line2 start Y should remain within tolerance",
      withinTolerance(150.0, line2.getStartY))
    assertTrue("Line2 end X should remain within tolerance",
      withinTolerance(300.0, line2.getEndX))
    assertTrue("Line2 end Y should remain within tolerance",
      withinTolerance(200.0, line2.getEndY))

  @Test def testDiagonalLineSnapping(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 200)) // Diagonal line
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(210, 210), new Point2D(300, 300)) // Slightly offset diagonal

    val distEndToStart = dist((line1.getEndX, line1.getEndY), (line2.getStartX, line2.getStartY))
    println(f"Distance between Line1 End and Line2 Start: $distEndToStart%.2f")
    assertEquals("Line2 start should snap diagonally to Line1 end",
      (line1.getEndX, line1.getEndY),
      (line2.getStartX, line2.getStartY))

  @Test def testMultipleLinesSnappingAndDraggingExtended(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw a central baseline
    val baseLine = drawLine(grid, gridBoundsInLocal, new Point2D(300, 300), new Point2D(450, 300))
    val baseAnchor = baseLine.getAnchors(1) // Anchor at the end of the baseline

    // Draw 5 lines snapped to the base anchor
    val lines = (1 to 5).map { i =>
      val offset = i * 10
      drawLine(grid, gridBoundsInLocal, new Point2D(400, 300 + offset), new Point2D(450, 300))
    }

    // Validate all lines snapped to the base anchor
    lines.foreach { line =>
      val distToBaseAnchor = dist((line.getEndX, line.getEndY), (baseAnchor.getCenterX, baseAnchor.getCenterY))
      println(f"Distance from Line Start to Base Anchor: $distToBaseAnchor%.2f")
      assertEquals("Line end should snap to the base anchor",
        (baseAnchor.getCenterX, baseAnchor.getCenterY),
        (line.getEndX, line.getEndY))
    }

    // Define positions to move the anchor
    val positions = Map(
      "down" -> new Point2D(450, 450),
      "up" -> new Point2D(250, 250),
      "left" -> new Point2D(200, 300),
      "right" -> new Point2D(500, 300)
    )

    // Move the base anchor to each position and validate
    positions.foreach { case (direction, position) =>
      println(s"Moving base anchor $direction to position $position")

      val moveToPoint = grid.localToScreen(gridToLocal(gridBoundsInLocal, position))
      moveTo(baseAnchor)
      drag()
      moveTo(moveToPoint)
      drop()

      // Validate all lines moved correctly with the base anchor
      assertTrue(s"All lines connected to the base anchor should move correctly when dragged $direction",
        baseAnchor.connectedLines.forall { (line, idx) =>
          val (expectedX, expectedY) = (baseAnchor.getCenterX, baseAnchor.getCenterY)
          if idx == 0 then (line.getStartX, line.getStartY) == (expectedX, expectedY)
          else (line.getEndX, line.getEndY) == (expectedX, expectedY)
        }
      )
    }


  @Test def testMiddleLineSnappingToTwoLines(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw the first line
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))

    // Draw the second line parallel to the first one
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(300, 100), new Point2D(400, 100))

    // Draw the third line starting in between line1 and line2
    val line3 = drawLine(grid, gridBoundsInLocal, new Point2D(205, 100), new Point2D(300, 100))

    // Validate that line3 snapped to both line1 and line2
    val distLine3StartToLine1End = dist((line1.getEndX, line1.getEndY), (line3.getStartX, line3.getStartY))
    val distLine3EndToLine2Start = dist((line2.getStartX, line2.getStartY), (line3.getEndX, line3.getEndY))

    println(f"Distance from Line3 Start to Line1 End: $distLine3StartToLine1End%.2f")
    println(f"Distance from Line3 End to Line2 Start: $distLine3EndToLine2Start%.2f")

    assertEquals("Line3 start should snap to Line1 end",
      (line1.getEndX, line1.getEndY),
      (line3.getStartX, line3.getStartY))

    assertEquals("Line3 end should snap to Line2 start",
      (line2.getStartX, line2.getStartY),
      (line3.getEndX, line3.getEndY))

    // Validate that moving Line1's end also moves Line3's start
    val anchorLine1 = line1.getAnchors(1) // Anchor at the end of Line1
    val moveToPoint = grid.localToScreen(gridToLocal(gridBoundsInLocal, new Point2D(150, 110)))

    moveTo(anchorLine1)
    drag()
    moveTo(moveToPoint)
    drop()

    assertTrue("Line3 should move correctly with Line1's anchor",
      anchorLine1.connectedLines.forall { (line, idx) =>
        val (expectedX, expectedY) = (anchorLine1.getCenterX, anchorLine1.getCenterY)
        if idx == 0 then (line.getStartX, line.getStartY) == (expectedX, expectedY)
        else true // Only validating snapping for Line3
      }
    )

    // Validate that moving Line2's start also moves Line3's end
    val anchorLine2 = line2.getAnchors(0) // Anchor at the start of Line2
    val moveToPointLine2 = grid.localToScreen(gridToLocal(gridBoundsInLocal, new Point2D(350, 110)))

    moveTo(anchorLine2)
    drag()
    moveTo(moveToPointLine2)
    drop()

    assertTrue("Line3 should move correctly with Line2's anchor",
      anchorLine2.connectedLines.forall { (line, idx) =>
        val (expectedX, expectedY) = (anchorLine2.getCenterX, anchorLine2.getCenterY)
        if idx == 1 then (line.getEndX, line.getEndY) == (expectedX, expectedY)
        else true // Only validating snapping for Line3
      }
    )


  @Test def testLineSnappingToClosestAnchor(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw the first line
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))

    // Draw the second line near the first one but slightly further from the potential snapping point
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(240, 100), new Point2D(400, 110))

    // Calculate the pre-snapping distances of the third line's start to the anchors of line1 and line2
    val thirdLineStart = new Point2D(218, 105)
    val distToLine1End = dist((line1.getEndX, line1.getEndY), (thirdLineStart.getX, thirdLineStart.getY))
    val distToLine2Start = dist((line2.getStartX, line2.getStartY), (thirdLineStart.getX, thirdLineStart.getY))

    // Determine the expected snapping target
    val expectedSnapTarget =
      if distToLine1End <= distToLine2Start then (line1.getEndX, line1.getEndY)
      else (line2.getStartX, line2.getStartY)

    println(f"Expected Snap Target: $expectedSnapTarget")

    // Draw the third line and let it snap
    val line3 = drawLine(grid, gridBoundsInLocal, thirdLineStart, new Point2D(250, 150))

    // Validate that line3 snapped to the correct anchor
    val snappedStart = (line3.getStartX, line3.getStartY)
    println(f"Snapped Start: $snappedStart")

    assertEquals("Line3 start should snap to the closest anchor",
      expectedSnapTarget,
      (line3.getStartX, line3.getStartY))

  @Test def testDeletingARoad(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw a line on the grid
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))

    // Validate the line exists
    assertTrue("The line should exist before deletion", ResizableLine.allLines.contains(line1))

    // Select the line using the pointer tool
    clickOn("#line_pointer")
    moveTo(line1)
    press(javafx.scene.input.MouseButton.PRIMARY)
    release(javafx.scene.input.MouseButton.PRIMARY)

    // Open the "Edit" menu and delete the line
    clickOn("Edit")
    clickOn("#deleteMenuItem")

    // Validate the line has been deleted
    assertFalse("The line should have been removed from ResizableLine.allLines after deletion", ResizableLine.allLines.contains(line1))


  @Test def testDeletingARoadWithBackspace(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw a line on the grid
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(300, 300), new Point2D(400, 300))

    // Validate the line exists
    assertTrue("The line should exist before deletion", ResizableLine.allLines.contains(line1))

    // Select the line using the pointer tool
    clickOn("#line_pointer")
    moveTo(line1)
    press(javafx.scene.input.MouseButton.PRIMARY)
    release(javafx.scene.input.MouseButton.PRIMARY)

    // Delete the line using the Backspace key
    press(javafx.scene.input.KeyCode.BACK_SPACE)

    // Validate the line has been deleted
    assertFalse("The line should have been removed from ResizableLine.allLines after pressing Backspace key", ResizableLine.allLines.contains(line1))


  @Test def testSharedAnchorsPersistAfterMiddleRoadDeletion(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw three connected roads (road1 -> road2 -> road3)
    val road1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))
    val road2 = drawLine(grid, gridBoundsInLocal, new Point2D(205, 100), new Point2D(300, 100)) // Shared anchor
    val road3 = drawLine(grid, gridBoundsInLocal, new Point2D(305, 100), new Point2D(400, 100))

    // Validate initial shared anchors
    val sharedAnchorBetweenRoad1AndRoad2 = road1.getAnchors.last
    val sharedAnchorBetweenRoad2AndRoad3 = road2.getAnchors.last

    assertTrue("Shared anchor between road1 and road2 should exist",
      road2.getAnchors.contains(sharedAnchorBetweenRoad1AndRoad2))
    assertTrue("Shared anchor between road2 and road3 should exist",
      road3.getAnchors.contains(sharedAnchorBetweenRoad2AndRoad3))

    // Select and delete the middle road (road2)
    clickOn("#line_pointer")
    moveTo(road2)
    press(javafx.scene.input.MouseButton.PRIMARY)
    release(javafx.scene.input.MouseButton.PRIMARY)
    press(javafx.scene.input.KeyCode.BACK_SPACE)

    // Validate the middle road has been deleted
    assertFalse("Road2 should be removed after deletion", ResizableLine.allLines.contains(road2))

    // Validate shared anchors persist logically
    assertTrue("Shared anchor between road1 and road2 should remain the same",
      road1.getAnchors.contains(sharedAnchorBetweenRoad1AndRoad2))
    assertTrue("Shared anchor between road2 and road3 should remain the same",
      road3.getAnchors.contains(sharedAnchorBetweenRoad2AndRoad3))

    // Validate anchors are still drawn
    val sceneNodes = grid.getChildrenUnmodifiable.asScala
    assertTrue("Shared anchor between road1 and road2 should still be drawn",
      sceneNodes.exists(node => node.getBoundsInParent.contains(sharedAnchorBetweenRoad1AndRoad2.getCenterX, sharedAnchorBetweenRoad1AndRoad2.getCenterY)))
    assertTrue("Shared anchor between road2 and road3 should still be drawn",
      sceneNodes.exists(node => node.getBoundsInParent.contains(sharedAnchorBetweenRoad2AndRoad3.getCenterX, sharedAnchorBetweenRoad2AndRoad3.getCenterY)))

    // Log debugging information
    println(s"Shared anchor for road1-road2: (${sharedAnchorBetweenRoad1AndRoad2.getCenterX}, ${sharedAnchorBetweenRoad1AndRoad2.getCenterY})")
    println(s"Shared anchor for road2-road3: (${sharedAnchorBetweenRoad2AndRoad3.getCenterX}, ${sharedAnchorBetweenRoad2AndRoad3.getCenterY})")

    // Ensure remaining roads still behave correctly
    assertEquals("Road1 endpoint should match shared anchor position",
      (sharedAnchorBetweenRoad1AndRoad2.getCenterX, sharedAnchorBetweenRoad1AndRoad2.getCenterY),
      (road1.getEndX, road1.getEndY))

    assertEquals("Road3 start point should match shared anchor position",
      (sharedAnchorBetweenRoad2AndRoad3.getCenterX, sharedAnchorBetweenRoad2AndRoad3.getCenterY),
      (road3.getStartX, road3.getStartY))


  @Test def testStarTopologyAfterRoadDeletion(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    val centralPoint = new Point2D(300, 300)

    // Create four roads connecting to the central point
    val road1 = drawLine(grid, gridBoundsInLocal, new Point2D(300, 200), centralPoint) // Top
    val road2 = drawLine(grid, gridBoundsInLocal, new Point2D(300, 400), centralPoint) // Bottom
    val road3 = drawLine(grid, gridBoundsInLocal, new Point2D(200, 300), centralPoint) // Left
    val road4 = drawLine(grid, gridBoundsInLocal, new Point2D(400, 300), centralPoint) // Right

    val centralAnchor = road1.getAnchors.last

    // Delete one of the roads
    clickOn("#line_pointer")
    moveTo(road2)
    press(javafx.scene.input.MouseButton.PRIMARY)
    release(javafx.scene.input.MouseButton.PRIMARY)
    press(javafx.scene.input.KeyCode.BACK_SPACE)

    // Validate deleted road is removed
    assertFalse("Road2 should be removed after deletion", ResizableLine.allLines.contains(road2))

    // Validate remaining roads still connect to the central anchor
    assertTrue("Road1 should still connect to the central anchor", road1.getAnchors.contains(centralAnchor))
    assertTrue("Road3 should still connect to the central anchor", road3.getAnchors.contains(centralAnchor))
    assertTrue("Road4 should still connect to the central anchor", road4.getAnchors.contains(centralAnchor))

    // Move the central anchor to test interactivity
    moveTo(centralAnchor)
    press(javafx.scene.input.MouseButton.PRIMARY)
    drag()
    moveBy(100, 100)
    drop()

    // Validate anchor position logically aligns with remaining roads
    assertEquals("Central anchor X position should align with Road1 endpoint",
      road1.getEndX, centralAnchor.getCenterX, 0.0)
    assertEquals("Central anchor Y position should align with Road1 endpoint",
      road1.getEndY, centralAnchor.getCenterY, 0.0)

    assertEquals("Central anchor X position should align with Road3 endpoint",
      road3.getEndX, centralAnchor.getCenterX, 0.0)
    assertEquals("Central anchor Y position should align with Road3 endpoint",
      road3.getEndY, centralAnchor.getCenterY, 0.0)

    assertEquals("Central anchor X position should align with Road4 endpoint",
      road4.getEndX, centralAnchor.getCenterX, 0.0)
    assertEquals("Central anchor Y position should align with Road4 endpoint",
      road4.getEndY, centralAnchor.getCenterY, 0.0)

    println(s"Final central anchor position: (${centralAnchor.getCenterX}, ${centralAnchor.getCenterY})")


  @Test def deletingOriginalRoadOfASharedAnchor(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    val line3 = drawLine(grid, gridBoundsInLocal, new Point2D(250, 105), new Point2D(200, 100))
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(150, 110), new Point2D(200, 100))
    assertEquals("Initial number of lines", 3, ResizableLine.allLines.size)

    val anchorsOriginal = line3.getAnchors //The Snapped Anchor

    clickOn("#line_pointer")
    clickOn(anchorsOriginal.last)
    moveBy(-10, -10)
    drag()
    moveTo(anchorsOriginal.head)
    moveBy(10,10)
    release(javafx.scene.input.MouseButton.PRIMARY)
    press(javafx.scene.input.KeyCode.BACK_SPACE)

    assertEquals("Number of lines after deleting one", 2, ResizableLine.allLines.size)
    assertFalse("Original line should be deleted", ResizableLine.allLines.contains(line3))

    // Ensure the anchor is still functional
    moveTo(anchorsOriginal.last)
    press(javafx.scene.input.MouseButton.PRIMARY)
    moveBy(10, 10)

    val remainingLines = ResizableLine.allLines
    assertTrue("Line 1 should still exist", remainingLines.contains(line1))
    assertTrue("Line 2 should still exist", remainingLines.contains(line2))