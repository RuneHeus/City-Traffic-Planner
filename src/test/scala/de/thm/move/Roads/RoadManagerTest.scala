package de.thm.move.Roads

import de.thm.move.MoveSpec
import de.thm.move.Roads
import de.thm.move.Roads.RoadType.{Normal, UnPaved, Double}

class RoadManagerTest extends MoveSpec {

  // ============================================================
  // Tests for Adding Roads
  // ============================================================

  "A RoadManager" should "add a new road correctly if the Nodes arent in the road Manager yet" in {
    val startNode = new RoadNode("Node-1")
    val endNode = new RoadNode("Node-2")
    val road = new Road("Road-1", startNode, endNode, Normal)

    RoadManager.addRoad(road)
    val retrievedRoad = RoadManager.getRoad("Road-1")

    retrievedRoad.isDefined shouldEqual true
    retrievedRoad.get.id shouldEqual "Road-1"
    retrievedRoad.get.start shouldEqual startNode
    retrievedRoad.get.end shouldEqual endNode
    retrievedRoad.get.roadType shouldEqual Normal

    startNode.connectedRoads.contains("Road-1") shouldEqual true
    endNode.connectedRoads.contains("Road-1") shouldEqual true
  }

  it should "add a new road correctly if the Nodes are already in the road Manager by id" in {
    val startNode = RoadManager.getOrCreateNode("Node-1")
    val endNode = RoadManager.getOrCreateNode("Node-2")
    val road = new Road("Road-1", startNode, endNode, Normal)

    RoadManager.addRoad(road)
    val retrievedRoad = RoadManager.getRoad("Road-1")

    retrievedRoad.isDefined shouldEqual true
    retrievedRoad.get.id shouldEqual "Road-1"
    retrievedRoad.get.start shouldEqual startNode
    retrievedRoad.get.end shouldEqual endNode
    retrievedRoad.get.roadType shouldEqual Normal

    startNode.connectedRoads.contains("Road-1") shouldEqual true
    endNode.connectedRoads.contains("Road-1") shouldEqual true
  }

  it should "add a new road correctly if the Nodes are already in the road Manager by instance" in {
    val startNode = new RoadNode("Node-1")
    val endNode = new RoadNode("Node-2")
    RoadManager.getOrCreateNode(startNode)
    RoadManager.getOrCreateNode(endNode)
    val road = new Road("Road-1", startNode, endNode, Normal)

    RoadManager.addRoad(road)
    val retrievedRoad = RoadManager.getRoad("Road-1")

    retrievedRoad.isDefined shouldEqual true
    retrievedRoad.get.id shouldEqual "Road-1"
    retrievedRoad.get.start shouldEqual startNode
    retrievedRoad.get.end shouldEqual endNode
    retrievedRoad.get.roadType shouldEqual Normal

    startNode.connectedRoads.contains("Road-1") shouldEqual true
    endNode.connectedRoads.contains("Road-1") shouldEqual true
  }


  it should "handle adding a road with duplicate ID correctly" in {
    val startNode1 = RoadManager.getOrCreateNode("Node-A")
    val endNode1 = RoadManager.getOrCreateNode("Node-B")
    val road1 = new Road("Road-1", startNode1, endNode1, Normal)

    val startNode2 = RoadManager.getOrCreateNode("Node-C")
    val endNode2 = RoadManager.getOrCreateNode("Node-D")
    val updatedRoad = new Road("Road-1", startNode2, endNode2, UnPaved)

    RoadManager.addRoad(road1)
    RoadManager.addRoad(updatedRoad)

    val retrievedRoad = RoadManager.getRoad("Road-1")
    retrievedRoad.isDefined shouldEqual true
    retrievedRoad.get.start shouldEqual startNode2
    retrievedRoad.get.end shouldEqual endNode2
    retrievedRoad.get.roadType shouldEqual UnPaved

    startNode1.connectedRoads.contains("Road-1") shouldEqual false
    endNode1.connectedRoads.contains("Road-1") shouldEqual false
  }

  it should "add multiple roads correctly" in {
    val startNode1 = RoadManager.getOrCreateNode("Node-1")
    val endNode1 = RoadManager.getOrCreateNode("Node-2")
    val road1 = new Road("Road-1", startNode1, endNode1, Normal)

    val startNode2 = RoadManager.getOrCreateNode("Node-3")
    val endNode2 = RoadManager.getOrCreateNode("Node-4")
    val road2 = new Road("Road-2", startNode2, endNode2, UnPaved)

    RoadManager.addRoad(road1)
    RoadManager.addRoad(road2)

    RoadManager.roads.size shouldEqual 2
    RoadManager.nodes.size shouldEqual 4
  }

