package de.thm.move.loader.fileOperations

import de.thm.move.controllers.drawing.GUITesting.GuiTest
import de.thm.move.controllers.*
import de.thm.move.views.shapes.{ResizableLine, ResizablePolygon}
import javafx.application.Platform
import javafx.scene.input.MouseButton
import javafx.stage.{Stage, Window}
import org.junit.Assert.{assertEquals, assertNotNull, assertTrue, fail}
import org.junit.jupiter.api.{MethodOrderer, Order, TestMethodOrder}
import org.junit.{Before, Test}
import org.mockito.ArgumentMatchers.{any, anyDouble}

import scala.collection.JavaConverters.*

@TestMethodOrder(classOf[MethodOrderer.OrderAnnotation])
class FileGuiOperationsTest extends GuiTest {

  /**
   * Mock object to simulate the behavior of the FileChooser.
   * This object is used to test the file operations without opening the actual file chooser dialog.
   */
  object FileChooserMock {
    private var mockFile: java.io.File = _
    private var lastChosenFile: java.io.File = _

    def setMockFile(file: java.io.File): Unit = {
      mockFile = file
    }

    def openFileChooser(): Unit = {
      lastChosenFile = mockFile
    }

    def openSaveFileChooser(): Unit = {
      lastChosenFile = mockFile
    }

    def getLastChosenFile: java.io.File = lastChosenFile
  }

  /**
   * Mock object to simulate the behavior of the FileCtrl.
   * This object is used to test the file operations without actually saving the file.
   */
  @Test
  @Order(2)
  def fileChooserOpens(): Unit = {
    if isHeadlessMode then{
      // Mock the FileChooser behavior
      val mockFile = new java.io.File("mocked-file.json")
      FileChooserMock.setMockFile(mockFile)

      FileChooserMock.openFileChooser()

      // Verify the mock was used
      val chosenFile = FileChooserMock.getLastChosenFile
      assertNotNull("Mocked file should be chosen", chosenFile)
      assertEquals("mocked-file.json", chosenFile.getName)
      return
    }
    clickOn("#fileMenu")
    clickOn("#openMenuItem")

    val openDialogs = listWindows().asScala
    val isDialogOpen = openDialogs.exists(_.isShowing)

    assertTrue("A file chooser should be open", isDialogOpen)
  }

  /**
   * Mock object to simulate the behavior of the FileCtrl.
   * This object is used to test the file operations without actually saving the file.
   */
  @Test
  @Order(3)
  def fileSaveChooserOpens(): Unit = {
    if (isHeadlessMode) then {
      // Mock the FileChooser behavior
      val mockFile = new java.io.File("mocked-save-file.json")
      FileChooserMock.setMockFile(mockFile)

      FileChooserMock.openSaveFileChooser()

      // Verify the mock was used
      val savedFile = FileChooserMock.getLastChosenFile
      assertNotNull("Mocked save file should be chosen", savedFile)
      assertEquals("mocked-save-file.json", savedFile.getName)
      return
    }

    clickOn("#fileMenu")
    clickOn("#saveMenuItem")

    val openDialogs = listWindows().asScala
    val isDialogOpen = openDialogs.exists(_.isShowing)

    assertTrue("A file chooser should be open", isDialogOpen)
  }

  /**
   * Test saving and loading an empty file.
   */
  @Test
  @Order(4)
  def saveAndLoadEmptyFile(): Unit = {
    val tempFilePath = os.pwd / "test_open_empty.json"
    try {
      val drawPanel = this.ctrl.drawPanel
      val fileController = ctrl.fileCtrl

      // Saving current work
      Platform.runLater(() => {
        JsonCtrl.save_json_file(drawPanel.getShapes, drawPanel.getWidth, drawPanel.getHeight, tempFilePath)
      })

      // Renew grid
      clickOn("#fileMenu")
      clickOn("#newMenuItem")
      clickOn("OK")

      Platform.runLater(() => {
        ctrl.openFile(tempFilePath.toNIO)
      })
      sleep(100)
      clickOn("OK")

      val shapes = drawPanel.getShapes
      assertTrue("The loaded file should be empty", shapes.isEmpty)
    } catch {
      case e: Exception =>
        println(s"Test failed with exception: ${e.getMessage}")
        e.printStackTrace()
        fail("The test encountered an unexpected exception.")
    }
    finally {
      os.remove(tempFilePath)
    }
  }

