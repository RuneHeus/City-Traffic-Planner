package de.thm.move.controllers.drawing.GUITesting

import de.thm.move.Roads.{RoadManager, RoadType, RoadTypeManager}
import de.thm.move.views.shapes.ResizableLine
import javafx.geometry.Point2D
import javafx.scene.control.{ChoiceBox, Label}
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import org.junit.Assert.*
import org.junit.Test

import scala.jdk.CollectionConverters.*

class RoadToolbarTest extends GuiTest:


  @Test def testToolbarAppearance(): Unit =
    clickOn("#roadNormal_btn")

    val roadTypeChooser = lookup("#roadTypeChooser").queryAs(classOf[ChoiceBox[RoadType]])
    assertNotNull("ChoiceBox should be visible", roadTypeChooser)

    // Verify default road type and available options
    assertEquals("Default road type does not match", RoadType.Normal, roadTypeChooser.getValue)
    val actualItems = roadTypeChooser.getItems.asScala.toSeq
    val expectedItems = List(RoadType.Normal, RoadType.UnPaved, RoadType.Double)
    assertEquals("Road type options do not match", expectedItems, actualItems)

  @Test def testToolbarReappearanceAfterSwitching(): Unit =
    clickOn("#polygon_btn") // Switch to another toolbar
    clickOn("#roadNormal_btn") // Return to the road toolbar

    val roadTypeChooser = lookup("#roadTypeChooser").queryAs(classOf[ChoiceBox[RoadType]])
    assertNotNull("ChoiceBox should be visible after switching back", roadTypeChooser)
    assertEquals("Default road type does not match after switching back", RoadType.Normal, roadTypeChooser.getValue)

  @Test def testRoadTypeSelectionAffectsLineDrawing(): Unit =
    val unpavedColor = RoadTypeManager.roadTypeProperties(RoadType.UnPaved).head._1
    clickOn("#roadNormal_btn")

    // Select "UnPaved" road type
    val roadTypeChooser = lookup("#roadTypeChooser").queryAs(classOf[ChoiceBox[RoadType]])
    assertNotNull("ChoiceBox should not be null", roadTypeChooser)
    clickOn(roadTypeChooser)
    clickOn("UnPaved")
    assertEquals("Road type should update after dropdown selection", RoadType.UnPaved, roadTypeChooser.getValue)

    // Draw a road and verify its type
    val grid = lookup("#drawStub").queryAs(classOf[StackPane])
    assertNotNull("Grid should not be null", grid)

    val startScenePoint = grid.localToScene(grid.getLayoutBounds.getMinX + 50, grid.getLayoutBounds.getMinY + 50)
    val endScenePoint = grid.localToScene(grid.getLayoutBounds.getMinX + 200, grid.getLayoutBounds.getMinY + 200)

    moveTo(startScenePoint.getX, startScenePoint.getY)
    drag(MouseButton.PRIMARY)
    moveTo(endScenePoint.getX, endScenePoint.getY)
    drop()

    val drawnLines = ResizableLine.allLines
    assertEquals("Exactly one road should be drawn", 1, drawnLines.size)
    val drawnLine = drawnLines.head
    assertEquals("Drawn road type does not match selected type", unpavedColor, drawnLine.getFillColor)

  @Test def testRoadTypeOptions(): Unit =
    clickOn("#roadNormal_btn")

    // Verify road type options and test drawing for each type
    val roadTypeChooser = lookup("#roadTypeChooser").queryAs(classOf[ChoiceBox[RoadType]])
    assertNotNull("ChoiceBox should not be null", roadTypeChooser)

    val actualItems = roadTypeChooser.getItems.asScala.toSeq
    val expectedItems = List(RoadType.Normal, RoadType.UnPaved, RoadType.Double)
    assertEquals("Road type options do not match", expectedItems, actualItems)

    val grid = lookup("#drawStub").queryAs(classOf[StackPane])
    assertNotNull("Grid should not be null", grid)

    val gridBounds = grid.getLayoutBounds
    val baseX = gridBounds.getMinX + 50
    val baseY = gridBounds.getMinY + 50
    val offset = 50 // Offset for each road to avoid overlap

    RoadType.values.zipWithIndex.foreach { case (roadType, index) =>
      // Select road type
      clickOn(roadTypeChooser)
      clickOn(roadType.toString)
      assertEquals(s"Road type should update to $roadType", roadType, roadTypeChooser.getValue)

      val startScenePoint = grid.localToScene(baseX + index * offset, baseY)
      val endScenePoint = grid.localToScene(baseX + 100 + index * offset, baseY + 100)

      moveTo(startScenePoint.getX, startScenePoint.getY)
      drag(MouseButton.PRIMARY)
      moveTo(endScenePoint.getX, endScenePoint.getY)
      drop()

      // Verify drawn road's type
      val drawnLines = ResizableLine.allLines
      val drawnLine = drawnLines.last
      val expectedColor = RoadTypeManager.roadTypeProperties(roadType).head._1
      assertEquals(s"Drawn road type does not match selected type $roadType", expectedColor, drawnLine.getFillColor)
    }

  @Test def testRoadTypeChanger(): Unit =
    val NormalColor = RoadTypeManager.roadTypeProperties(RoadType.Normal).head._1
    val DoubleColor = RoadTypeManager.roadTypeProperties(RoadType.Double).head._1
    val UnpavedColor = RoadTypeManager.roadTypeProperties(RoadType.UnPaved).head._1
    val grid = lookup("#drawStub").queryAs(classOf[StackPane])
    clickOn("#roadTypeChooser")
    clickOn("UnPaved")
    clickOn("#roadNormal_btn")
    moveTo(grid.localToScene(grid.getLayoutBounds.getMinX + 150, grid.getLayoutBounds.getMinY))
    press(MouseButton.PRIMARY)
    moveTo(grid.localToScene(grid.getLayoutBounds.getMinX + 350, grid.getLayoutBounds.getMinY + 200))
    release(MouseButton.PRIMARY)
    val roadLine1 = ResizableLine.allLines.head
    assertEquals(roadLine1.getFillColor, UnpavedColor)

    clickOn("#roadNormal_btn")
    clickOn("#roadTypeChooser")
    clickOn("Double")
    clickOn("#roadNormal_btn")

    moveTo(grid.localToScene(grid.getLayoutBounds.getMinX + 250, grid.getLayoutBounds.getMinY))
    press(MouseButton.PRIMARY)
    moveTo(grid.localToScene(grid.getLayoutBounds.getMinX + 450, grid.getLayoutBounds.getMinY + 200))
    release(MouseButton.PRIMARY)

    sleep(1000)
    val roadLine2 = ResizableLine.allLines.tail.head
    assertEquals(roadLine2.getFillColor, DoubleColor)
    clickOn("#line_pointer")
    clickOn(grid.localToScene(200, 50))
    clickOn("#roadTypeChooser")
    clickOn("Normal")
    assertEquals(roadLine1.getFillColor, NormalColor)
    clickOn("#roadTypeChooser")
    clickOn("UnPaved")
    assertEquals(roadLine1.getFillColor, UnpavedColor)
    clickOn("#roadTypeChooser")
    clickOn("Double")
    assertEquals(roadLine1.getFillColor, DoubleColor)
    clickOn("#roadTypeChooser") // test if everything is ok if you change to the same road type as the current one
    clickOn("Double")
    assertEquals(roadLine1.getFillColor, DoubleColor)
    ResizableLine.allLines.clear()

  @Test def testOneWayButton(): Unit =
    // INITIALIZATION: Initialize + draw 2 lines
    val roadLabel = lookup("#oneWayDirectionLabel").queryAs(classOf[Label])
    val grid = lookup("#drawStub").queryAs(classOf[StackPane])

    // Draw line 1
    clickOn("#roadNormal_btn")
    moveTo(grid.localToScene(grid.getLayoutBounds.getMinX + 200, grid.getLayoutBounds.getMinY))
    press(MouseButton.PRIMARY)
    moveTo(grid.localToScene(grid.getLayoutBounds.getMinX + 400, grid.getLayoutBounds.getMinY + 200))
    release(MouseButton.PRIMARY)

    // Draw line 2
    moveTo(grid.localToScene(grid.getLayoutBounds.getMinX + 500, grid.getLayoutBounds.getMinY))
    press(MouseButton.PRIMARY)
    moveTo(grid.localToScene(grid.getLayoutBounds.getMinX + 700, grid.getLayoutBounds.getMinY + 200))
    release(MouseButton.PRIMARY)
    val roadLine = ResizableLine.allLines.head

    clickOn("#line_pointer")
    clickOn("#OneWayButton")
    assertEquals("N/A", roadLabel.getText)
    clickOn(roadLine)
    assertEquals("None", roadLabel.getText)
    clickOn("#OneWayButton")
    assertEquals("Front", roadLabel.getText)
    assert(roadLine.getStrokeWidth == RoadManager.oneWayThickness, "Width after changing to Front does not match")
    clickOn("#OneWayButton")
    assert(roadLine.getStrokeWidth == RoadManager.oneWayThickness, "Width after changing to Back does not match")
    assertEquals("Back", roadLabel.getText)
    clickOn("#OneWayButton")
    assertEquals("None", roadLabel.getText)
    assert(roadLine.getStrokeWidth == RoadTypeManager.roadTypeProperties(RoadType.Normal).head._3, "Width after changing back to normal does not match")
    clickOn("#OneWayButton")
    assertEquals("Front", roadLabel.getText)
    clickOn(grid.localToScene(550, 50))
    assertEquals("None", roadLabel.getText)

    clickOn("#line_pointer")
    assertEquals("N/A", roadLabel.getText)
    ResizableLine.allLines.clear()