  it should "handle adding roads between shared nodes correctly" in {
    val sharedNode = RoadManager.getOrCreateNode("Node-Shared")
    val endNode1 = RoadManager.getOrCreateNode("Node-1")
    val endNode2 = RoadManager.getOrCreateNode("Node-2")
    val road1 = new Road("Road-Shared-1", sharedNode, endNode1, Normal)
    val road2 = new Road("Road-Shared-2", sharedNode, endNode2, UnPaved)

    RoadManager.addRoad(road1)
    RoadManager.addRoad(road2)

    sharedNode.connectedRoads.size shouldEqual 2
    sharedNode.connectedNodeIds should contain allOf("Node-1", "Node-2")
  }

  it should "not allow adding a road with the same start and end node" in {
    val node = RoadManager.getOrCreateNode("Node-Same")
    val road = new Road("Road-SameNode", node, node, Normal)

    an[IllegalArgumentException] should be thrownBy RoadManager.addRoad(road)

    RoadManager.roads.contains("Road-SameNode") shouldEqual false
  }

  it should "not allow adding a duplicate road with reversed start and end nodes" in {
    val nodeA = RoadManager.getOrCreateNode("Node-A")
    val nodeB = RoadManager.getOrCreateNode("Node-B")
    val road1 = new Road("Road-1", nodeA, nodeB, Normal)
    val road2 = new Road("Road-2", nodeB, nodeA, Normal)

    RoadManager.addRoad(road1)
    an[IllegalArgumentException] should be thrownBy RoadManager.addRoad(road2)

    RoadManager.roads.size shouldEqual 1
    RoadManager.roads.contains("Road-1") shouldEqual true
  }

  it should "not allow adding a road with null start or end nodes" in {
    val startNode = null
    val endNode = RoadManager.getOrCreateNode("Node-End")
    an[IllegalArgumentException] should be thrownBy {
      RoadManager.addRoad(new Road("Invalid-Road", startNode, endNode, Normal))
    }

    val validNode = RoadManager.getOrCreateNode("Node-Start")
    an[IllegalArgumentException] should be thrownBy {
      RoadManager.addRoad(new Road("Another-Invalid-Road", validNode, null, Normal))
    }
  }

  it should "not allow roads with id null or empty string" in {
    val startNode = RoadManager.getOrCreateNode("start")
    val endNode = RoadManager.getOrCreateNode("end")

    // Test for null ID
    val roadWithNullId = new Road(null, startNode, endNode, Normal)
    an[IllegalArgumentException] should be thrownBy RoadManager.addRoad(roadWithNullId)
    RoadManager.roads.size shouldEqual 0

    // Test for empty string ID
    val roadWithEmptyId = new Road("", startNode, endNode, Normal)
    an[IllegalArgumentException] should be thrownBy RoadManager.addRoad(roadWithEmptyId)
    RoadManager.roads.size shouldEqual 0
  }

  it should "handle adding a road when nodes already exist" in {
    val existingNode = RoadManager.getOrCreateNode("Existing-Node")
    val newNode = RoadManager.getOrCreateNode("New-Node")
    val road = new Road("Road-Existing", existingNode, newNode, Normal)

    RoadManager.addRoad(road)

    RoadManager.roads.contains("Road-Existing") shouldEqual true
    RoadManager.nodes.contains("Existing-Node") shouldEqual true
    RoadManager.nodes.contains("New-Node") shouldEqual true
  }





  // ============================================================
  // Tests for Retrieving Roads
  // ============================================================

  it should "retrieve a road by its ID" in {
    val startNode = RoadManager.getOrCreateNode("Node-5")
    val endNode = RoadManager.getOrCreateNode("Node-6")
    val road = new Road("Road-2", startNode, endNode, UnPaved)

    RoadManager.addRoad(road)
    val retrievedRoad = RoadManager.getRoad("Road-2")

    retrievedRoad.isDefined shouldEqual true
    retrievedRoad.get.id shouldEqual "Road-2"
    retrievedRoad.get.start shouldEqual startNode
    retrievedRoad.get.end shouldEqual endNode
    retrievedRoad.get.roadType shouldEqual UnPaved
  }

