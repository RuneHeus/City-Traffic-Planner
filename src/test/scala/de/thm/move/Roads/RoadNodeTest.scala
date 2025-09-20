package de.thm.move.Roads

import de.thm.move.MoveSpec

class RoadNodeTest extends MoveSpec:

  // Group 1: Adding and Removing Roads
  "A RoadNode" should "add and remove roads correctly" in {
    val node = new RoadNode("Node-A")
    val road1 = new Road("Road-1", node, new RoadNode("Node-B"))
    val road2 = new Road("Road-2", node, new RoadNode("Node-C"))

    node.addRoad(road1)
    node.addRoad(road2)

    node.connectedRoads.size shouldEqual 2
    node.connectedRoads("Road-1") shouldEqual road1
    node.connectedRoads("Road-2") shouldEqual road2

    node.removeRoad("Road-1")
    node.connectedRoads.size shouldEqual 1
    node.connectedRoads.contains("Road-1") shouldEqual false
    node.connectedRoads("Road-2") shouldEqual road2
  }

  it should "not create duplicates when adding the same road multiple times" in {
    val node = new RoadNode("Node-A")
    val road = new Road("Road-1", node, new RoadNode("Node-B"))

    node.addRoad(road)
    node.addRoad(road)

    node.connectedRoads.size shouldEqual 1
    node.connectedRoads("Road-1") shouldEqual road
  }

  it should "handle removing non-existent roads gracefully" in {
    val node = new RoadNode("Node-A")
    noException should be thrownBy node.removeRoad("Non-Existent-Road")
    node.connectedRoads.isEmpty shouldEqual true
  }

  // Group 2: Node Connectivity
  it should "validate isNodeEmpty correctly" in {
    val node = new RoadNode("Node-A")
    node.isNodeEmpty shouldEqual true

    val road = new Road("Road-1", node, new RoadNode("Node-B"))
    node.addRoad(road)

    node.isNodeEmpty shouldEqual false
  }

  it should "check connections to other nodes" in {
    val nodeA = new RoadNode("Node-A")
    val nodeB = new RoadNode("Node-B")
    val nodeC = new RoadNode("Node-C")
    val road = new Road("Road-1", nodeA, nodeB)

    nodeA.addRoad(road)
    nodeB.addRoad(road)

    nodeA.isConnectedTo("Node-B") shouldEqual true
    nodeB.isConnectedTo("Node-A") shouldEqual true
    nodeA.isConnectedTo("Node-C") shouldEqual false
    nodeA.isConnectedTo("Node-A") shouldEqual false // Self-check
  }

  it should "return all directly connected node IDs" in {
    val node = new RoadNode("Node-A")
    val road1 = new Road("Road-1", node, new RoadNode("Node-B"))
    val road2 = new Road("Road-2", node, new RoadNode("Node-C"))

    node.addRoad(road1)
    node.addRoad(road2)

    node.connectedNodeIds shouldEqual Set("Node-B", "Node-C")
  }

  it should "exclude irrelevant or invalid connections" in {
    val node = new RoadNode("Node-A")
    val validRoad = new Road("Road-1", node, new RoadNode("Node-B"))
    val irrelevantRoad = new Road("Road-2", new RoadNode("Node-X"), new RoadNode("Node-Y"))

    node.addRoad(validRoad)
    an[IllegalArgumentException] should be thrownBy node.addRoad(irrelevantRoad)

    node.connectedNodeIds shouldEqual Set("Node-B")
  }

  it should "handle self-loops and exclude them from connections" in {
    val node = new RoadNode("Node-A")
    val selfLoop = new Road("Road-Self", node, node)

    an[IllegalArgumentException] should be thrownBy node.addRoad(selfLoop)
    node.connectedNodeIds shouldEqual Set.empty
  }

  it should "handle empty connectedNodeIds when no roads are connected" in {
    val node = new RoadNode("Node-A")
    node.connectedNodeIds shouldEqual Set.empty
  }

  it should "handle duplicate and mixed connections correctly" in {
    val node = new RoadNode("Node-A")
    val road1 = new Road("Road-1", node, new RoadNode("Node-B")) // Valid road
    val road2 = new Road("Road-2", node, new RoadNode("Node-B")) // Duplicate
    val irrelevantRoad = new Road("Road-3", new RoadNode("Node-X"), new RoadNode("Node-Y"))

    node.addRoad(road1)
    node.addRoad(road2)
    an[IllegalArgumentException] should be thrownBy node.addRoad(irrelevantRoad)

    node.connectedNodeIds shouldEqual Set("Node-B")
  }

  it should "evaluate complex connectivity scenarios" in {
    val nodeA = new RoadNode("Node-A")
    val nodeB = new RoadNode("Node-B")
    val nodeC = new RoadNode("Node-C")
    val nodeD = new RoadNode("Node-D")
    val road1 = new Road("Road-1", nodeA, nodeB)
    val road2 = new Road("Road-2", nodeB, nodeC)
    val road3 = new Road("Road-3", nodeC, nodeD)

    nodeA.addRoad(road1)
    nodeB.addRoad(road2)
    nodeC.addRoad(road3)

    nodeA.isConnectedTo("Node-B") shouldEqual true
    nodeA.isConnectedTo("Node-C") shouldEqual false // No direct connection
    nodeB.isConnectedTo("Node-C") shouldEqual true
    nodeC.isConnectedTo("Node-D") shouldEqual true
  }

  // Group 3: Traffic Light Behavior
  it should "handle trafficLight updates correctly" in {
    val node = new RoadNode("Node-A")
    node.trafficLight shouldEqual false

    node.trafficLight = true
    node.trafficLight shouldEqual true
  }

  // Group 4: String Representation
  it should "provide a correct string representation" in {
    val node = new RoadNode("Node-A")
    val road = new Road("Road-1", node, new RoadNode("Node-B"))

    node.addRoad(road)
    node.toString should include("RoadNode(id=Node-A")
    node.toString should include("connectedNodes=Set(Node-B)")

    val isolatedNode = new RoadNode("Node-Isolated")
    isolatedNode.toString should include("RoadNode(id=Node-Isolated")
    isolatedNode.toString should include("connectedNodes=Set()")
  }