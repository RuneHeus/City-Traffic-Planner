/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */



package de.thm.move.views.shapes

import de.thm.move.Global
import de.thm.move.history.History
import javafx.scene.shape.Line
import de.thm.move.views.shapes.ColorizableShape.{fillPatternRW, linePatternRW}
import de.thm.move.views.shapes.SerializableColor
import de.thm.move.types.*
import javafx.scene.paint.Color
import upickle.default.*
import de.thm.move.views.anchors.*
import de.thm.move.views.anchors.SharedAnchor
import de.thm.move.types.*
import de.thm.move.util.JFxUtils.withConsumedEvent
import javafx.scene.input.MouseEvent
import java.util.UUID

import de.thm.move.Roads.RoadManager

import scala.collection.mutable.ListBuffer
import scala.collection.mutable
import scala.math.*
import de.thm.move.views.shapes.ResizableLandLot
import de.thm.move.Roads.Road
import de.thm.move.lotOperations._
/**
 * Represents a resizable line with configurable start and end points,
 * support for snapping to anchors, and resizing with anchors.
 *
 * @param start      the starting point of the line as a tuple (x, y)
 * @param end        the ending point of the line as a tuple (x, y)
 * @param strokeSize the thickness of the line
 */
class ResizableLine(start: Point, end: Point, strokeSize: Int)
    extends Line(start._1, start._2, end._1, end._2)
    with ResizableShape
    with ColorizableShape
    with PathLike
    with checkLandLotsOutsideZone
    with moveLotWithRoad:

  setStrokeWidth(strokeSize)
  override lazy val edgeCount: Int = 2

  override def copy: ResizableLine =
    val duplicate =
      new ResizableLine((getStartX, getStartY), (getEndX, getEndY), strokeSize)
    duplicate.copyColors(this)
    duplicate.setRotate(getRotate)
    duplicate

  override def getEdgePoint(idx: Int): (Double, Double) = idx match
    case 0 => (getStartX, getStartY)
    case 1 => (getEndX, getEndY)
    case _ =>
      throw new IllegalArgumentException(
        s"There is now edge with given idx $idx"
      )

  override protected def boundsChanged(): Unit = null // TODO: this causes the inf loop with the snapping, need to fix this. This is done to not break other shapes.

  /**
   * Creates a new anchor at the specified position.
   *
   * @param x the x-coordinate
   * @param y the y-coordinate
   * @return a new SharedAnchor instance
   */
  override protected def makeAnchor(x: Double, y: Double): SharedAnchor =
    val anchor = new SharedAnchor(x,y)
    anchor.setId(UUID.randomUUID().toString) // Generate unique ID
    anchor


  /**
   * Resizes the line by moving the specified anchor.
   *
   * @param idx the index of the anchor (0 for start, 1 for end)
   * @param delta the amount to move the anchor (dx, dy)
   */
  override def resizeWithAnchor(idx: Int, delta: (Double, Double)): Unit =
    val sharedAnchor = getAnchors(idx)
    sharedAnchor.setCenterX(sharedAnchor.getCenterX + delta.x)
    sharedAnchor.setCenterY(sharedAnchor.getCenterY + delta.y)
    sharedAnchor.moveWithSharedAnchor(delta)

  /**
   * Stores the predefined anchors for this `ResizableLine`.
   * If this list is non-empty, these anchors will be used instead of generating new ones.
   */
  private var predefinedAnchors: List[SharedAnchor] = List.empty

  /**
   * Sets the predefined anchors for this `ResizableLine`.
   * These anchors will override the automatically generated anchors, allowing for customization.
   *
   * @param anchors the list of predefined `SharedAnchor` objects to associate with this `ResizableLine`
   */
  def setPredefinedAnchors(anchors: List[SharedAnchor]): Unit =
    predefinedAnchors = anchors

  /**
   * Retrieves the list of anchors for this `ResizableLine`.
   * If the `predefinedAnchors` list is non-empty, it will return that list;
   * otherwise, it will generate anchors using the `genAnchors` method.
   *
   * @return a list of `SharedAnchor` objects representing the current anchors of this `ResizableLine`
   */
  override lazy val getAnchors: List[SharedAnchor] =
    if predefinedAnchors.isEmpty then genAnchors else predefinedAnchors

  /**
   * Creates a single anchor at the specified index and initializes its behavior.
   *
   * @param idx the index of the anchor (0 for start, 1 for end)
   * @return a SharedAnchor object
   */
  def createSingleAnchor(idx: Int): SharedAnchor =
    val (x, y) = getEdgePoint(idx)
    val anchor = makeAnchor(x, y)
    var startP = (0.0, 0.0)
    var mouseP = startP

    anchor.addConnection(this, idx)

    anchor.setOnMousePressed(withConsumedEvent { (me: MouseEvent) =>
      startP = (me.getSceneX, me.getSceneY)
      mouseP = startP
    })
    anchor.setOnMouseDragged(withConsumedEvent { (mv: MouseEvent) =>
      val delta = (mv.getSceneX - mouseP.x, mv.getSceneY - mouseP.y)
      resizeWithAnchor(idx, delta)

      //land lot has to move together with the line.
      RoadManager.getRoad(this.getId) match {
        case Some(road: Road) =>
          // If the road exists, move the associated land lots
          moveWithLine(road.getLots, delta, this)
        case None =>
        // Handle the case where the road does not exist
        // because the road has been deleted already but the anchor is shared by another road
      }

      mouseP = (mv.getSceneX, mv.getSceneY)
    })
    anchor.setOnMouseReleased(withConsumedEvent { (mv: MouseEvent) =>
      // calculate delta (offset from original position) for un-/redo
      checkLandLotsOutside()
      val deltaRedo = (mv.getSceneX - startP.x, mv.getSceneY - startP.y)
      val deltaUndo = deltaRedo.map(_ * (-1))

      val cmd = History.newCommand(
        resizeWithAnchor(idx, deltaRedo),
        resizeWithAnchor(idx, deltaUndo)
      )
      Global.history.save(cmd)
    })
    anchor

  /**
   * Generates the anchors for the line's start and end points.
   *
   * @return a list of SharedAnchor objects
   */
  override def genAnchors: List[SharedAnchor] =
    val tempAnchors = ListBuffer.tabulate(edgeCount)(createSingleAnchor)

    attemptSnapAnchors(tempAnchors) // Checks for snapping and adjusts the anchors
    tempAnchors.toList

  /**
   * Snaps a line edge to the given target anchor.
   *
   * @param lineIdx      the index of the line edge (0 for start, 1 for end)
   * @param targetAnchor the SharedAnchor to snap to
   */

  def snapToAnchor(lineIdx: Int, targetAnchor: SharedAnchor): Unit =
    val targetX = targetAnchor.getCenterX
    val targetY = targetAnchor.getCenterY

    lineIdx match
      case 0 => // Snap start anchor
        setStartX(targetX)
        setStartY(targetY)
        targetAnchor.addConnection(this, lineIdx)
      case 1 => // Snap end anchor
        setEndX(targetX)
        setEndY(targetY)
        targetAnchor.addConnection(this, lineIdx)
      case _ =>
        throw new IllegalArgumentException(
          s"No edge with the given index $lineIdx"
        )

  /**
   * Attempts to snap the line's anchors to nearby anchors within a threshold.
   *
   * @param tempAnchors the temporary list of anchors to adjust
   */
  private def attemptSnapAnchors(tempAnchors: ListBuffer[SharedAnchor]): Unit =
    val startAnchor = tempAnchors(0)
    val endAnchor = tempAnchors(1)



    // Find nearest snap candidates within threshold for both anchors
    val startSnapCandidate = ResizableLine.findNearestPointAnchor(startAnchor)
    val endSnapCandidate = ResizableLine.findNearestPointAnchor(endAnchor)
    (startSnapCandidate, endSnapCandidate) match
      // Case 1: Both want to snap to the same target
      case (Some(targetA), Some(targetB)) if targetA == targetB =>
        val startDist = ResizableLine.dist((startAnchor.getCenterX, startAnchor.getCenterY), (targetA.getCenterX, targetA.getCenterY))
        val endDist = ResizableLine.dist((endAnchor.getCenterX, endAnchor.getCenterY), (targetB.getCenterX, targetB.getCenterY))
        if startDist <= endDist then
          snapToAnchor(0, targetA)
          tempAnchors.update(0, targetA) // Update tempAnchors with snapped anchor
        else
          snapToAnchor(1, targetB)
          tempAnchors.update(1, targetB)

      // Case 2: Both have different targets, but targets are on the same line
      case (Some(targetA), Some(targetB)) if targetA != targetB && targetA.isConnectedToLine(targetB) =>
        val startDist = ResizableLine.dist((startAnchor.getCenterX, startAnchor.getCenterY), (targetA.getCenterX, targetA.getCenterY))
        val endDist = ResizableLine.dist((endAnchor.getCenterX, endAnchor.getCenterY), (targetB.getCenterX, targetB.getCenterY))
        if startDist <= endDist then
          snapToAnchor(0, targetA)
          tempAnchors.update(0, targetA)
        else
          snapToAnchor(1, targetB)
          tempAnchors.update(1, targetB)

      // Case 3: Only start anchor has a candidate
      case (Some(targetA), None) =>
        snapToAnchor(0, targetA)
        tempAnchors.update(0, targetA)

      // Case 4: Only end anchor has a candidate
      case (None, Some(targetB)) =>
        snapToAnchor(1, targetB)
        tempAnchors.update(1, targetB)

      // Case 5: Both have different targets, and targets are on different lines
      case (Some(targetA), Some(targetB)) if targetA != targetB =>
        snapToAnchor(0, targetA)
        snapToAnchor(1, targetB)
        tempAnchors.update(0, targetA)
        tempAnchors.update(1, targetB)

      // Case 6: No snap candidates within threshold
      case _ => // No snapping needed



  override def resize(idx: Int, delta: (Double, Double)): Unit = idx match
    case 0 =>
      setStartX(getStartX + delta.x)
      setStartY(getStartY + delta.y)
    case 1 =>
      setEndX(getEndX + delta.x)
      setEndY(getEndY + delta.y)
    case _ =>
      throw new IllegalArgumentException(
        s"There is now edge with given idx $idx"
      )