  it should "return None for a non-existing road ID" in {
    val retrievedRoad = RoadManager.getRoad("Non-Existing-ID")
    retrievedRoad.isDefined shouldEqual false
  }


  it should "retrieve all roads connected to a node" in {
    val nodeA = RoadManager.getOrCreateNode("Node-A")
    val nodeB = RoadManager.getOrCreateNode("Node-B")
    val nodeC = RoadManager.getOrCreateNode("Node-C")
    val road1 = new Road("Road-1", nodeA, nodeB, Normal)
    val road2 = new Road("Road-2", nodeA, nodeC, UnPaved)

    RoadManager.addRoad(road1)
    RoadManager.addRoad(road2)

    val connectedRoads = RoadManager.getConnectedRoads("Node-A")
    connectedRoads.size shouldEqual 2
    connectedRoads.map(_.id) should contain allOf("Road-1", "Road-2")
  }

  it should "return an empty list for a non-existent node ID" in {
    val connectedRoads = RoadManager.getConnectedRoads("Non-Existent")
    connectedRoads shouldBe empty
  }

  it should "return an empty list for a node with no connected roads" in {
    val node = RoadManager.getOrCreateNode("Node-Isolated")
    RoadManager.getConnectedRoads("Node-Isolated") shouldBe empty
  }

  it should "find a road between two nodes in the forward direction" in {
    val nodeA = RoadManager.getOrCreateNode("Node-A")
    val nodeB = RoadManager.getOrCreateNode("Node-B")
    val road = new Road("Road-Forward", nodeA, nodeB, Normal)

    RoadManager.addRoad(road)
    val foundRoad = RoadManager.findRoadBetween("Node-A", "Node-B")

    foundRoad.isDefined shouldEqual true
    foundRoad.get.id shouldEqual "Road-Forward"
  }

  it should "find a road between two nodes in the reverse direction" in {
    val nodeA = RoadManager.getOrCreateNode("Node-A")
    val nodeB = RoadManager.getOrCreateNode("Node-B")
    val road = new Road("Road-Reverse", nodeA, nodeB, Normal)

    RoadManager.addRoad(road)
    val foundRoad = RoadManager.findRoadBetween("Node-B", "Node-A")

    foundRoad.isDefined shouldEqual true
    foundRoad.get.id shouldEqual "Road-Reverse"
  }

  it should "return None if no road exists between two nodes" in {
    val nodeA = RoadManager.getOrCreateNode("Node-X")
    val nodeB = RoadManager.getOrCreateNode("Node-Y")

    val foundRoad = RoadManager.findRoadBetween("Node-X", "Node-Y")
    foundRoad shouldEqual None
  }

  it should "return the correct road among multiple roads" in {
    val nodeA = RoadManager.getOrCreateNode("Node-A")
    val nodeB = RoadManager.getOrCreateNode("Node-B")
    val nodeC = RoadManager.getOrCreateNode("Node-C")
    val roadAB = new Road("Road-AB", nodeA, nodeB, Normal)
    val roadBC = new Road("Road-BC", nodeB, nodeC, Normal)

    RoadManager.addRoad(roadAB)
    RoadManager.addRoad(roadBC)

    val foundRoad = RoadManager.findRoadBetween("Node-A", "Node-B")
    foundRoad.isDefined shouldEqual true
    foundRoad.get.id shouldEqual "Road-AB"
  }

  it should "not find a road if multiple unrelated roads exist" in {
    val nodeA = RoadManager.getOrCreateNode("Node-A")
    val nodeB = RoadManager.getOrCreateNode("Node-B")
    val unrelatedRoad = new Road("Road-X", nodeA, RoadManager.getOrCreateNode("Node-C"), Normal)

    RoadManager.addRoad(unrelatedRoad)
    val foundRoad = RoadManager.findRoadBetween("Node-A", "Node-B")
    foundRoad shouldEqual None
  }


  it should "return connected roads for a node with multiple connections" in {
    val nodeA = RoadManager.getOrCreateNode("Node-A")
    val nodeB = RoadManager.getOrCreateNode("Node-B")
    val nodeC = RoadManager.getOrCreateNode("Node-C")

    val road1 = new Road("Road-1", nodeA, nodeB, Normal)
    val road2 = new Road("Road-2", nodeA, nodeC, UnPaved)

    RoadManager.addRoad(road1)
    RoadManager.addRoad(road2)

    val connectedRoads = RoadManager.getConnectedRoads("Node-A")
    connectedRoads.size shouldEqual 2
    connectedRoads.map(_.id) should contain allOf("Road-1", "Road-2")
  }

