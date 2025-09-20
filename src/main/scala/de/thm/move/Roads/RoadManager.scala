package de.thm.move.Roads

import de.thm.move.types.Point
import de.thm.move.Roads.Road
import de.thm.move.models.LinePattern
import de.thm.move.views.shapes.{ResizableLine, ResizableShape, SerializableColor}
import os.read
import upickle.default.read
import upickle.legacy.{ReadWriter, readwriter, writeJs}

import scala.collection.mutable

/** Manages a collection of roads, providing functionality to add, retrieve, and
  * remove roads.
  */
object RoadManager:
  /** A mutable map that stores roads by their unique identifier.
    */
  val roads: mutable.Map[String, Road] = mutable.Map()
  /** A mutable map that stores roadNodes by their unique identifier.
   */
  val nodes: mutable.Map[String, RoadNode] = mutable.Map()
  /** The defined width for a one way road.
   *  */
  val oneWayThickness = 1

  /**
   * Retrieves a `RoadNode` from the map by its ID or creates a new one if it does not exist.
   * Ensures that nodes with the same ID are always represented by the same instance.
   *
   * @param id the unique identifier of the `RoadNode`.
   * @return the `RoadNode` associated with the given ID, either retrieved or newly created.
   */
  def getOrCreateNode(id: String): RoadNode =
    nodes.getOrElseUpdate(id, RoadNode(id))

  /**
   * Retrieves a `RoadNode` from the map by its ID or adds the provided `RoadNode`
   * to the map if it does not already exist. Ensures that nodes with the same ID
   * are always represented by the same instance.
   *
   * @param node the `RoadNode` to retrieve or add if it does not exist in the map.
   * @return the `RoadNode` associated with the given ID, either retrieved or added.
   */
  def getOrCreateNode(node: RoadNode): RoadNode =
    nodes.getOrElseUpdate(node.id, node)


  /** Adds a road to the manager or updates an existing road with the same ID.
    *
    * @param road
    *   the road to add or update in the collection
    */

  /** Adds a road to the manager or updates an existing road with the same ID. */
  def addRoad(road: Road): Unit = {
    require(road.id != null && road.id.nonEmpty, "Road ID cannot be null or empty") // to enforce that every road id has a meaning
    require(road.start != null && road.end != null, "Start and end nodes cannot be null.")
    if road.start == road.end then
      throw IllegalArgumentException("SelfLoop connections are not allowed.") // A road should not have the same start and end node

    // Check if a road with the same anchors exists
    val duplicateRoadExists = roads.values.exists { existingRoad =>
      (existingRoad.start == road.start && existingRoad.end == road.end) ||
        (existingRoad.start == road.end && existingRoad.end == road.start) // Check for bidirectional equivalence
    }

    if duplicateRoadExists then
      throw IllegalArgumentException("A road with the same anchors already exists.")

    // Check if a road with the same ID exists
    roads.get(road.id).foreach { existingRoad =>
      // Remove the road from the connectedRoads of the old nodes
      existingRoad.start.removeRoad(existingRoad.id)
      existingRoad.end.removeRoad(existingRoad.id)

      // Remove old nodes if they are empty
      if (existingRoad.start.isNodeEmpty) then nodes.remove(existingRoad.start.id)
      if (existingRoad.end.isNodeEmpty) then nodes.remove(existingRoad.end.id)
    }

    // Ensure both nodes exist in the map
    val startNode = getOrCreateNode(road.start)
    val endNode = getOrCreateNode(road.end)

    // Add the road to the roads collection
    roads(road.id) = road

    // Add the road to the connectedRoads of the new start and end nodes
    startNode.addRoad(road)
    endNode.addRoad(road)
  }

  def updateTrafficLightStatus(nodeId: String, trafficLight: Boolean): Unit =
    nodes.get(nodeId).foreach { node =>
      if trafficLight && !node.canHaveTrafficLight then
        throw new IllegalArgumentException(s"Cannot add traffic light to node $nodeId: not an intersection.")
      else
        node.trafficLight = trafficLight
        val action = if trafficLight then "added" else "removed"
        println(s"Traffic light $action for RoadNode with ID: $nodeId")
    }

    /**
     * Retrieves a list of road nodes that have traffic lights.
     *
     * @return A `List` of `RoadNode` instances that are identified as traffic light nodes.
     */
  def getTrafficLights: List[RoadNode] = nodes.values.filter(_.trafficLight).toList


  /** Retrieves a road by its unique identifier.
    *
    * @param id
    *   the unique identifier of the road to retrieve
    * @return
    *   an `Option` containing the road if found, or `None` if no road with the
    *   given ID exists
    */
  def getRoad(id: String): Option[Road] = roads.get(id)

  /** Removes a road from the manager by its unique identifier.
    *
    * @param id
    *   the unique identifier of the road to remove
    */
  /** Removes a road from the manager by its unique identifier. */
  def removeRoad(id: String): Unit = {
    roads.get(id).foreach { road =>
      // Remove the road from the connectedRoads of the start and end nodes
      road.start.removeRoad(id)
      road.end.removeRoad(id)

      // Remove the road from the roads collection
      roads.remove(id)

      // Check if the start node is empty and remove it if necessary
      if (road.start.isNodeEmpty) then {
        nodes.remove(road.start.id)
      }

      // Check if the end node is empty and remove it if necessary
      if (road.end.isNodeEmpty) then {
        nodes.remove(road.end.id)
      }
    }
  }

  /**
   * Retrieves all roads directly connected to a specified node.
   *
   * @param nodeId
   * The unique identifier of the node for which to retrieve connected roads.
   * @return
   * A list of `Road` objects directly connected to the specified node.
   * Returns an empty list if the node does not exist or has no connected roads.
   */
  def getConnectedRoads(nodeId: String): List[Road] =
    nodes.get(nodeId).map(_.connectedRoads.values.toList).getOrElse(List.empty)

  /**
   * Finds a road that directly connects two specified nodes.
   *
   * @param nodeAId
   * The unique identifier of the first node.
   * @param nodeBId
   * The unique identifier of the second node.
   * @return
   * An `Option[Road]` containing the `Road` object if a road exists between the two nodes,
   * or `None` if no such road is found.
   */
  def findRoadBetween(nodeAId: String, nodeBId: String): Option[Road] =
    roads.values.find(road =>
      (road.start.id == nodeAId && road.end.id == nodeBId) ||
        (road.start.id == nodeBId && road.end.id == nodeAId)
    )

  

    /** Gets the road of a given id. We can then use this to access its individual properties
     * @param id the unique identifier of the road
     * @return the road linked to that id
     * @throws Exception throw an exception if the given id is not linked to a road.
     *             We assume that the given id will always be linked to a road.
     * */
  def getRoadProperties(id: String): Road =
    getRoad(id) match {
      case Some(road) => road
      case None => throw new NoSuchElementException("No road found with given id")
    }


  def visualizeGraph(): Unit = {
    println("Graph Visualization:\n====================")

    if nodes.isEmpty then
      println("No nodes or roads in the graph.")
      return

    nodes.values.foreach { node =>
      println(s"Node [${node.id}]")
      if node.connectedRoads.isEmpty then
        println("  No connected roads.")
      else
        node.connectedRoads.values.foreach { road =>
          val connectedNodeId = if road.start.id == node.id then road.end.id else road.start.id
          val direction = if road.start.id == node.id then "->" else "<-"
          println(f"  $direction ${connectedNodeId}%-15s [Road ID: ${road.id}, Type: ${road.roadType}]")
        }
    }

    println("\nSummary:\n--------")
    println(s"Total Nodes: ${nodes.size}")
    println(s"Total Roads: ${roads.size}")

  }


  implicit val roadManagerRW: ReadWriter[RoadManager.type] = readwriter[ujson.Value].bimap(
    // Serialization
    rm => {
      val trafficLightNodes = rm.nodes.values
        .filter(_.trafficLight) // Only include nodes with traffic lights
        .map(node => ujson.Obj("id" -> node.id))
        .toSeq

      ujson.Obj(
        "trafficLights" -> trafficLightNodes
      )
    },
    // Deserialization
    json => json("type").str match {
      case "RoadManager" =>
        val deserializedTrafficLights = json("trafficLights").arr
  
        // Clear all traffic light states before applying deserialized data
        RoadManager.nodes.values.foreach(_.trafficLight = false)
  
        // Apply traffic light states using the provided updateTrafficLightStatus method
        deserializedTrafficLights.foreach { tlNode =>
          val nodeId = tlNode("id").str
          RoadManager.updateTrafficLightStatus(nodeId, true)
        }

        RoadManager
      case _ => throw new IllegalArgumentException("Unexpected shape type")
    }
  )