object ResizableLine:
  private val snapThreshold = 30.0
  val allLines: ListBuffer[ResizableLine] = ListBuffer()

  def addLine(line: ResizableLine): Unit =
    allLines += line

  def removeLine(line: ResizableLine): Unit =
    allLines -= line
    RoadManager.removeRoad(line.getId)

  def getAllAnchors: mutable.Set[SharedAnchor] =
    val anchorSet: mutable.Set[SharedAnchor]= mutable.Set()
    allLines.foreach(element =>
      anchorSet ++= element.getAnchors
    )
    anchorSet

  private def dist(p1:(Double, Double) , p2: (Double, Double)): Double =
    sqrt(pow(p1._1 - p2._1, 2) + pow(p1._2 - p2._2, 2))

  def findNearestPointAnchor(anchor: SharedAnchor): Option[SharedAnchor] =
    val anchors = getAllAnchors
    anchors.map(a => (a, dist((anchor.getCenterX, anchor.getCenterY), (a.getCenterX, a.getCenterY))))
      .filter((_, distance) => distance <= snapThreshold) // removing Anchors that are further away from the threshold
      .minByOption(_._2) // find the smallest distance or None
      .map(_._1) // None or extracting the nearest Anchor

  /** Implicit JSON serializer and deserializer for a point (x, y) represented as a tuple.
   * Each point is serialized as a JSON array with two numbers: x and y.
   */
  implicit val pointRW: ReadWriter[Point] = readwriter[ujson.Value].bimap[Point](
    p => ujson.Arr(p._1, p._2),
    json => (json(0).num, json(1).num)
  )

  /** Implicit JSON serializer and deserializer for ResizableLine.
   * Serializes the line as a JSON object containing the type, start point, end point, and stroke width.
   *
   * @example Serialization:
   * {{{
   *   val line = new ResizableLine((0.0, 0.0), (100.0, 100.0), 2)
   *   val json = write(line)
   *     }}}
   * @example Deserialization: *   {{{
   *   val jsonStr = """{"type": "ResizableLine", "start": [0.0, 0.0], "end": [100.0, 100.0], "strokeSize": 2}"""
   *   val line = read[ResizableLine](jsonStr)
   * }}}
   */

  implicit val rw: ReadWriter[ResizableLine] = readwriter[ujson.Value].bimap[ResizableLine](
    // Serialization
    line => {
      val fillColor = line.getFillColor match {
        case color: Color => writeJs(SerializableColor.colorToSerializableColor(color))
        case _ => writeJs(SerializableColor(0, 0, 0, 0)) // default transparent
      }
      val strokeColor = line.getStrokeColor match {
        case color: Color => writeJs(SerializableColor.colorToSerializableColor(color))
        case _ => writeJs(SerializableColor(0, 0, 0, 0)) // default transparent
      }

      ujson.Obj(
        "type" -> "ResizableLine",
        "id" -> line.getId,
        "start" -> List(line.getStartX, line.getStartY),
        "end" -> List(line.getEndX, line.getEndY),
        "strokeSize" -> line.getStrokeWidth,
        "fillColor" -> fillColor,
        "strokeColor" -> strokeColor
      )
    },
    // Deserialization
    json => json("type").str match {
      case "ResizableLine" =>
        val line = new ResizableLine(
          (json("start")(0).num, json("start")(1).num),
          (json("end")(0).num, json("end")(1).num),
          json("strokeSize").num.toInt
        )
        line.setId(json("id").str)

        val fillColor = read[SerializableColor](json("fillColor"))
        val strokeColor = read[SerializableColor](json("strokeColor"))

        line.setFillColor(fillColor)
        line.setStrokeColor(strokeColor)
        line
      case _ => throw new IllegalArgumentException("Unexpected shape type")
    }
  )
