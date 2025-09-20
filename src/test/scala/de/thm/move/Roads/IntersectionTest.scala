package de.thm.move.controllers.drawing.GUITesting

import de.thm.move.Global.CurrentSelectedShape
import de.thm.move.MoveSpec
import de.thm.move.Roads.*
import de.thm.move.controllers.drawing.LineStrategy
import de.thm.move.controllers.{DrawPanelCtrl, RoadToolbarCtrl}
import de.thm.move.views.panes.DrawPanel
import de.thm.move.views.shapes.ResizableLine
import javafx.scene.input.InputEvent
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock

class IntersectionTest extends MoveSpec():
  // initialization
  val drawPanel = new DrawPanel()
  val dummyHandler = { (ev: InputEvent) => () }
  val drawPanelCtrl = new DrawPanelCtrl(drawPanel, dummyHandler)
  val lineStrategy = new LineStrategy(drawPanelCtrl)

  var l1: ResizableLine = _
  var l2: ResizableLine = _
  // helper functions
  def addTestLine(line: ResizableLine, id: String): Unit = {
    line.setId(id)
    ResizableLine.addLine(line)
    val startNode = RoadManager.getOrCreateNode(line.getId + "-start")
    val endNode = RoadManager.getOrCreateNode(line.getId + "-end")
    val road = Road(
      id = line.getId,
      start = startNode,
      end = endNode,
      roadType = RoadType.Normal
    )
    RoadManager.addRoad(road)
  }

  def changeCoordinates(line: ResizableLine, x1: Int, x2: Int, y1: Int, y2: Int): Unit = {
    line.setStartX(x1)
    line.setStartY(y1)
    line.setEndX(x2)
    line.setEndY(y2)
  }

  def initializeMock(): RoadToolbarCtrl = // mock behaviour for certain tests that test integration between intersections <-> one-way roads, and intersections <-> road types
    val RoadToolbarCtrlMock = mock[RoadToolbarCtrl]
    when(RoadToolbarCtrlMock.handleRoadTypeChange(any[RoadType])).thenAnswer { invocation => // mock the behaviour of handleRoadTypeChange (only the relevant part)
      val newRoadType = invocation.getArgument[RoadType](0)
      CurrentSelectedShape match {
        case shape: ResizableLine =>
          RoadManager.getRoadProperties(shape.getId).roadType = newRoadType
        case _ =>
      }
    }
    when(RoadToolbarCtrlMock.ToggleOneWay(true)).thenAnswer { _ =>  // mock the behaviour of ToggleOneWay (only the relevant part)
      CurrentSelectedShape match {
        case road: ResizableLine =>
          RoadManager.getRoadProperties(road.getId).one_way match {
            case OneWayLabel.None =>
              RoadManager.getRoadProperties(road.getId).one_way = OneWayLabel.Front
              road.setStrokeWidth(RoadManager.oneWayThickness)
            case _ =>
          }
      }
    }
    return RoadToolbarCtrlMock

  // test definitions to avoid code duplication
  def CheckIntersectionTest(expected: Boolean): Unit = { // test def that tells you if checkintersection detects an intersection or not as expected.
    if expected then
      lineStrategy.CheckIntersection(l1).isRight shouldBe true
      lineStrategy.CheckIntersection(l2).isRight shouldBe true
    else
      lineStrategy.CheckIntersection(l1).isLeft shouldBe true
      lineStrategy.CheckIntersection(l2).isLeft shouldBe true
  }

  def splitTest(splitValue: Int): (ResizableLine, ResizableLine) = { // a specific test def used in 3 tests that splits a line with the input point and then checks if it is correctly split
    val oldSize = ResizableLine.allLines.size
    val (s1: ResizableLine, s2: ResizableLine) = lineStrategy.SplitRoad(l1, splitValue, splitValue, false)
    s1.getStartX shouldBe l1.getStartX
    s1.getStartY shouldBe l1.getStartY
    s1.getEndX shouldBe splitValue
    s1.getEndY shouldBe splitValue
    s2.getStartX shouldBe splitValue
    s2.getStartY shouldBe splitValue
    s2.getEndX shouldBe l1.getEndX
    s2.getEndY shouldBe l1.getEndY
    ResizableLine.allLines.size shouldBe oldSize + 1
    return (s1, s2)
  }

  def twointersectionsTest(inputLine: ResizableLine): Unit = { // a specific test def for checking if two lines are split properly when called on an input line, is used in two tests
    val oldSize = ResizableLine.allLines.size
    lineStrategy.handleIntersection(inputLine, false)
    val IntersectionPoint = 50
    val l1s1 = ResizableLine.allLines.find(line => line.getStartX == 0 && line.getStartY == 0 && line.getEndX == IntersectionPoint && line.getEndY == IntersectionPoint)
    val l1s2 = ResizableLine.allLines.find(line => line.getEndX == 100 && line.getEndY == 100 && line.getStartX == IntersectionPoint && line.getStartY == IntersectionPoint)
    val l2s1 = ResizableLine.allLines.find(line => line.getStartX == 0 && line.getStartY == 100 && line.getEndX == IntersectionPoint && line.getEndY == IntersectionPoint)
    val l2s2 = ResizableLine.allLines.find(line => line.getEndX == 100 && line.getEndY == 0 && line.getStartX == IntersectionPoint && line.getStartY == IntersectionPoint)
    l1s1.getOrElse(throw new AssertionError(s"Expected a line with start (0,0) and end ($IntersectionPoint, $IntersectionPoint) after the split of l1, but it was not found."))
    l1s2.getOrElse(throw new AssertionError(s"Expected a line with start ($IntersectionPoint, $IntersectionPoint) and end (100, 100) after the split of l1, but it was not found."))
    l2s1.getOrElse(throw new AssertionError(s"Expected a line with start (0, 100) and end ($IntersectionPoint, $IntersectionPoint) after the split of l2, but it was not found."))
    l2s2.getOrElse(throw new AssertionError(s"Expected a line with start ($IntersectionPoint, $IntersectionPoint) and end (100, 0) after the split of l2, but it was not found."))
    ResizableLine.allLines.size shouldBe oldSize + 2
  }

    override def withFixture(test: NoArgTest) = {
      l1 = new ResizableLine((0, 0), (100, 100), 4)
      l2 = new ResizableLine((0, 100), (100, 0), 4)
      addTestLine(l1, "1")
      addTestLine(l2, "2")

      try {
        test()
      } finally {
        ResizableLine.allLines.clear()
        RoadManager.roads.clear()
        RoadManager.nodes.clear()
      }
    }

  "CheckIntersection" should "correctly identify two intersecting lines" in {
    CheckIntersectionTest(true)
  }

  it should "correctly identify two intersecting lines with start and end values reversed" in {
    changeCoordinates(l2, 100, 0, 0, 100)
    CheckIntersectionTest(true)
  }

  it should "correctly identify two non-intersecting lines" in {
    changeCoordinates(l2, 300, 400, 500, 600)
    CheckIntersectionTest(false)
  }

  it should "indicate two parallel lines as non-intersecting" in {
    changeCoordinates(l2, 75, 75, 150, 150)
    CheckIntersectionTest(false)
  }

  it should "correctly identify two non-intersecting lines that are 1 pixel away from intersecting" in {
    changeCoordinates(l2, 50, 49, 200, 50)
    CheckIntersectionTest(false)
  }

  it should "correctly identify two intersecting lines that are 1 pixel away from not intersecting" in {
    changeCoordinates(l2, 50, 51, 200, 50)
    CheckIntersectionTest(true)
  }

  "SplitRoad" should "correctly split a line in 2 when split in the middle" in {
    splitTest(50)
  }

  it should "correctly split a line in 2 when split near the start point" in {
    splitTest(1)
  }
  it should "correctly split a line in 2 when split near the end point" in {
    splitTest(99)
  }

  it should "correctly split a line with a different Road Type in 2, and preserve its unique properties in both splitted lines" in {
    val RoadToolbarCtrlMock = initializeMock()
    CurrentSelectedShape = l1
    RoadToolbarCtrlMock.handleRoadTypeChange(RoadType.Double)
    val l1r = RoadManager.getRoad(l1.getId).get.roadType
    val (s1, s2) = splitTest(50)
    RoadManager.getRoad(s1.getId).get.roadType shouldBe l1r
    RoadManager.getRoad(s2.getId).get.roadType shouldBe l1r
  }

  it should "correctly split a line with a different One Way label in 2, and preserve its unique properties in both splitted lines" in {
    val RoadToolbarCtrlMock = initializeMock()
    CurrentSelectedShape = l1
    RoadToolbarCtrlMock.OneWayAction()
    val l1o = RoadManager.getRoad(l1.getId).get.one_way
    val l1w = l1.getStrokeWidth
    val (s1, s2) = splitTest(50)
    RoadManager.getRoad(s1.getId).get.one_way shouldBe l1o
    RoadManager.getRoad(s2.getId).get.one_way shouldBe l1o
    s1.getStrokeWidth shouldBe l1w
    s2.getStrokeWidth shouldBe l1w
  }

  it should "throw an exception when the coordinates are out of bounds" in {
    intercept[IllegalArgumentException] {
      lineStrategy.SplitRoad(l1, 150, 150, false)
    }
  }

  it should "throw an exception when the coordinates are out of bounds by just one pixel" in {
    intercept[IllegalArgumentException] {
      lineStrategy.SplitRoad(l1, 101, 100, false)
    }
  }

  it should "throw an exception when passed a null road" in {
    intercept[IllegalArgumentException] {
      lineStrategy.SplitRoad(null, 50, 50, false)
    }
  }

  it should "throw an exception when called with negative coordinates" in {
    changeCoordinates(l1, -50, -50, 50, 50)
    intercept[IllegalArgumentException] {
      lineStrategy.SplitRoad(l1, -25, -25, false)
    }
  }

  "handleIntersection" should "split both intersecting lines when called on line 1, creating 2 new lines on the board with the correct coordinates" in {
    twointersectionsTest(l1)
  }

  it should "split both intersecting lines when called on line 2, creating 2 new lines on the board with the correct coordinates" in { // ensure it works when either line is given as input
    twointersectionsTest(l2)
  }

  it should "split a line that intersects with 2 other lines, creating 4 new lines on the board with the correct coordinates" in {
    val oldSize = ResizableLine.allLines.size
    val l3 = new ResizableLine((0, 50), (50, 0), 4)
    addTestLine(l3, "3")
    lineStrategy.handleIntersection(l3, false)
    ResizableLine.allLines.size shouldBe oldSize + 5

    val IntersectionPoint1 = 25
    val IntersectionPoint2 = 50
    val l1s1 = ResizableLine.allLines.find(line => line.getStartX == 0 && line.getStartY == 0 && line.getEndX == IntersectionPoint1 && line.getEndY == IntersectionPoint1)
    val l1s2 =
      ResizableLine.allLines.find(line => line.getEndX == IntersectionPoint2 && line.getEndY == IntersectionPoint2 && line.getStartX == IntersectionPoint1 && line.getStartY == IntersectionPoint1)
    val l1s3 = ResizableLine.allLines.find(line => line.getEndX == 100 && line.getEndY == 100 && line.getStartX == IntersectionPoint2 && line.getStartY == IntersectionPoint2)

    val l2s1 = ResizableLine.allLines.find(line => line.getStartX == 0 && line.getStartY == 100 && line.getEndX == IntersectionPoint2 && line.getEndY == IntersectionPoint2)
    val l2s2 = ResizableLine.allLines.find(line => line.getEndX == 100 && line.getEndY == 0 && line.getStartX == IntersectionPoint2 && line.getStartY == IntersectionPoint2)

    val l3s1 = ResizableLine.allLines.find(line => line.getStartX == 0 && line.getStartY == 50 && line.getEndX == IntersectionPoint1 && line.getEndY == IntersectionPoint1)
    val l3s2 = ResizableLine.allLines.find(line => line.getEndX == 50 && line.getEndY == 0 && line.getStartX == IntersectionPoint1 && line.getStartY == IntersectionPoint1)

    l1s1.getOrElse(throw new AssertionError(s"Expected a line with start (0, 0) and end ($IntersectionPoint1, $IntersectionPoint1) after the split of l1, but it was not found."))
    l1s2.getOrElse(
      throw new AssertionError(s"Expected a line with start ($IntersectionPoint1, $IntersectionPoint1) and end ($IntersectionPoint2, $IntersectionPoint2) after the split of l1, but it was not found.")
    )
    l1s3.getOrElse(throw new AssertionError(s"Expected a line with start ($IntersectionPoint2, $IntersectionPoint2) and end (100, 100) after the split of l2, but it was not found."))
    l2s1.getOrElse(throw new AssertionError(s"Expected a line with start (700, 900) and end ($IntersectionPoint1, $IntersectionPoint1) after the split of l2, but it was not found."))
    l2s2.getOrElse(throw new AssertionError(s"Expected a line with start ($IntersectionPoint1, $IntersectionPoint1) and end (900, 700) after the split of l2, but it was not found."))
    l3s1.getOrElse(throw new AssertionError(s"Expected a line with start (800, 900) and end ($IntersectionPoint2, $IntersectionPoint2) after the split of l3, but it was not found."))
    l3s2.getOrElse(throw new AssertionError(s"Expected a line with start ($IntersectionPoint2, $IntersectionPoint2) and end (900, 800) after the split of l3, but it was not found."))
  }

  it should "split a line that intersects with 3 other lines, creating 6 new lines on the board" in {
    val oldSize = ResizableLine.allLines.size // skip checking the coordinates, it is already checked in the last test. this test ensures that the function can handle > 1 iteration of the loop
    val l3 = new ResizableLine((0, 50), (50, 0), 4)
    val l4 = new ResizableLine((0, 75), (75, 0), 4)
    addTestLine(l3, "3")
    addTestLine(l4, "4")
    lineStrategy.handleIntersection(l1, false)
    ResizableLine.allLines.size shouldBe oldSize + 8
  }

  it should "not do anything when called on a non intersecting line" in {
    val oldSize = ResizableLine.allLines.size
    changeCoordinates(l1, 200, 200, 300, 300)
    lineStrategy.handleIntersection(l1, false)
    ResizableLine.allLines.size shouldBe oldSize
  }

  it should "not split a line again when called twice on an intersecting line" in {
    lineStrategy.handleIntersection(l1, false)
    val oldSize = ResizableLine.allLines.size
    lineStrategy.handleIntersection(l1, false)
    ResizableLine.allLines.size shouldBe oldSize
  }

  it should "not split 2 lines that are just 1 pixel away from creating an intersection" in {
    val oldSize = ResizableLine.allLines.size
    changeCoordinates(l2, 50, 49, 200, 50)
    lineStrategy.handleIntersection(l2, false)
    ResizableLine.allLines.size shouldBe oldSize
  }

  it should "split 2 lines that are just 1 pixel away from not creating an intersection" in {
    val oldSize = ResizableLine.allLines.size
    changeCoordinates(l2, 50, 51, 200, 50)
    lineStrategy.handleIntersection(l2, false)
    ResizableLine.allLines.size shouldBe oldSize+2
  }






