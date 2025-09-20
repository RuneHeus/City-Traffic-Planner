package de.thm.move.controllers.drawing.GUITesting

import de.thm.move.views.shapes.ResizableLine
import javafx.geometry.Point2D
import javafx.scene.layout.StackPane
import org.junit.Assert.*
import org.junit.Test
import scala.math._
import javafx.geometry.Bounds
import javafx.scene.control.ToggleButton

class TrafficLightTest extends GuiTest:
  private val TOLERANCE = 3.0

  // Helper: Calculate Euclidean distance
  private def dist(p1: (Double, Double), p2: (Double, Double)): Double =
    sqrt(pow(p1._1 - p2._1, 2) + pow(p1._2 - p2._2, 2))

  @Test def testAddTrafficLight(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw three lines connected at a shared anchor
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(150, 110), new Point2D(200, 100))
    val line3 = drawLine(grid, gridBoundsInLocal, new Point2D(250, 105), new Point2D(200, 100))

    val anchorEnd = line1.getAnchors.last

    // Add a traffic light to the shared anchor
    clickOn("#line_pointer")
    clickOn(anchorEnd)
    clickOn("#addRemoveTrafficLightButton")

    assertTrue("Traffic light should be present on the anchor", anchorEnd.getTrafficLight.isDefined)

  @Test def testRemoveTrafficLight(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw three lines connected at a shared anchor
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(150, 110), new Point2D(200, 100))
    val line3 = drawLine(grid, gridBoundsInLocal, new Point2D(250, 105), new Point2D(200, 100))

    val anchorEnd = line1.getAnchors.last

    // Add and then remove a traffic light
    clickOn("#line_pointer")
    clickOn(anchorEnd)
    clickOn("#addRemoveTrafficLightButton")
    assertTrue("Traffic light should be present on the anchor after adding", anchorEnd.getTrafficLight.isDefined)

    clickOn("#addRemoveTrafficLightButton")
    assertTrue("Traffic light should be removed from the anchor", anchorEnd.getTrafficLight.isEmpty)

  @Test def testMoveAnchorWithTrafficLight(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw three lines connected at a shared anchor
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(150, 110), new Point2D(200, 100))
    val line3 = drawLine(grid, gridBoundsInLocal, new Point2D(250, 105), new Point2D(200, 100))

    val anchorEnd = line1.getAnchors.last

    // Add a traffic light to the shared anchor
    clickOn("#line_pointer")
    clickOn(anchorEnd)
    clickOn("#addRemoveTrafficLightButton")
    assertTrue("Traffic light should be present on the anchor", anchorEnd.getTrafficLight.isDefined)

    // Move the anchor and verify traffic light offset remains correct
    moveTo(anchorEnd)
    drag()
    moveBy(0, -20)
    release(javafx.scene.input.MouseButton.PRIMARY)

    val trafficLight = anchorEnd.getTrafficLight.get
    val anchorBounds = anchorEnd.localToParent(anchorEnd.getBoundsInLocal)

    val anchorPosition = (anchorBounds.getMinX, anchorBounds.getMinY)
    val trafficLightPosition = (trafficLight.getLayoutX, trafficLight.getLayoutY)
    val actualOffset = (trafficLightPosition._1 - anchorPosition._1, trafficLightPosition._2 - anchorPosition._2)
    val expectedOffset = (10.0, 10.0)

    assertEquals("Traffic light should maintain correct X offset", expectedOffset._1, actualOffset._1, TOLERANCE)
    assertEquals("Traffic light should maintain correct Y offset", expectedOffset._2, actualOffset._2, TOLERANCE)

  @Test def testRemoveTrafficLightWhenConnectionsDropBelowThreeByDeletingLine(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw three lines connected at a shared anchor
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(150, 110), new Point2D(200, 100))
    val line3 = drawLine(grid, gridBoundsInLocal, new Point2D(250, 105), new Point2D(200, 100))

    val anchorEnd = line1.getAnchors.last

    // Add a traffic light to the shared anchor
    clickOn("#line_pointer")
    clickOn(anchorEnd)
    clickOn("#addRemoveTrafficLightButton")
    assertTrue("Traffic light should be present on the anchor", anchorEnd.getTrafficLight.isDefined)

    // Remove a connection and ensure traffic light is removed
    clickOn(line2)
    press(javafx.scene.input.KeyCode.BACK_SPACE)
    release(javafx.scene.input.KeyCode.BACK_SPACE)

    assertTrue("Traffic light should be removed when fewer than 3 connections", anchorEnd.getTrafficLight.isEmpty)

  @Test def testRemoveTrafficLightWhenConnectionsDropBelowThreeByDeletingAnchor(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Draw three lines connected at a shared anchor
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(150, 110), new Point2D(200, 100))
    val line3 = drawLine(grid, gridBoundsInLocal, new Point2D(250, 105), new Point2D(200, 100))

    val anchors = line1.getAnchors

    // Add a traffic light to the shared anchor
    clickOn("#line_pointer")
    clickOn(anchors.last)
    clickOn("#addRemoveTrafficLightButton")
    assertTrue("Traffic light should be present on the anchor", anchors.last.getTrafficLight.isDefined)

    // Remove a connection and ensure traffic light is removed
    clickOn(anchors.head)
    press(javafx.scene.input.KeyCode.BACK_SPACE)
    release(javafx.scene.input.KeyCode.BACK_SPACE)

    assertTrue("Traffic light should be removed when fewer than 3 connections", anchors.last.getTrafficLight.isEmpty)

  @Test def testTrafficLightButtonState(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(150, 110), new Point2D(200, 100))
    val line3 = drawLine(grid, gridBoundsInLocal, new Point2D(250, 105), new Point2D(200, 100))

    val anchorEnd = line1.getAnchors.last
    clickOn("#line_pointer")
    clickOn(anchorEnd)

    val button = lookup("#addRemoveTrafficLightButton").queryAs(classOf[javafx.scene.control.Button])
    assertEquals("Add Traffic Light", button.getText)

    clickOn(button)
    assertEquals("Remove Traffic Light", button.getText)

    clickOn(button)
    assertEquals("Add Traffic Light", button.getText)

  @Test def testCannotAddTrafficLightWithTwoConnections(): Unit =
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()
    val line1 = drawLine(grid, gridBoundsInLocal, new Point2D(100, 100), new Point2D(200, 100))
    val line2 = drawLine(grid, gridBoundsInLocal, new Point2D(150, 110), new Point2D(200, 100))

    val anchorEnd = line1.getAnchors.last
    clickOn("#line_pointer")
    clickOn(anchorEnd)

    val button = lookup("#addRemoveTrafficLightButton").queryAs(classOf[javafx.scene.control.Button])
    assertTrue("Button should be disabled when fewer than 3 connections", button.isDisable)