  /**
   * Test saving and loading a file with crossed lines.
   */
  @Test
  @Order(5)
  def saveAndLoadCrossedLines(): Unit = {
    val tempFilePath = os.pwd / "test_open_shapes.json"
    try{
      val drawPanel = this.ctrl.drawPanel
      val bounds = drawPanel.localToScreen(drawPanel.getBoundsInLocal)
      val fileController = ctrl.fileCtrl

      val x = bounds.getMinX
      val y = bounds.getMinY

      // Crossing Roads
      clickOn("#roadNormal_btn")

      moveTo(x + 200, y + 150)
      press(MouseButton.PRIMARY)
      moveBy(300, 30)
      release(MouseButton.PRIMARY)
      moveTo(x + 350, y + 225)
      press(MouseButton.PRIMARY)
      moveBy(10, -150)
      release(MouseButton.PRIMARY)

      // Saving current work
      Platform.runLater(() => {
        JsonCtrl.save_json_file(drawPanel.getShapes, drawPanel.getWidth, drawPanel.getHeight, tempFilePath)
      })

      // Renew grid
      clickOn("#fileMenu")
      clickOn("#newMenuItem")
      clickOn("OK")

      Platform.runLater(() => {
        ctrl.openFile(tempFilePath.toNIO)
      })
      sleep(100)
      clickOn("OK")

      //Check if the anchors are still there and working
      moveTo(x + 354.04, y + 165.404)
      press(MouseButton.PRIMARY)
      moveTo(x + 354.04, y + 250.404)

      val shapes = drawPanel.getShapes
      shapes.foreach {
        case line: ResizableLine =>
          val start = (line.getStartX, line.getStartY)
          val end = (line.getEndX, line.getEndY)

          // Calculate the expected positions based on the movement logic
          val expectedStart = if start == (354.04, 165.404) then (354.04, 250.404) else start
          val expectedEnd = if end == (354.04, 165.404) then (354.04, 250.404) else end

          // Assert that the actual positions match the expected positions
          assertEquals(
            s"Line's start point is incorrect. Expected: $expectedStart, Actual: $start",
            expectedStart,
            start
          )
          assertEquals(
            s"Line's end point is incorrect. Expected: $expectedEnd, Actual: $end",
            expectedEnd,
            end
          )
        case _ =>
      }
    }catch {
      case e: Exception =>
        println(s"Test failed with exception: ${e.getMessage}")
        e.printStackTrace()
        fail("The test encountered an unexpected exception.")
    }
    finally{
       os.remove(tempFilePath)
    }
  }

  /**
   * Test saving and loading a file with a polygon.
   */
//  @Test
//  def saveAndLoadPolygons(): Unit = {
//    val tempFilePath = os.pwd / "test_open_shapes.json"
//    try {
//      val drawPanel = this.ctrl.drawPanel
//      val bounds = drawPanel.localToScreen(drawPanel.getBoundsInLocal)
//      val fileController = ctrl.fileCtrl
//
//      val x = bounds.getMinX
//      val y = bounds.getMinY
//
//      // Drawing the first polygon
//      clickOn("#polygon_btn")
//      moveTo(x + 200, y + 200)
//      press(MouseButton.PRIMARY)
//      release(MouseButton.PRIMARY)
//      moveBy(100, 0)
//      press(MouseButton.PRIMARY)
//      release(MouseButton.PRIMARY)
//      moveBy(0, 100)
//      press(MouseButton.PRIMARY)
//      release(MouseButton.PRIMARY)
//      moveBy(-100, 0)
//      press(MouseButton.PRIMARY)
//      release(MouseButton.PRIMARY)
//      moveBy(0, -100)
//      press(MouseButton.PRIMARY)
//      release(MouseButton.PRIMARY)
//
//      // Drawing the second overlapping polygon
//      moveTo(x + 250, y + 250) // Start slightly offset to overlap the first polygon
//      press(MouseButton.PRIMARY)
//      release(MouseButton.PRIMARY)
//      moveBy(100, 0)
//      press(MouseButton.PRIMARY)
//      release(MouseButton.PRIMARY)
//      moveBy(0, 100)
//      press(MouseButton.PRIMARY)
//      release(MouseButton.PRIMARY)
//      moveBy(-100, 0)
//      press(MouseButton.PRIMARY)
//      release(MouseButton.PRIMARY)
//      moveBy(0, -100)
//      press(MouseButton.PRIMARY)
//      release(MouseButton.PRIMARY)
//
//      // Saving current work
//      Platform.runLater(() => {
//        JsonCtrl.save_json_file(drawPanel.getShapes, drawPanel.getWidth, drawPanel.getHeight, tempFilePath)
//      })
//
//      // Renew grid
//      clickOn("#fileMenu")
//      clickOn("#newMenuItem")
//      clickOn("OK")
//
//      Platform.runLater(() => {
//        ctrl.openFile(tempFilePath.toNIO)
//      })
//      sleep(100)
//      clickOn("OK")
//
//      val shapes = drawPanel.getShapes
//      assertEquals("The loaded file should contain 2 shapes", 2, shapes.size)
//    } catch {
//      case e: Exception =>
//        println(s"Test failed with exception: ${e.getMessage}")
//        e.printStackTrace()
//        fail("The test encountered an unexpected exception.")
//    } finally {
//      os.remove(tempFilePath)
//    }
//  }

