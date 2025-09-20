package de.thm.move.controllers.drawing.GUITesting

import de.thm.move.{Global, MoveApp, MoveAppCls}
import de.thm.move.controllers.MoveCtrl
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.Bounds
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.robot.Robot
import javafx.scene.{Parent, Scene}
import javafx.stage.{Stage, WindowEvent}
import org.junit.{After, Before}
import org.testfx.api.FxRobot
import org.testfx.framework.junit.ApplicationTest
import de.thm.move.Roads.RoadManager
import de.thm.move.views.shapes.ResizableLine
import org.junit.Assert.*
import javafx.geometry.Point2D
import javafx.geometry.Bounds
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import org.junit.Assert.*
import de.thm.move.views.shapes.{ResizableLandLot, ResizableLine, ResizablePolygon}
import javafx.scene.control.ToggleButton

import java.nio.file.Paths

/**
 * Base class for writing TestFX tests
 */
abstract class GuiTest extends ApplicationTest:

  var ctrl: MoveCtrl = _
  var stage: Stage = _

  override def start(stg: Stage): Unit =
    stage = stg
    val windowWidth = Global.config.getDouble("window.width").getOrElse(600.0)
    val windowHeight = Global.config.getDouble("window.height").getOrElse(600.0)
    val fxmlLoader = new FXMLLoader(MoveApp.getClass.getResource("/fxml/move.fxml"))
    fxmlLoader.setResources(Global.fontBundle)
    val mainViewRoot: Parent = fxmlLoader.load()
    val scene = new Scene(mainViewRoot)
    scene.getStylesheets.add(Global.styleSheetUrl)

    stage.setTitle(Global.config.getString("window.title").getOrElse(""))
    stage.setScene(scene)
    stage.setWidth(windowWidth)
    stage.setHeight(windowHeight)
    stage.show()
    ctrl = fxmlLoader.getController[MoveCtrl]
    ctrl.setupMove(stage, None)

    stage.setOnCloseRequest((_: WindowEvent) => ctrl.shutdownMove())

  // This convinces the Scala compiler to consider a `() => Unit` as a `Runnable`
  // to prevent an ambiguous overload.
  override def interact(callable: Runnable): FxRobot =
    super.interact(callable)


  /**
   * Clear shared state before each test.
   */
  @Before def clearState(): Unit =
    interact(() => {
      ResizableLine.allLines.clear()
      RoadManager.roads.clear() // Clear all roads
      RoadManager.nodes.clear()
    })

  /**
   *   Ensure shared state is cleared after each test.
   */
  @After def tearDown(): Unit =
    interact(() => {
      ResizableLine.allLines.clear()
      RoadManager.roads.clear() // Clear all roads
      RoadManager.nodes.clear()
    })

  /**
   * Get color of the pixel under the mouse pointer.
   */
  def getColorUnderPointer: Color =
    // HACK: We need to use `interact` so that we execute the actions on the GUI thread. However,
    // `interact` does not return the return value of the callable. However, `interact` is a blocking
    // call so we can assign inside its body.
    var color: Color = null
    interact(() => {
      val robot = new Robot()
      color = robot.getPixelColor(robot.getMousePosition)
    })
    color

  /**
   * Take a screenshot of the current window and save it to the "target/gui-screenshots"
   * folder.
   *
   * @param name Name of the screenshot file.
   */
  def saveScreenshot(name: String): Unit =
    val targetPath = Paths.get("target", "gui-screenshots", "%s.png".format(name))
    targetPath.getParent.toFile.mkdirs()

    interact(() => {
      val cs = robotContext().getCaptureSupport
      val image = cs.captureNode(targetWindow.getScene.getRoot)
      cs.saveImage(image, targetPath)
    })


  def isHeadlessMode: Boolean = {
    System.getProperty("testfx.headless", "false").toBoolean
  }



  //Helper code for making tests not fail in the pipeline.
  def setupEnvironmentAndGrid(): (StackPane, Bounds) =
    val grid = lookup("#drawStub").queryAs(classOf[StackPane])
    assertNotNull("Grid should not be null", grid)
    clickOn("#roadNormal_btn")
    (grid, grid.getBoundsInLocal)

  // Helper: Convert grid coordinates to local coordinates
  def gridToLocal(gridBoundsInLocal: Bounds, point: Point2D): Point2D =
    new Point2D(gridBoundsInLocal.getMinX + point.getX, gridBoundsInLocal.getMinY + point.getY)

  // Helper: Draw a line and return the resulting ResizableLine
  def drawLine(grid: StackPane, gridBoundsInLocal: Bounds, start: Point2D, end: Point2D): ResizableLine =
    val roadButton = lookup("#roadNormal_btn").queryAs(classOf[ToggleButton])
    val toggleGroup = roadButton.getToggleGroup // Get the ToggleGroup directly from the button
    val currentToggle = toggleGroup.getSelectedToggle // Get the currently selected toggle

    if (currentToggle != roadButton) then
      clickOn(roadButton) // Activate the button if it’s not the currently selected toggle
    val startLocal = gridToLocal(gridBoundsInLocal, start)
    val endLocal = gridToLocal(gridBoundsInLocal, end)
    moveTo(grid.localToScreen(startLocal).getX, grid.localToScreen(startLocal).getY)
    press(javafx.scene.input.MouseButton.PRIMARY)
    moveTo(grid.localToScreen(endLocal).getX, grid.localToScreen(endLocal).getY)
    release(javafx.scene.input.MouseButton.PRIMARY)
    ResizableLine.allLines.last


