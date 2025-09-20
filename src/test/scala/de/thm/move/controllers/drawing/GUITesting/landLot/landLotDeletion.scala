package de.thm.move.controllers.drawing.GUITesting.landLot

import de.thm.move.Roads.RoadManager
import de.thm.move.controllers.drawing.GUITesting.GuiTest
import de.thm.move.views.shapes.{ResizableLandLot, ResizableLine, ResizablePolygon}
import javafx.geometry.Point2D
import javafx.geometry.Bounds
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import org.junit.Assert._
import org.junit.Test
import javafx.scene.input.KeyCode




class landLotDeletion extends GuiTest:
  val firstPointL1 = new Point2D(250, 60)
  val secondPointL1 = new Point2D(400, 60)
  val thirdPointL1 = new Point2D(400, 200)
  val fourthPointL1 = new Point2D(250, 200)

  // Define points for the zone
  val firstPointP1 = new Point2D(100, 20)
  val secondPointP1 = new Point2D(600, 20)
  val thirdPointP1 = new Point2D(600, 300)
  val fourthPointP1 = new Point2D(100, 300)

  // Define points for the road
  val firstPointR1 = new Point2D(200, 40)
  val secondPointR1 = new Point2D(500, 40)

  //road anchors moved such that part of the land lot is outside the zone.
  @Test def moveRoadAnchor(): Unit =
    ResizablePolygon.clearPolygons()
    ResizableLandLot.clearLandLots()
    ResizableLine.allLines.clear()

    // Setup environment
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()


    val draggingPoint = new Point2D(780, 40)

    clickOn("#polygon_btn")
    Seq(firstPointP1, secondPointP1, thirdPointP1, fourthPointP1, firstPointP1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)

    clickOn("#roadNormal_btn")
    val line = drawLine(grid, gridBoundsInLocal, firstPointR1, secondPointR1)

    clickOn("#rectangle_btn")
    Seq(firstPointL1, secondPointL1, thirdPointL1, fourthPointL1, firstPointL1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)


    // Debug the first assertion
    val landLotsRegistered = ResizableLandLot.getLots.nonEmpty

    assertTrue("Land lot has been registered", landLotsRegistered)

    clickOn("#line_pointer")
    val anchors = line.getAnchors
    val localDraggingPoint = gridToLocal(gridBoundsInLocal, draggingPoint)
    moveTo(anchors.last)
    drag()
    moveBy(220,0)
    release(MouseButton.PRIMARY)
    sleep(600)

    assertTrue("Land lot has been deleted", ResizableLandLot.getLots.isEmpty)





  //road moved such that part of the land lot is outside the zone.
  @Test def moveRoad(): Unit =
    ResizablePolygon.clearPolygons()
    ResizableLandLot.clearLandLots()
    ResizableLine.allLines.clear()

    // Setup environment
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    val selectionPointRoad = new Point2D(450, 40)
    val draggingPoint = new Point2D(750, 40)

    clickOn("#polygon_btn")
    Seq(firstPointP1, secondPointP1, thirdPointP1, fourthPointP1, firstPointP1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)

    val line = drawLine(grid, gridBoundsInLocal, firstPointR1, secondPointR1)

    clickOn("#rectangle_btn")
    Seq(firstPointL1, secondPointL1, thirdPointL1, fourthPointL1, firstPointL1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)
    clickOn("#line_pointer")

    val landLotsRegistered = ResizableLandLot.getLots.nonEmpty
    assertTrue("Land lot has been registered", landLotsRegistered)

    //move the road such that a part of the land lot is outside the zone.
    Seq(selectionPointRoad).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      val localDraggingPoint = gridToLocal(gridBoundsInLocal, draggingPoint)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      drag()
      moveTo(grid.localToScreen(localDraggingPoint).getX, grid.localToScreen(localDraggingPoint).getY)
      release(MouseButton.PRIMARY)
    }
    sleep(600)

    assertTrue("Land lot has been deleted", ResizableLandLot.getLots.isEmpty)






  //zone anchors moved such that part of the land lot is outside the zone.
  @Test def moveZoneAnchor(): Unit =
    println("Starting moveRoad test...")
    // Clear previous data and log the state
    println("Clearing polygons, land lots, and lines...")
    ResizablePolygon.clearPolygons()
    ResizableLandLot.clearLandLots()
    ResizableLine.allLines.clear()

    // Setup environment
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()


    val selectionPointZone = fourthPointP1
    val draggingPoint = secondPointL1

    clickOn("#polygon_btn")
    Seq(firstPointP1, secondPointP1, thirdPointP1, fourthPointP1, firstPointP1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)

    val line = drawLine(grid, gridBoundsInLocal, firstPointR1, secondPointR1)

    clickOn("#rectangle_btn")
    Seq(firstPointL1, secondPointL1, thirdPointL1, fourthPointL1, firstPointL1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)
    clickOn("#line_pointer")

    val landLotsRegistered = ResizableLandLot.getLots.nonEmpty
    assertTrue("Land lot has been registered", landLotsRegistered)

    //move one anchor of the zone such that part of the land lot falls outside the zone.
    Seq(selectionPointZone).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      val localDraggingPoint = gridToLocal(gridBoundsInLocal, draggingPoint)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      drag()
      moveTo(grid.localToScreen(localDraggingPoint).getX, grid.localToScreen(localDraggingPoint).getY)
      release(MouseButton.PRIMARY)
    }
    sleep(600)

    assertTrue("Land lot has been deleted", ResizableLandLot.getLots.isEmpty)





  //zone moved such that part of the land lot is outside the zone.
  @Test def moveZone(): Unit =
    ResizablePolygon.clearPolygons()
    ResizableLandLot.clearLandLots()
    ResizableLine.allLines.clear()

    // Setup environment
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()


    val selectionPointZone = new Point2D(450, 200)
    val draggingPoint = new Point2D(600, 200)

    clickOn("#polygon_btn")
    Seq(firstPointP1, secondPointP1, thirdPointP1, fourthPointP1, firstPointP1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)

    val line = drawLine(grid, gridBoundsInLocal, firstPointR1, secondPointR1)

    clickOn("#rectangle_btn")
    Seq(firstPointL1, secondPointL1, thirdPointL1, fourthPointL1, firstPointL1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)
    clickOn("#line_pointer")

    val landLotsRegistered = ResizableLandLot.getLots.nonEmpty
    assertTrue("Land lot has been registered", landLotsRegistered)

    //move one anchor of the zone such that part of the land lot falls outside the zone.
    Seq(selectionPointZone).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      val localDraggingPoint = gridToLocal(gridBoundsInLocal, draggingPoint)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      drag()
      moveTo(grid.localToScreen(localDraggingPoint).getX, grid.localToScreen(localDraggingPoint).getY)
      release(MouseButton.PRIMARY)
    }
    sleep(600)

    assertTrue("Land lot has been deleted", ResizableLandLot.getLots.isEmpty)


  //second zone drawn that overlaps with the first one. This causes the land lot to inside two zones which is not allowed.
  @Test def OverlappingZone(): Unit =
    ResizablePolygon.clearPolygons()
    ResizableLandLot.clearLandLots()
    ResizableLine.allLines.clear()

    // Setup environment
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    // Define points for the zone
    val firstPointP2 = new Point2D(350, 10)
    val secondPointP2 = new Point2D(650, 10)
    val thirdPointP2 = new Point2D(650, 350)
    val fourthPointP2 = new Point2D(350, 350)


    clickOn("#polygon_btn")
    Seq(firstPointP1, secondPointP1, thirdPointP1, fourthPointP1, firstPointP1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)

    val line = drawLine(grid, gridBoundsInLocal, firstPointR1, secondPointR1)

    clickOn("#rectangle_btn")
    Seq(firstPointL1, secondPointL1, thirdPointL1, fourthPointL1, firstPointL1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)
    clickOn("#line_pointer")

    val landLotsRegistered = ResizableLandLot.getLots.nonEmpty
    assertTrue("Land lot has been registered", landLotsRegistered)

    clickOn("#polygon_btn")
    Seq(firstPointP2, secondPointP2, thirdPointP2, fourthPointP2, firstPointP2).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)

    assertTrue("Land lot has been deleted", ResizableLandLot.getLots.isEmpty)


  //road deleted such that all the lots connected to that road should be deleted as well.
  @Test def deleteRoad(): Unit =
    ResizablePolygon.clearPolygons()
    ResizableLandLot.clearLandLots()
    ResizableLine.allLines.clear()

    // Setup environment
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()


    val selectionPointRoad = new Point2D(450, 40)

    clickOn("#polygon_btn")
    Seq(firstPointP1, secondPointP1, thirdPointP1, fourthPointP1, firstPointP1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)

    val line = drawLine(grid, gridBoundsInLocal, firstPointR1, secondPointR1)

    clickOn("#rectangle_btn")
    Seq(firstPointL1, secondPointL1, thirdPointL1, fourthPointL1, firstPointL1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)
    clickOn("#line_pointer")


    val landLotsRegistered = ResizableLandLot.getLots.nonEmpty
    assertTrue("Land lot has been registered", landLotsRegistered)

    //move one anchor of the zone such that part of the land lot falls outside the zone.
    Seq(selectionPointRoad).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
      press(KeyCode.BACK_SPACE)
    }
    sleep(600)

    assertTrue("Land lot has been deleted", ResizableLandLot.getLots.isEmpty)






  //deleted zone such that the land lots inside this zone should be deleted.
  @Test def deleteZone(): Unit =
    ResizablePolygon.clearPolygons()
    ResizableLandLot.clearLandLots()
    ResizableLine.allLines.clear()

    // Setup environment
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()


    val selectionPointZone = new Point2D(450, 100)

    clickOn("#polygon_btn")
    Seq(firstPointP1, secondPointP1, thirdPointP1, fourthPointP1, firstPointP1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)

    val line = drawLine(grid, gridBoundsInLocal, firstPointR1, secondPointR1)

    clickOn("#rectangle_btn")
    Seq(firstPointL1, secondPointL1, thirdPointL1, fourthPointL1, firstPointL1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)
    clickOn("#line_pointer")
    val landLotsRegistered = ResizableLandLot.getLots.nonEmpty
    assertTrue("Land lot has been registered", landLotsRegistered)

    //move one anchor of the zone such that part of the land lot falls outside the zone.
    Seq(selectionPointZone).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
      press(KeyCode.BACK_SPACE)
    }
    sleep(600)

    assertTrue("Land lot has been deleted", ResizableLandLot.getLots.isEmpty)