  it should "return an empty list for a node with no connections" in {
    val isolatedNode = RoadManager.getOrCreateNode("Node-Isolated")
    RoadManager.getConnectedRoads("Node-Isolated") shouldBe empty
  }

  it should "return an empty list for a non-existent node" in {
    val connectedRoads = RoadManager.getConnectedRoads("Non-Existent-Node")
    connectedRoads shouldBe empty
  }

  it should "handle connected roads bidirectionally" in {
    val nodeA = RoadManager.getOrCreateNode("Node-A")
    val nodeB = RoadManager.getOrCreateNode("Node-B")

    val road = new Road("Road-AB", nodeA, nodeB, Normal)
    RoadManager.addRoad(road)

    val roadsFromA = RoadManager.getConnectedRoads("Node-A")
    val roadsFromB = RoadManager.getConnectedRoads("Node-B")

    roadsFromA.size shouldEqual 1
    roadsFromA.head.id shouldEqual "Road-AB"

    roadsFromB.size shouldEqual 1
    roadsFromB.head.id shouldEqual "Road-AB"
  }

  // ============================================================
  // Tests for Updating Roads
  // ============================================================

  it should "update an existing road correctly" in {
    val startNode1 = RoadManager.getOrCreateNode("Node-1")
    val endNode1 = RoadManager.getOrCreateNode("Node-2")
    val road1 = new Road("Road-1", startNode1, endNode1, Normal)

    val startNode2 = RoadManager.getOrCreateNode("Node-3")
    val endNode2 = RoadManager.getOrCreateNode("Node-4")
    val updatedRoad = new Road("Road-1", startNode2, endNode2, RoadType.Double)

    RoadManager.addRoad(road1)
    RoadManager.addRoad(updatedRoad)

    val retrievedRoad = RoadManager.getRoad("Road-1")

    retrievedRoad.isDefined shouldEqual true
    retrievedRoad.get.start shouldEqual startNode2
    retrievedRoad.get.end shouldEqual endNode2
    retrievedRoad.get.roadType shouldEqual RoadType.Double

    startNode1.connectedRoads.contains("Road-1") shouldEqual false
    endNode1.connectedRoads.contains("Road-1") shouldEqual false
    startNode2.connectedRoads.contains("Road-1") shouldEqual true
    endNode2.connectedRoads.contains("Road-1") shouldEqual true
  }

  // ============================================================
  // Tests for Removing Roads
  // ============================================================

  it should "remove a road by its ID" in {
    val startNode = RoadManager.getOrCreateNode("Node-7")
    val endNode = RoadManager.getOrCreateNode("Node-8")
    val road = new Road("Road-3", startNode, endNode, Normal)

    RoadManager.addRoad(road)
    RoadManager.removeRoad("Road-3")
    val retrievedRoad = RoadManager.getRoad("Road-3")

    retrievedRoad.isDefined shouldEqual false

    startNode.connectedRoads.contains("Road-3") shouldEqual false
    endNode.connectedRoads.contains("Road-3") shouldEqual false
    RoadManager.nodes.contains("Node-7") shouldEqual false
    RoadManager.nodes.contains("Node-8") shouldEqual false
  }

  it should "handle removing a road that does not exist" in {
    RoadManager.removeRoad("Non-Existent")
    RoadManager.roads.contains("Non-Existent") shouldEqual false
    RoadManager.roads.size shouldEqual 0

  }

  it should "update connected roads correctly after removing a road" in {
    val nodeA = RoadManager.getOrCreateNode("Node-A")
    val nodeB = RoadManager.getOrCreateNode("Node-B")
    val road = new Road("Road-1", nodeA, nodeB, Normal)

    RoadManager.addRoad(road)
    RoadManager.removeRoad("Road-1")

    val connectedRoads = RoadManager.getConnectedRoads("Node-A")
    connectedRoads shouldBe empty
  }

  // ============================================================
  // Tests for Edge Cases and Graph Consistency
  // ============================================================

