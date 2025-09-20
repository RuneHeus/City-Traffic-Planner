package de.thm.move.Roads

import de.thm.move.MoveSpec
import de.thm.move.controllers.drawing.TrafficLightStrategy
import de.thm.move.views.anchors.SharedAnchor
import de.thm.move.views.panes.DrawPanel
import de.thm.move.views.shapes.ResizableLine
import javafx.collections.FXCollections
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.*

import java.util.UUID

class TrafficLightTest extends MoveSpec with MockitoSugar {

  private def setupMockDrawPanel(): (DrawPanel, javafx.collections.ObservableList[Any]) = {
    val mockDrawPanel = mock[DrawPanel]
    val mockChildren = FXCollections.observableArrayList[Any]()
    when(mockDrawPanel.getChildren).thenReturn(mockChildren)
    (mockDrawPanel, mockChildren)
  }

  private def createAnchorWithConnections(connectionCount: Int): SharedAnchor = {
    val anchor = new SharedAnchor(100, 100)
    anchor.setId("Anchor"+ UUID.randomUUID().toString)
    (1 to connectionCount).foreach(_ => anchor.addConnection(mock[ResizableLine], 0))
    anchor
  }

  "TrafficLightStrategy" should "add a traffic light to a valid anchor with at least 3 connections" in {
    val (mockDrawPanel, mockChildren) = setupMockDrawPanel()
    val trafficLightStrategy = new TrafficLightStrategy(mockDrawPanel)
    val anchor = createAnchorWithConnections(3)

    trafficLightStrategy.addTrafficLight(anchor)

    anchor.getTrafficLight shouldBe defined
    val trafficLight = anchor.getTrafficLight.get
    trafficLight.getText shouldEqual "\uf0eb"
    mockChildren.contains(trafficLight) shouldBe true
  }

  it should "not add a traffic light if the anchor has less than 3 connections" in {
    val (mockDrawPanel, mockChildren) = setupMockDrawPanel()
    val trafficLightStrategy = new TrafficLightStrategy(mockDrawPanel)
    val anchor = createAnchorWithConnections(2)

    val exception = intercept[IllegalArgumentException] {
      trafficLightStrategy.addTrafficLight(anchor)
    }

    exception.getMessage should include("does not meet the conditions for a traffic light")

    anchor.getTrafficLight shouldBe empty
    mockChildren.isEmpty shouldBe true
  }

  it should "remove the traffic light when requested" in {
    val (mockDrawPanel, mockChildren) = setupMockDrawPanel()
    val trafficLightStrategy = new TrafficLightStrategy(mockDrawPanel)
    val anchor = createAnchorWithConnections(3)

    trafficLightStrategy.addTrafficLight(anchor)
    trafficLightStrategy.removeTrafficLight(anchor)

    anchor.getTrafficLight shouldBe empty
    mockChildren.isEmpty shouldBe true
  }

  it should "remove the traffic light when the number of connections drops below 3" in {
    val (mockDrawPanel, mockChildren) = setupMockDrawPanel()
    val trafficLightStrategy = new TrafficLightStrategy(mockDrawPanel)
    val anchor = createAnchorWithConnections(3)

    trafficLightStrategy.addTrafficLight(anchor)
    anchor.getTrafficLight shouldBe defined
    mockChildren.size() shouldBe 1

    val trafficLightBefore = anchor.getTrafficLight.get
    mockChildren.contains(trafficLightBefore) shouldBe true

    val lineToRemove = anchor.connectedLines.head._1
    anchor.removeConnection(lineToRemove)

    trafficLightStrategy.removeTrafficLight(anchor)

    anchor.getTrafficLight shouldBe empty
    mockChildren.contains(trafficLightBefore) shouldBe false
    mockChildren.isEmpty shouldBe true

    anchor.connectedLines.size shouldBe <(3)
  }

  it should "not allow adding a traffic light if one already exists" in {
    val (mockDrawPanel, mockChildren) = setupMockDrawPanel()
    val trafficLightStrategy = new TrafficLightStrategy(mockDrawPanel)
    val anchor = createAnchorWithConnections(3)

    trafficLightStrategy.addTrafficLight(anchor)
    anchor.getTrafficLight shouldBe defined
    mockChildren.size() shouldBe 1

    val exception = intercept[IllegalArgumentException] {
      trafficLightStrategy.addTrafficLight(anchor)
    }

    exception.getMessage should include("already has a traffic light")

    anchor.getTrafficLight shouldBe defined
    mockChildren.size() shouldBe 1
  }

  it should "not remove a listener if the anchor has no listeners" in {
    val (mockDrawPanel, _) = setupMockDrawPanel()
    val trafficLightStrategy = new TrafficLightStrategy(mockDrawPanel)
    val anchor = createAnchorWithConnections(3)

    trafficLightStrategy.removeTrafficLight(anchor)

    anchor.getTrafficLight shouldBe empty
  }

  it should "update the icon position when the anchor moves" in {
    val (mockDrawPanel, _) = setupMockDrawPanel()
    val trafficLightStrategy = new TrafficLightStrategy(mockDrawPanel)
    val anchor = createAnchorWithConnections(3)
    trafficLightStrategy.addTrafficLight(anchor)
    val trafficLight = anchor.getTrafficLight.get

    anchor.setCenterX(200)
    anchor.setCenterX(150)

    trafficLight.getLayoutX shouldEqual (anchor.getBoundsInLocal.getMinX + 10)
    trafficLight.getLayoutY shouldEqual (anchor.getBoundsInLocal.getMinY + 10)
  }

  it should "invoke listeners on connection change" in {
    val (mockDrawPanel, _) = setupMockDrawPanel()
    val trafficLightStrategy = new TrafficLightStrategy(mockDrawPanel)
    val anchor = createAnchorWithConnections(3)
    trafficLightStrategy.addTrafficLight(anchor)

    val mockListener = mock[() => Unit]
    anchor.addConnectionChangeListener(mockListener)

    anchor.addConnection(mock[ResizableLine], 0)

    verify(mockListener, times(1)).apply()
  }
}