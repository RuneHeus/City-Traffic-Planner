package de.thm.move.Roads

import de.thm.move.Global.CurrentSelectedShape
import de.thm.move.MoveSpec
import de.thm.move.Roads.OneWayLabel.{Back, Front, None}
import de.thm.move.controllers.RoadToolbarCtrl
import de.thm.move.controllers.drawing.LineStrategy
import de.thm.move.views.shapes.ResizableLine
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.Outcome
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock

class OneWayRoadsTest extends MoveSpec {
  private var l1: ResizableLine = _
  private val roadToolbarCtrl = new RoadToolbarCtrl()

  def initializeMock(): RoadToolbarCtrl = // mocks handleRoadTypeChange for integration tests that require road types
    val RoadToolbarCtrlMock = mock[RoadToolbarCtrl]
    when(RoadToolbarCtrlMock.handleRoadTypeChange(any[RoadType])).thenAnswer { invocation => // mock the behaviour of handleRoadTypeChange
      val newRoadType = invocation.getArgument[RoadType](0)
      CurrentSelectedShape match {
        case shape: ResizableLine =>
          RoadManager.getRoadProperties(shape.getId).roadType = newRoadType
          if RoadManager.getRoadProperties(shape.getId).one_way == OneWayLabel.None
          then shape.setStrokeWidth(RoadTypeManager.roadTypeProperties(newRoadType).head._3)
        case _ =>
      }
    }
    when(RoadToolbarCtrlMock.ToggleOneWay(true)).thenCallRealMethod() // ToggleOneWay is simply called as it is normally)
    return RoadToolbarCtrlMock

  override def withFixture(test: NoArgTest): Outcome = {
    l1 = new ResizableLine((0, 0), (100, 100), RoadTypeManager.roadTypeProperties(RoadType.Normal).head._3)
    l1.setId("1")
    val r1 = Road(
      id = l1.getId,
      start = new RoadNode("string"),
      end = new RoadNode("string2"),
      roadType = RoadType.Normal
    )
    RoadManager.addRoad(r1)
    CurrentSelectedShape = l1

    try {
      test()
    } finally {
      ResizableLine.allLines.clear()
      RoadManager.roads.clear()
      RoadManager.nodes.clear()
      CurrentSelectedShape = null
    }
  }

  // Tests start here
  "Creating a new road" should "ensure it is initially not a one-way road" in {
    RoadManager.getRoadProperties(l1.getId).one_way shouldBe OneWayLabel.None
  }

  "ToggleOneWay" should "change the road to a Front one-way road and change its stroke width to oneWayThickness after one call" in {
    roadToolbarCtrl.ToggleOneWay(true)
    RoadManager.getRoadProperties(l1.getId).one_way shouldBe OneWayLabel.Front
    l1.getStrokeWidth shouldBe RoadManager.oneWayThickness
  }

  it should "change the road to a Back one-way road and change its stroke width to oneWayThickness after two calls" in {
    roadToolbarCtrl.ToggleOneWay(true)
    roadToolbarCtrl.ToggleOneWay(true)
    RoadManager.getRoadProperties(l1.getId).one_way shouldBe Back
    l1.getStrokeWidth shouldBe RoadManager.oneWayThickness
  }

  it should "change the road to a None one-way road and keep its original stroke width after three calls" in {
    val originalWidth = l1.getStrokeWidth
    roadToolbarCtrl.ToggleOneWay(true)
    roadToolbarCtrl.ToggleOneWay(true)
    roadToolbarCtrl.ToggleOneWay(true)
    RoadManager.getRoadProperties(l1.getId).one_way shouldBe None
    l1.getStrokeWidth shouldBe originalWidth
  }

  it should "not do anything when called whenever CurrentSelectedShape is null" in {
    val original = RoadManager.getRoadProperties(l1.getId).one_way
    CurrentSelectedShape = null
    roadToolbarCtrl.ToggleOneWay(true)
    RoadManager.getRoadProperties(l1.getId).one_way shouldBe original
  }

  it should "allow the road type of a one way road to be changed" in {
    val rtbMock = initializeMock()

    rtbMock.ToggleOneWay(true)
    rtbMock.handleRoadTypeChange(RoadType.Double)
    assert(RoadManager.getRoadProperties(l1.getId).roadType == RoadType.Double,
      s"Road Type was not changed accordingly of the one way road, real: ${RoadManager.getRoadProperties(l1.getId).roadType}, expected: ${RoadType.Double}")
    assert(RoadManager.getRoadProperties(l1.getId).one_way == OneWayLabel.Front,
      s"Road's one way label has been changed by changing the road type, real: ${RoadManager.getRoadProperties(l1.getId).one_way}, expected: ${OneWayLabel.Front}")
    assert(l1.getStrokeWidth == RoadManager.oneWayThickness,
      s"Road's width was changed after changing the road type, real: ${l1.getStrokeWidth}, expected: ${RoadManager.oneWayThickness}.")
  }

  it should "change the one way label of a changed road type" in {
    val rtbMock = initializeMock()

    rtbMock.handleRoadTypeChange(RoadType.Double)
    rtbMock.ToggleOneWay(true)
    assert(RoadManager.getRoadProperties(l1.getId).roadType == RoadType.Double,
      s"Road Type was not correct after changing the road type and then changing one way label, real: ${RoadManager.getRoadProperties(l1.getId).roadType}, expected: ${RoadType.Double}")
    assert(RoadManager.getRoadProperties(l1.getId).one_way == OneWayLabel.Front,
      s"Road's one way label has not been changed correctly by changing the road type and then changing one way label, real: ${RoadManager.getRoadProperties(l1.getId).one_way}, expected: ${OneWayLabel.Back}")
    assert(l1.getStrokeWidth == RoadManager.oneWayThickness,
      s"Road's width was not correct after changing the road type and then changing one way label, real: ${l1.getStrokeWidth}, expected: ${RoadManager.oneWayThickness}.")
  }
}

