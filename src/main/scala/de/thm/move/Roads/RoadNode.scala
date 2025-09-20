package de.thm.move.Roads

import de.thm.move.types.Point
import os.read
import upickle.legacy.{ReadWriter, macroRW, readwriter, writeJs}

import scala.collection.mutable

/** Represents a node in a road network.
 *
 * @param id
 *   Unique identifier for the node
 */
class RoadNode(val id: String):
  /** Indicates if the node has a traffic light*/
  var trafficLight: Boolean = false

  /** A collection of connected roads, keyed by road ID
  */
  val connectedRoads: mutable.Map[String, Road] = mutable.Map()


  /** Checks if the node can have a traffic light.
   *
   * A node must have at least three connected roads to qualify.
   *
   * @return
   * `true` if eligible, `false` otherwise.
   */
  def canHaveTrafficLight: Boolean = connectedRoads.size >= 3

  /** Adds a connected road to this node.
   *
   * @param road
   *   The road to add
   */
  def addRoad(road: Road): Unit =
    if road.start.id == id && road.end.id == id then
      throw new IllegalArgumentException("Self-loops are not allowed.")
    else if road.start.id == id || road.end.id == id then
      connectedRoads.put(road.id, road)
    else
      throw new IllegalArgumentException(s"Road ${road.id} does not connect to node $id.")

  /** Removes a connected road from this node.
   *
   * @param roadId
   *   The ID of the road to remove
   */
  def removeRoad(roadId: String): Unit =
    connectedRoads.remove(roadId)

  /** Checks if this node is connected to another node via a road.
   *
   * @param nodeId
   *   The ID of the other node
   * @return
   *   True if there is a direct road connection
   */
  def isConnectedTo(nodeId: String): Boolean =
    if nodeId == id then false // Self-loops are not considered valid connections
    else connectedRoads.values.exists(road =>
      road.connectsTo(id) && (road.start.id == nodeId || road.end.id == nodeId)
    )
  
  /** Checks if this node has no connected roads. */
  def isNodeEmpty: Boolean =
    connectedRoads.isEmpty

  /** Retrieves IDs of all directly connected nodes.
   *
   * @return
   *   A set of IDs for directly connected nodes
   */
  def connectedNodeIds: Set[String] =
    connectedRoads.values.flatMap(road => Set(road.start.id, road.end.id)).filter(_ != id).toSet

  override def toString: String =
    s"RoadNode(id=$id, trafficLight=$trafficLight, connectedNodes=${connectedNodeIds})"

object RoadNode {
  implicit val rw: ReadWriter[RoadNode] = readwriter[ujson.Value].bimap[RoadNode](
    // Serialization
    node => {
      ujson.Obj(
        "id" -> node.id,
        "trafficLight" -> node.trafficLight,
      )
    },
    // Deserialization
    json => {
      val node = new RoadNode(json("id").str)
      node.trafficLight = json("trafficLight").bool
      node
    }
  )
}