package de.thm.move.Anchor

import de.thm.move.MoveSpec
import de.thm.move.views.anchors.{Anchor, SharedAnchor}
import de.thm.move.views.shapes.ResizableLine
import javafx.scene.text.Text
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.*

class SharedAnchorTest extends MoveSpec {

  "SharedAnchor" should "initialize with the correct position" in {
    val anchor = new SharedAnchor(10.0, 20.0)
    val position = (anchor.getCenterX, anchor.getCenterY)
    position shouldBe (10.0, 20.0)
  }

  it should "give the correct position back in the layout" in {
    val mockAnchor = mock(classOf[SharedAnchor]) // Mock is needed because the method position depends on the Node to be added to a pane
    when(mockAnchor.getLayoutX).thenReturn(5.0)
    when(mockAnchor.getLayoutY).thenReturn(15.0)
    when(mockAnchor.getPosition).thenCallRealMethod()

    val position = mockAnchor.getPosition
    position shouldBe(5.0, 15.0)
  }

  it should "allow adding and removing connections" in {
    val anchor = new SharedAnchor(0.0, 0.0)
    val line1 = new ResizableLine((0.0, 0.0), (100.0, 100.0), 2)
    val line2 = new ResizableLine((0.0, 0.0), (50.0, 50.0), 2)

    anchor.addConnection(line1, 0)
    anchor.addConnection(line2, 1)

    anchor.connectedLines should contain allOf ((line1, 0), (line2, 1))

    anchor.removeConnection(line1)
    anchor.connectedLines should not contain ((line1, 0))
    anchor.connectedLines should contain ((line2, 1))
  }

  it should "correctly track connection status" in {
    val anchor = new SharedAnchor(0.0, 0.0)
    val line = new ResizableLine((0.0, 0.0), (100.0, 100.0), 2)

    anchor.hasConnections shouldBe false
    anchor.addConnection(line, 0)
    anchor.hasConnections shouldBe true
    anchor.removeConnection(line)
    anchor.hasConnections shouldBe false
  }

  it should "allow setting and removing traffic lights" in {
    val anchor = new SharedAnchor(0.0, 0.0)
    val trafficLight = new Text("Traffic Light")

    anchor.setTrafficLight(trafficLight)
    anchor.getTrafficLight shouldBe Some(trafficLight)

    anchor.removeTrafficLight()
    anchor.getTrafficLight shouldBe None
  }

  it should "notify listeners on connection changes" in {
    val anchor = new SharedAnchor(0.0, 0.0)
    val line = new ResizableLine((0.0, 0.0), (100.0, 100.0), 2)

    var listenerCalled = false
    anchor.addConnectionChangeListener(() => listenerCalled = true)

    anchor.addConnection(line, 0)
    listenerCalled shouldBe true
    listenerCalled = false

    anchor.removeConnection(line)
    listenerCalled shouldBe true
  }

  it should "remove connection listeners properly" in {
    val anchor = new SharedAnchor(0.0, 0.0)

    var listenerCalled = false
    val listener = () => listenerCalled = true
    anchor.addConnectionChangeListener(listener)

    anchor.removeConnectionChangeListener(listener)

    // Simulate connection change
    anchor.addConnection(mock(classOf[ResizableLine]), 0)
    listenerCalled shouldBe false
  }

  it should "move connected lines when moved" in {
    val anchor = new SharedAnchor(0.0, 0.0)
    val line = new ResizableLine((0.0, 0.0), (100.0, 100.0), 2)

    anchor.addConnection(line, 0)
    anchor.moveWithSharedAnchor((10.0, 20.0))

    line.getStartX shouldBe 10.0
    line.getStartY shouldBe 20.0
  }

  it should "detect shared connections with another anchor" in {
    val anchor1 = new SharedAnchor(0.0, 0.0)
    val anchor2 = new SharedAnchor(10.0, 20.0)
    val line = new ResizableLine((0.0, 0.0), (100.0, 100.0), 2)

    anchor1.addConnection(line, 0)
    anchor2.addConnection(line, 1)

    anchor1.isConnectedToLine(anchor2) shouldBe true
    anchor2.isConnectedToLine(anchor1) shouldBe true
  }

  it should "add and trigger dynamic listeners" in {
    val anchor = new SharedAnchor(0.0, 0.0)

    var listenerTriggered = false
    anchor.addDynamicListener(() => listenerTriggered = true)

    // Simulate a layout change
    anchor.setLayoutX(10.0)
    anchor.setLayoutY(20.0)

    listenerTriggered shouldBe true
  }
}