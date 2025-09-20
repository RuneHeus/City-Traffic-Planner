package de.thm.move.Roads

import de.thm.move.MoveSpec
import de.thm.move.views.shapes.ResizableLine
import de.thm.move.views.anchors.SharedAnchor

class RoadWithPredefinedAnchorsTest extends MoveSpec {

  "A ResizableLine" should "return predefined anchors when they are set" in {
    // Create a ResizableLine and predefined anchors
    val line = new ResizableLine((0, 0), (10, 10), 2)
    val predefinedAnchors = List(
      new SharedAnchor(5, 5),  // Predefined anchor at (5, 5)
      new SharedAnchor(10, 10) // Predefined anchor at (10, 10)
    )

    // Set the predefined anchors and retrieve them using getAnchors
    line.setPredefinedAnchors(predefinedAnchors)
    val anchors = line.getAnchors

    // Verify that the anchors returned are the predefined ones
    anchors shouldEqual predefinedAnchors
    anchors.size shouldEqual 2
    anchors.head.getCenterX shouldEqual 5.0
    anchors.head.getCenterY shouldEqual 5.0
    anchors.last.getCenterX shouldEqual 10.0
    anchors.last.getCenterY shouldEqual 10.0
  }

  it should "generate anchors if no predefined anchors are set" in {
    // Create a ResizableLine without predefined anchors
    val line = new ResizableLine((0, 0), (10, 10), 2)

    // Retrieve anchors using getAnchors
    val anchors = line.getAnchors

    // Verify that the generated anchors match the line's endpoints
    anchors.size shouldEqual 2
    anchors.head.getCenterX shouldEqual 0.0
    anchors.head.getCenterY shouldEqual 0.0
    anchors.last.getCenterX shouldEqual 10.0
    anchors.last.getCenterY shouldEqual 10.0
  }
}