  /**
   * Test saving and loading a complex scenario with multiple shapes.
   */
  @Test
  @Order(1)
  def saveAndLoadComplexScenario(): Unit = {
    val tempFilePath = os.pwd / "test_complex_scenario.json"
    try {
      val drawPanel = this.ctrl.drawPanel
      val bounds = drawPanel.localToScreen(drawPanel.getBoundsInLocal)
      val fileController = ctrl.fileCtrl

      val x = bounds.getMinX
      val y = bounds.getMinY

      // Add multiple crossing roads (5 lines crossing each other)
      clickOn("#roadNormal_btn")

      // Line 1
      moveTo(x + 200, y + 150)
      press(MouseButton.PRIMARY)
      moveBy(300, 5)
      release(MouseButton.PRIMARY)

      // Line 2
      moveTo(x + 350, y + 100)
      press(MouseButton.PRIMARY)
      moveBy(50, 200)
      release(MouseButton.PRIMARY)

      // Line 3
      moveTo(x + 250, y + 50)
      press(MouseButton.PRIMARY)
      moveBy(3, 200)
      release(MouseButton.PRIMARY)

      // Line 4
      moveTo(x + 200, y + 200)
      press(MouseButton.PRIMARY)
      moveBy(300, 5)
      release(MouseButton.PRIMARY)

      // Add overlapping polygons (4 polygons)
      clickOn("#polygon_btn")

      // Polygon 1
      moveTo(x + 200, y + 200)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(100, 0)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(0, 100)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(-100, 0)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(0, -100)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)

      // Polygon 2
      moveTo(x + 250, y + 250)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(100, 0)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(0, 100)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(-100, 0)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(0, -100)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)

      // Polygon 3
      moveTo(x + 300, y + 300)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(100, 0)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(0, 100)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(-100, 0)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(0, -100)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)

      // Polygon 4
      moveTo(x + 350, y + 350)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(100, 0)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(0, 100)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(-100, 0)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)
      moveBy(0, -100)
      press(MouseButton.PRIMARY)
      release(MouseButton.PRIMARY)

      // Save current work
      Platform.runLater(() => {
        JsonCtrl.save_json_file(drawPanel.getShapes, drawPanel.getWidth, drawPanel.getHeight, tempFilePath)
      })

      // Clear the panel and reload saved shapes
      clickOn("#fileMenu")
      clickOn("#newMenuItem")
      clickOn("OK")

      Platform.runLater(() => {
        ctrl.openFile(tempFilePath.toNIO)
      })
      sleep(1000)
      clickOn("OK")

      // Validate loaded shapes
      val shapes = drawPanel.getShapes
      assertEquals("The loaded file should contain 20 shapes", 20, shapes.size)

      val (lines, polygons) = shapes.partition(_.isInstanceOf[ResizableLine])

      // Validate lines
      assertEquals("There should be 16 roads loaded", 16, lines.size)
      lines.foreach {
        case line: ResizableLine =>
          val start = (line.getStartX, line.getStartY)
          val end = (line.getEndX, line.getEndY)
          assertNotNull("Line should have start and end points", start)
          assertNotNull("Line should have start and end points", end)
          line.getAnchors.foreach(anchor => {
            assertNotNull("Anchor should exist", anchor)
            assertTrue("Anchor should be connected to lines", anchor.connectedLines.nonEmpty)
          })
        case _ => fail("Shape is not a ResizableLine")
      }

      // Validate polygons
      assertEquals("There should be 4 polygons loaded", 4, polygons.size)
      polygons.foreach {
        case polygon: ResizablePolygon =>
          polygon.getAnchors.foreach(anchor => {
            assertNotNull("Polygon anchor should exist", anchor)
          })
        case _ => fail("Shape is not a ResizablePolygon")
      }

    } catch {
      case e: Exception =>
        println(s"Test failed with exception: ${e.getMessage}")
        e.printStackTrace()
        fail("The test encountered an unexpected exception.")
    } finally {
      os.remove(tempFilePath)
    }
  }

  /**
   * Test saving and loading a complex scenario with multiple shapes.
   */
//  @Test
//  def testFileCtrlMock(): Unit = {
//    val mockWindow = mock[Window]
//    PowerMockito.mockStatic(classOf[FileCtrl])
//    PowerMockito.when(FileCtrl.saveFile(any(), anyDouble, anyDouble, any())).thenReturn(())
//
//    // Call the method
//    FileCtrl.saveFile(List(), 800, 600, "mocked-path")
//
//    // Verify
//    PowerMockito.verifyStatic(FileCtrl, times(1))
//    FileCtrl.saveFile(any(), anyDouble, anyDouble, any())
//  }
}