  it should "not allow overlapping roads with the same anchors" in {
    val nodeA = RoadManager.getOrCreateNode("Node-A")
    val nodeB = RoadManager.getOrCreateNode("Node-B")
    val road1 = new Road("Road-1", nodeA, nodeB, Normal)
    val road2 = new Road("Road-2", nodeA, nodeB, UnPaved)

    RoadManager.addRoad(road1)
    an[IllegalArgumentException] should be thrownBy RoadManager.addRoad(road2)

    RoadManager.roads.size shouldEqual 1
    RoadManager.roads.contains("Road-1") shouldEqual true
  }

  it should "maintain graph consistency after bulk operations" in {
    val nodeA = RoadManager.getOrCreateNode("Node-A")
    val nodeB = RoadManager.getOrCreateNode("Node-B")
    val nodeC = RoadManager.getOrCreateNode("Node-C")
    val road1 = new Road("Road-1", nodeA, nodeB, Normal)
    val road2 = new Road("Road-2", nodeB, nodeC, UnPaved)

    RoadManager.addRoad(road1)
    RoadManager.addRoad(road2)

    RoadManager.roads.size shouldEqual 2
    RoadManager.nodes.size shouldEqual 3

    RoadManager.removeRoad("Road-1")
    RoadManager.removeRoad("Road-2")

    RoadManager.roads.isEmpty shouldEqual true
    RoadManager.nodes.isEmpty shouldEqual true
  }

  it should "handle a fully connected graph efficiently" in {
    val numNodes = 100
    val nodes = (1 to numNodes).map(i => RoadManager.getOrCreateNode(s"Node-$i"))
    val roads = for {
      i <- 0 until numNodes
      j <- (i + 1) until numNodes
    } yield new Road(s"Road-${i}-${j}", nodes(i), nodes(j), Normal)

    roads.foreach(RoadManager.addRoad)

    RoadManager.roads.size shouldEqual numNodes * (numNodes - 1) / 2
    nodes.foreach(node => node.connectedNodeIds.size shouldEqual numNodes - 1)
  }




  // ============================================================
  // Tests for Traffic Light Status Update
  // ============================================================

  it should "update traffic light status correctly for valid nodes" in {
    val intersectionNode = RoadManager.getOrCreateNode("Intersection-Node")

    // an intersection with 3 connected roads
    intersectionNode.connectedRoads ++= Seq(
      "Road-1" -> new Road("Road-1", intersectionNode, new RoadNode("Node-1"), RoadType.Normal),
      "Road-2" -> new Road("Road-2", intersectionNode, new RoadNode("Node-2"), RoadType.Normal),
      "Road-3" -> new Road("Road-3", intersectionNode, new RoadNode("Node-3"), RoadType.Normal)
    )

    // Add a traffic light
    RoadManager.updateTrafficLightStatus("Intersection-Node", trafficLight = true)
    intersectionNode.trafficLight shouldEqual true

    // Remove the traffic light
    RoadManager.updateTrafficLightStatus("Intersection-Node", trafficLight = false)
    intersectionNode.trafficLight shouldEqual false
  }

  it should "throw an exception when trying to add a traffic light to a non-intersection node" in {
    val regularNode = RoadManager.getOrCreateNode("Regular-Node")

    //a node with less than 3 connections
    regularNode.connectedRoads ++= Seq(
      "Road-1" -> new Road("Road-1", regularNode, new RoadNode("Node-1"), RoadType.Normal)
    )

    an[IllegalArgumentException] should be thrownBy {
      RoadManager.updateTrafficLightStatus("Regular-Node", trafficLight = true)
    }
    regularNode.trafficLight shouldEqual false
  }

  it should "not throw an exception when removing a traffic light from a non-intersection node" in {
    val regularNode = RoadManager.getOrCreateNode("Regular-Node")

    // Simulate a non-intersection node with a traffic light
    regularNode.trafficLight = true

    noException should be thrownBy {
      RoadManager.updateTrafficLightStatus("Regular-Node", trafficLight = false)
    }
    regularNode.trafficLight shouldEqual false
  }

  it should "do nothing for a non-existent node when trying to add a trafficLight" in {
    val initialNodeCount = RoadManager.nodes.size

    noException should be thrownBy {
      RoadManager.updateTrafficLightStatus("Non-Existent-Node", trafficLight = true)
    }

    RoadManager.nodes.size shouldEqual initialNodeCount
  }

}