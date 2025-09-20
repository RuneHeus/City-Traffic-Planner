package de.thm.move.Roads

import de.thm.move.Global.CurrentSelectedShape
import de.thm.move.MoveSpec
import de.thm.move.controllers.drawing.LineStrategy
import de.thm.move.controllers.{DrawPanelCtrl, RoadToolbarCtrl}
import de.thm.move.models.SelectedShape
import de.thm.move.views.panes.DrawPanel
import de.thm.move.views.shapes.ResizableLine
import javafx.scene.input.InputEvent
import org.mockito.Mockito._

import scala.collection.mutable
import org.mockito.ArgumentMatchers.any

class RoadTypeTest extends MoveSpec:
  // Shared state to track the RoadManager state
  var sharedRoads: mutable.Map[String, Road] = mutable.Map()
  var sharedNodes: mutable.Map[String, RoadNode] = mutable.Map()

  override def beforeEach(): Unit = {
    // Restore the shared state into the RoadManager
    RoadManager.roads.clear()
    RoadManager.nodes.clear()

    RoadManager.roads ++= sharedRoads
    RoadManager.nodes ++= sharedNodes
  }

  override def afterEach(): Unit = {
    // Save the current RoadManager state back to the shared state
    sharedRoads = RoadManager.roads.clone()
    sharedNodes = RoadManager.nodes.clone()
  }

  // Initialization: initialize the draw panel and 3 sample lines + their associated roads, each representing one of the three unique road types
  val drawPanel = new DrawPanel()
  val dummyHandler = { (ev: InputEvent) => () }
  val drawPanelCtrl = new DrawPanelCtrl(drawPanel, dummyHandler)
  val lineStrategy = new LineStrategy(drawPanelCtrl)

  val l1 = new ResizableLine((0, 0), (100, 100), 2)
  l1.setId("1")
  val r1 = Road(
    id = l1.getId,
    start = new RoadNode(l1.getId + "-start"),
    end = new RoadNode(l1.getId + "-end"),
    roadType = RoadType.Normal
  )
  RoadManager.addRoad(r1)

  // clone the RoadManager setup
  sharedRoads = RoadManager.roads.clone()
  sharedNodes = RoadManager.nodes.clone()

  val roadToolbarCtrl = new RoadToolbarCtrl()

  "getRoadTypeforShape" should "return the appropriate road types" in {
    RoadTypeManager.getRoadTypeForShape(SelectedShape.RoadNormal) shouldBe RoadType.Normal
    RoadTypeManager.getRoadTypeForShape(SelectedShape.RoadUnpaved) shouldBe RoadType.UnPaved
    RoadTypeManager.getRoadTypeForShape(SelectedShape.RoadDouble) shouldBe RoadType.Double
  }

  it should "return an exception when the road type is not recognized" in {
    assertThrows[NoSuchElementException] {
      RoadTypeManager.getRoadTypeForShape(null)
    }
  }

  "getShapeforRoadType" should "return the appropriate shape" in {
    RoadTypeManager.getShapeForRoadType(RoadType.Normal) shouldBe SelectedShape.RoadNormal
    RoadTypeManager.getShapeForRoadType(RoadType.UnPaved) shouldBe SelectedShape.RoadUnpaved
    RoadTypeManager.getShapeForRoadType(RoadType.Double) shouldBe SelectedShape.RoadDouble
  }

  it should "return an exception when the road type is not recognized" in {
    assertThrows[NoSuchElementException] {
      RoadTypeManager.getShapeForRoadType(null)
    }
  }

  "roadTypeProperties" should "return the appropriate values of the roads" in {
    val (shapeNormal, typeNormal, colorNormal, fillNormal, widthNormal) = RoadTypeManager.allRoadTypes.head
    val (shapeDouble, typeDouble, colorDouble, fillDouble, widthDouble) = RoadTypeManager.allRoadTypes.tail.head
    val (shapeUnpaved, typeUnpaved, colorUnpaved, fillUnpaved, widthUnpaved) = RoadTypeManager.allRoadTypes.tail.tail.head
    (colorNormal, fillNormal, widthNormal) shouldBe RoadTypeManager.roadTypeProperties(RoadType.Normal).head
    (colorDouble, fillDouble, widthDouble) shouldBe RoadTypeManager.roadTypeProperties(RoadType.Double).head
    (colorUnpaved, fillUnpaved, widthUnpaved) shouldBe RoadTypeManager.roadTypeProperties(RoadType.UnPaved).head
  }

  "handleRoadType" should "correctly change a road's type to Normal" in {
    CurrentSelectedShape = l1
    roadToolbarCtrl.handleRoadTypeChange(RoadType.Normal)

    r1.roadType shouldBe RoadType.Normal
    RoadTypeManager.getShapeForRoadType(r1.roadType) shouldBe SelectedShape.RoadNormal
    (l1.getFillColor, l1.getStrokeColor, l1.getStrokeWidth.toInt) shouldBe RoadTypeManager.roadTypeProperties(r1.roadType).head
  }

  it should "correctly change a road's type to Double" in {
    CurrentSelectedShape = l1
    roadToolbarCtrl.handleRoadTypeChange(RoadType.Double)

    r1.roadType shouldBe RoadType.Double
    RoadTypeManager.getShapeForRoadType(r1.roadType) shouldBe SelectedShape.RoadDouble
    (l1.getFillColor, l1.getStrokeColor, l1.getStrokeWidth.toInt) shouldBe RoadTypeManager.roadTypeProperties(r1.roadType).head
  }

  it should "correctly change a road's type to UnPaved" in {
    CurrentSelectedShape = l1
    roadToolbarCtrl.handleRoadTypeChange(RoadType.UnPaved)

    r1.roadType shouldBe RoadType.UnPaved
    RoadTypeManager.getShapeForRoadType(r1.roadType) shouldBe SelectedShape.RoadUnpaved
    (l1.getFillColor, l1.getStrokeColor, l1.getStrokeWidth.toInt) shouldBe RoadTypeManager.roadTypeProperties(r1.roadType).head
  }

  it should "work correctly when done multiple times in a row" in {
    CurrentSelectedShape = l1
    roadToolbarCtrl.handleRoadTypeChange(RoadType.Double)
    r1.roadType shouldBe RoadType.Double

    roadToolbarCtrl.handleRoadTypeChange(RoadType.UnPaved)
    r1.roadType shouldBe RoadType.UnPaved

    roadToolbarCtrl.handleRoadTypeChange(RoadType.Normal)
    r1.roadType shouldBe RoadType.Normal
  }

  it should "do nothing when called whenever CurrentSelectedShape is null" in {
    val oldRoadType = r1.roadType
    CurrentSelectedShape = null
    noException shouldBe thrownBy {
      roadToolbarCtrl.handleRoadTypeChange(RoadType.UnPaved)
    }
    r1.roadType shouldBe oldRoadType
  }
