package de.thm.move.Roads

import de.thm.move.MoveSpec
import de.thm.move.types.Point

class RoadTest extends MoveSpec {

  "A Road" should "initialize correctly with given parameters" in {
    val start = new RoadNode("node-1")
    val end = new RoadNode("node-2")
    val road = new Road("road-1", start, end, RoadType.Normal)

    road.id shouldEqual "road-1"
    road.start shouldEqual start
    road.end shouldEqual end
    road.roadType shouldEqual RoadType.Normal
  }

  it should "allow updating the id field" in {
    val startNode = new RoadNode("node-1")
    val endNode = new RoadNode("node-2")
    val road = new Road("road-1", startNode, endNode)
    road.id = "new-road-id"

    road.id shouldEqual "new-road-id"
  }
  
  it should "allow updating the road type" in {
    val startNode = new RoadNode("node-1")
    val endNode = new RoadNode("node-2")
    val road = new Road("road-1", startNode, endNode)
    road.roadType = RoadType.UnPaved

    road.roadType shouldEqual RoadType.UnPaved
  }

  it should "default to RoadType.Normal if not explicitly set" in {
    val startNode = new RoadNode("node-1")
    val endNode = new RoadNode("node-2")
    val road = new Road("road-2", startNode, endNode)

    road.roadType shouldEqual RoadType.Normal
  }

  it should "support different road types" in {
    val startNode = new RoadNode("node-1")
    val endNode = new RoadNode("node-2")

    val normalRoad = new Road("road-1", startNode, endNode, RoadType.Normal)
    val unpavedRoad = new Road("road-2", startNode, endNode, RoadType.UnPaved)
    val doubleRoad = new Road("road-3", startNode, endNode, RoadType.Double)

    normalRoad.roadType shouldEqual RoadType.Normal
    unpavedRoad.roadType shouldEqual RoadType.UnPaved
    doubleRoad.roadType shouldEqual RoadType.Double
  }
}