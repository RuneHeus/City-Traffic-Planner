package de.thm.move.controllers.drawing.GUITesting.zone

import de.thm.move.views.shapes.ResizablePolygon
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.layout.StackPane
import org.junit.Assert.*
import org.junit.Test
import org.testfx.api.FxAssert.verifyThat
import org.testfx.assertions.api.Assertions.assertThat
import org.testfx.matcher.base.NodeMatchers.isNotNull
import org.testfx.matcher.control.LabeledMatchers.hasText
import de.thm.move.controllers.drawing.GUITesting.GuiTest
import javafx.geometry.Point2D

class twoOverlappingTest extends GuiTest:
  @Test def clickOnPolygon(): Unit =
    ResizablePolygon.clearPolygons()
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()


    val draggingPoint =  new Point2D(700, 60)
    val selectionPoint =  new Point2D(500, 40)



    //first polygon
    val firstPointP1 =  new Point2D(200, 50)
    val secondPointP1 =  new Point2D(400, 50)
    val thirdPointP1 =  new Point2D(400, 100)
    val fourthPointP1 =  new Point2D(200, 100)

    //second polygon
    val firstPointP2 =  new Point2D(300, 30)
    val secondPointP2 =  new Point2D(600, 30)
    val thirdPointP2 =  new Point2D(600, 70)
    val fourthPointP2 =  new Point2D(300, 70)





    clickOn("#polygon_btn")
    Seq(firstPointP1, secondPointP1, thirdPointP1, fourthPointP1, firstPointP1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)

    assertTrue("first zone has been registered", ResizablePolygon.polygons.nonEmpty)

    //draw second polygon that overlaps with the first one.
    Seq(firstPointP2, secondPointP2, thirdPointP2, fourthPointP2, firstPointP2).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    clickOn("#line_pointer")
    sleep(600)

    Seq(selectionPoint).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      val localDraggingPoint = gridToLocal(gridBoundsInLocal, draggingPoint)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      drag()
      moveTo(grid.localToScreen(localDraggingPoint).getX, grid.localToScreen(localDraggingPoint).getY)
      release(MouseButton.PRIMARY)
    }
    sleep(1000)

    assertEquals("second zone has been registered, even after shrinking is done because they overlap", ResizablePolygon.polygons.length, 2)

    drop()




class threeOverlappingTest extends GuiTest:
  @Test def clickOnPolygon(): Unit =
    ResizablePolygon.clearPolygons()
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    val draggingPoint = new Point2D(700, 60)
    val selectionPoint = new Point2D(500, 40)

    val secondSelectionPoint = new Point2D(350, 60)
    val secondDraggingPoint = new Point2D(200, 300)


    //first polygon
    val firstPointP1 = new Point2D(200, 50)
    val secondPointP1 = new Point2D(400, 50)
    val thirdPointP1 = new Point2D(400, 100)
    val fourthPointP1 = new Point2D(200, 100)

    //second polygon
    val firstPointP2 = new Point2D(300, 30)
    val secondPointP2 = new Point2D(450, 30)
    val thirdPointP2 = new Point2D(450, 70)
    val fourthPointP2 = new Point2D(300, 70)

    //third polygon
    val firstPointP3 = new Point2D(350, 20)
    val secondPointP3 = new Point2D(600, 20)
    val thirdPointP3 = new Point2D(600, 120)
    val fourthPointP3 = new Point2D(350, 120)


    //draw the first polygon.
    clickOn("#polygon_btn")
    Seq(firstPointP1, secondPointP1, thirdPointP1, fourthPointP1, firstPointP1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)
    assertEquals("zone has been registered", ResizablePolygon.polygons.length, 1)

    //draw the second polygon that, which overlaps with the first one.
    Seq(firstPointP2, secondPointP2, thirdPointP2, fourthPointP2, firstPointP2).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)
    assertEquals("second zone has been registered, even after shrinking is done because they overlap", ResizablePolygon.polygons.length, 2)

    //draw the third polygon, which overlaps with the two previous ones.
    Seq(firstPointP3, secondPointP3, thirdPointP3, fourthPointP3, firstPointP3).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    clickOn("#line_pointer")
    sleep(600)
    assertEquals("third zone has been registered and the two previous ones are still registered, even when all three of them overlap", ResizablePolygon.polygons.length, 3)


    //show that after drawing all polygons, their size has actually shrunk.
    clickOn("#line_pointer")

    sleep(600)


    Seq(selectionPoint).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      val localDraggingPoint = gridToLocal(gridBoundsInLocal, draggingPoint)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      drag()
      moveTo(grid.localToScreen(localDraggingPoint).getX, grid.localToScreen(localDraggingPoint).getY)
      release(MouseButton.PRIMARY)
    }
    sleep(1000)
    Seq(secondSelectionPoint).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      val localDraggingPoint = gridToLocal(gridBoundsInLocal, secondDraggingPoint)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      drag()
      moveTo(grid.localToScreen(localDraggingPoint).getX, grid.localToScreen(localDraggingPoint).getY)
      release(MouseButton.PRIMARY)
    }
    sleep(1000)

    assertEquals("three zones still exist after dragging them", ResizablePolygon.polygons.length, 3)




//test case to check that a zone that has internal intersecting edges does not get drawn.
class internalIntersectingEdges extends GuiTest:
  @Test def clickOnPolygon(): Unit =
    ResizablePolygon.clearPolygons()

    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()

    //polygon that has internal intersecting edges.
    val firstPointP1 = new Point2D(200, 50)
    val secondPointP1 = new Point2D(400, 50)
    val thirdPointP1 = new Point2D(400, 100)
    val fourthPointP1 = new Point2D(200, 100)

    clickOn("#polygon_btn")
    //try to draw the polygon
    Seq(firstPointP1, secondPointP1, fourthPointP1, thirdPointP1, firstPointP1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)

    assertTrue("zone has not been registered because it has internal intersecting edges", ResizablePolygon.polygons.isEmpty)

    drop()


//test case to check if a zone that is fully contained by another zone is deleted.
class completeOverlap extends GuiTest:
  @Test def clickOnPolygon(): Unit =
    ResizablePolygon.clearPolygons()
    val (grid, gridBoundsInLocal) = setupEnvironmentAndGrid()



    //first zone
    val firstPointP1 =  new Point2D(200, 50)
    val secondPointP1 =  new Point2D(400, 50)
    val thirdPointP1 =  new Point2D(400, 100)
    val fourthPointP1 =  new Point2D(200, 100)

    //second zone
    val firstPointP2 =  new Point2D(100, 30)
    val secondPointP2 =  new Point2D(600, 30)
    val thirdPointP2 =  new Point2D(600, 120)
    val fourthPointP2 =  new Point2D(100, 120)





    clickOn("#polygon_btn")
    Seq(firstPointP1, secondPointP1, thirdPointP1, fourthPointP1, firstPointP1).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    sleep(600)

    assertTrue("first zone has been registered", ResizablePolygon.polygons.nonEmpty)

    //draw second polygon that overlaps with the first one.
    Seq(firstPointP2, secondPointP2, thirdPointP2, fourthPointP2, firstPointP2).foreach { point =>
      val localPoint = gridToLocal(gridBoundsInLocal, point)
      moveTo(grid.localToScreen(localPoint).getX, grid.localToScreen(localPoint).getY)
      clickOn(MouseButton.PRIMARY)
    }
    clickOn("#line_pointer")
    sleep(600)


    //zone should be deleted since it is completely inside the new zone.
    assertEquals("second zone has been registered, even after shrinking is done because they overlap", ResizablePolygon.polygons.length, 1)

    drop()
