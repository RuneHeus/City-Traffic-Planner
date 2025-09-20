package de.thm.move.Roads

import de.thm.move.Roads.RoadNode.*
import de.thm.move.types.Point
import de.thm.move.views.shapes.ResizablePolygon.rw
import de.thm.move.views.shapes.{ResizableLandLot, ResizablePolygon}
import upickle.default.{ReadWriter, readwriter, *}
import upickle.default.ReadWriter.join

/** Represents the type of a road in the system.
  */
enum RoadType:
  case Normal, UnPaved, Double



/** Represents if a road is a one-way road, if so, it specifies the direction. Can be None, Front, Back.
=======
/** Represents if a road is a one-way road, if so, it specifies the direction.
*/
*/
enum OneWayLabel:
  case None, Front, Back

/** Represents a road in the network connecting two nodes.

 *
 * @param id
 *   Unique identifier for the road
 * @param start
 *   The starting node of the road
 * @param end
 *   The ending node of the road
 * @param roadType
 *   The type of the road (default is `Normal`)
 * @param one_way
 *   Whether a road is a one-way road, and if so, the direction of the one-way road.
 */

class Road(
    var id: String,
    val start: RoadNode,
    val end: RoadNode,
    var roadType: RoadType = RoadType.Normal,
    var one_way: OneWayLabel = OneWayLabel.None
):
  /** Checks if this road connects to a specific node.
    *
    * @param nodeId
    *   The ID of the node
    * @return
    *   True if the road connects to the given node
    */
  def connectsTo(nodeId: String): Boolean =
    start.id == nodeId || end.id == nodeId

  override def toString: String =
    s"Road(id=$id, start=${start.id}, end=${end.id}, roadType=$roadType)"

  private var connectedLandLots: List[ResizablePolygon] = List()

  def registerLandLot(newLot: ResizablePolygon): Unit =
    this.connectedLandLots = this.connectedLandLots.::(newLot)

  def unregisterLandLot(oldLot: ResizablePolygon): Unit =
    this.connectedLandLots = this.connectedLandLots.filter((lot: ResizablePolygon) => lot != oldLot)

  def getLots: List[ResizablePolygon] = this.connectedLandLots

object Road:
  implicit val roadTypeRw: ReadWriter[RoadType] = readwriter[ujson.Value].bimap[RoadType](
    // Serialization
    {
      case RoadType.Normal  => ujson.Str("Normal")
      case RoadType.UnPaved => ujson.Str("UnPaved")
      case RoadType.Double  => ujson.Str("Double")
    },
    // Deserialization
    {
      case ujson.Str("Normal")  => RoadType.Normal
      case ujson.Str("UnPaved") => RoadType.UnPaved
      case ujson.Str("Double")  => RoadType.Double
      case other                => throw new IllegalArgumentException(s"Unknown RoadType: $other")
    }
  )

  implicit val oneWayLabelRw: ReadWriter[OneWayLabel] = readwriter[ujson.Value].bimap[OneWayLabel](
    {
      case OneWayLabel.None  => ujson.Str("None")
      case OneWayLabel.Front => ujson.Str("Front")
      case OneWayLabel.Back  => ujson.Str("Back")
    },
    {
      case ujson.Str("None")  => OneWayLabel.None
      case ujson.Str("Front") => OneWayLabel.Front
      case ujson.Str("Back")  => OneWayLabel.Back
      case other              => throw new IllegalArgumentException(s"Unknown OneWayLabel: $other")
    }
  )

  implicit val rw: ReadWriter[Road] = readwriter[ujson.Value].bimap[Road](
    road =>
      ujson.Obj(
        "type" -> "Road",
        "id" -> road.id,
        "start" -> road.start.id,
        "end" -> road.end.id,
        "roadType" -> writeJs(road.roadType),
        "one_way" -> writeJs(road.one_way)
      ),
    json =>
      Road(
        id = json("id").str,
        start = RoadNode(json("start").str),
        end = RoadNode(json("end").str),
        roadType = read[RoadType](json("roadType")),
        one_way = read[OneWayLabel](json("one_way"))
      )
  )
