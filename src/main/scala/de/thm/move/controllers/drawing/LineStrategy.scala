package de.thm.move.controllers.drawing

import de.thm.move.Roads.RoadType.Normal
import de.thm.move.Roads.{Road, RoadManager, RoadNode, RoadType}
import de.thm.move.controllers.ChangeDrawPanelLike
import de.thm.move.history.History
import de.thm.move.types.*
import de.thm.move.util.JFxUtils.withConsumedEvent
import de.thm.move.views.shapes.ResizableLine
import javafx.scene.input.MouseEvent
import de.thm.move.views.anchors.Anchor


import java.util.UUID

import de.thm.move.lotOperations.assignLotsToSplitRoad

import scala.language.postfixOps

class LineStrategy(
    changeLike: ChangeDrawPanelLike,
    roadTypeArg: Option[RoadType] = None
) extends PathLikeStrategy(changeLike) with assignLotsToSplitRoad:
  override type FigureType = ResizableLine
  override protected val tmpFigure = new ResizableLine((0, 0), (0, 0), 0)
  tmpFigure.setId(tmpShapeId)

  def setBounds(newX: Double, newY: Double): Unit =
    val (deltaX, deltaY) = (newX -> newY) - pointBuffer.head
    if drawConstraintProperty.get then
      val (startX, startY) = pointBuffer.head
      val (x, y) = if deltaX > deltaY then (newX, startY) else (startX, newY)
      tmpFigure.setEndX(x)
      tmpFigure.setEndY(y)
    else
      tmpFigure.setEndX(newX)
      tmpFigure.setEndY(newY)

  override def reset(): Unit =
    pointBuffer.clear()
    changeLike.remove(tmpFigure)
    resetLine(0, 0)

  private def resetLine(x: Double, y: Double): Unit =
    tmpFigure.setStartX(x)
    tmpFigure.setStartY(y)
    tmpFigure.setEndX(x)
    tmpFigure.setEndY(y)

  /** A function that splits a road in 2 by making 2 copies of the initial road and changing their attributes. This function is only called by handleIntersection.
    * @param roadLine
    *   The line of the road to be split in 2
    * @param newX
    *   x value of the point where the split will happen
    * @param newY
    *   y value of the point where the split will happen
   * @param Visual
   *    Boolean value that should be set to False if the function is used for testing, and should be set to True if the function is used for program code.
   *    If set to False some visual aspects that break the tests are disabled in the function.
    * @return
    *   Returns a pair of the newly splitted lines, so they can be checked afterwards for more intersections by handleIntersection.
    */
  def SplitRoad(
      roadLine: ResizableLine,
      newX: Double,
      newY: Double,
      Visual: Boolean
  ): (ResizableLine, ResizableLine) =
    require(roadLine != null, "roadLine cannot be null")
    require(newX >= 0 && newY >= 0, "Coordinates (newX, newY) must be non-negative")
    require(((newX > roadLine.getStartX && newX < roadLine.getEndX) || (newX < roadLine.getStartX && newX > roadLine.getEndX)), "newX must lie between the start and end X coordinates of the road")
    require(((newY > roadLine.getStartY && newY < roadLine.getEndY) || (newY < roadLine.getStartY && newY > roadLine.getEndY)), "newY must lie between the start and end Y coordinates of the road")

    val oldSize = ResizableLine.allLines.size // Save the initial number of lines

    val newRoadLine = roadLine.copy
    val newRoadLine2 = roadLine.copy
    val road: Option[Road] = RoadManager.getRoad(roadLine.getId)//road used to assign land lots to the new roads

    newRoadLine.setEndX(newX) // Set coordinates for the splitted roads
    newRoadLine.setEndY(newY)
    newRoadLine2.setStartX(newX)
    newRoadLine2.setStartY(newY)
    newRoadLine.setId(roadLine.getId)
    newRoadLine2.setId(UUID.randomUUID().toString)
    if Visual then changeLike.addShapeWithAnchors(newRoadLine) // Add new roads + lines
    ResizableLine.addLine(newRoadLine)
    if Visual then changeLike.addShapeWithAnchors(newRoadLine2)
    ResizableLine.addLine(newRoadLine2)

    val (start1, end1, start2, end2) = if Visual then {
      (
        RoadManager.getOrCreateNode(newRoadLine.getAnchors(0).getId),
        RoadManager.getOrCreateNode(newRoadLine.getAnchors(1).getId),
        RoadManager.getOrCreateNode(newRoadLine2.getAnchors(0).getId),
        RoadManager.getOrCreateNode(newRoadLine2.getAnchors(1).getId)
      )
    } else {
      (
        new RoadNode((newRoadLine.getId) + "-start"),
        new RoadNode((newRoadLine.getId) + "-end"),
        new RoadNode((newRoadLine2.getId) + "-start"),
        new RoadNode((newRoadLine2.getId) + "-end")
      )
    }
    val rType = RoadManager.getRoadProperties(newRoadLine.getId).roadType

    val r1 = Road(
      id = newRoadLine.getId,
      start = start1,
      end = end1,
      roadType = rType
    )
    val r2 = Road(
      id = newRoadLine2.getId,
      start = start2,
      end = end2,
      roadType = rType
    )
    if Visual then
      changeLike.removeShape(
        roadLine
      ) // Remove the old shape and line and make new ones for the 2 splits
    ResizableLine.removeLine(roadLine)
    assignLots(road.get.getLots, roadLine, newRoadLine, newRoadLine2, (newX, newY))//assign existing land lots to the new roads.
    //Console.println(r1.getLots)
    //Console.println(r2.getLots)
    RoadManager.addRoad(r1)
    RoadManager.addRoad(r2)
    (newRoadLine, newRoadLine2).ensuring(
      _ => ResizableLine.allLines.contains(newRoadLine) && ResizableLine.allLines.contains(newRoadLine2),
      "Postcondition failed: New road lines should be present in ResizableLine.allLines"
    )

  /** Checks if a line intersects with any other already-drawn line.
    * @param curr
    *   the line to be checked for intersections.
    * @return
    *   If there is no intersection, returns Left(false).
   *    If there is an intersection, returns Right(Px,Py,line), where Px and Py represent the x- and y- coordinates of the intersection point,
   *    and line represents the line that is intersecting with the input line.
   *    This is then passed on to handleIntersection so that SplitRoad can be called with these values.
    */
  def CheckIntersection(curr: ResizableLine): Either[Boolean, (Double, Double, ResizableLine)] = {
    val x1 = curr.getStartX
    val x2 = curr.getEndX
    val y1 = curr.getStartY
    val y2 = curr.getEndY

    ResizableLine.allLines.foreach {
      case line: ResizableLine if line.getId != curr.getId && line.getId != "temporary-shape" =>
        val x3 = line.getStartX
        val x4 = line.getEndX
        val y3 = line.getStartY
        val y4 = line.getEndY

        val D = ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4))
        if D == 0 then {
        } else {
          val Px =
            ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / D
          val Py =
            ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / D

          if (
              (Math.min(x1, x2) < Px && Px < Math.max(x1, x2) && Math.min(y1, y2) < Py && Py < Math.max(y1, y2)) &&
              (Math.min(x3, x4) < Px && Px < Math.max(x3, x4) && Math.min(y3, y4) < Py && Py < Math.max(y3, y4))
            )
          then {
            return Right(Px, Py, line)
            (Px >= 0, Py >= 0)
          }
        }
      case _ =>
    }
    return Left(false)
  }

  /** Function called every single time a new line is drawn, or by recursively calling itself. It uses CheckIntersection to see if any other line intersects with the input line. If there is, it calls
    * SplitRoad to split both lines in 2, and then recursively calls itself to check for more intersections. If there is no intersection, it does nothing.
    * @param curr
    *   The line to be checked for intersections
    */
  def handleIntersection(curr: ResizableLine, Visual: Boolean): Unit =
    require(curr != null, "The line being checked for intersections must not be null")
    CheckIntersection(curr) match {
      case Left(false) =>
      case Right((x: Double, y: Double, line: ResizableLine)) =>
        val (splitCurr1: ResizableLine, splitCurr2: ResizableLine) = SplitRoad(curr, x, y, Visual)
        val (splitLine1: ResizableLine, splitLine2: ResizableLine) = SplitRoad(line, x, y, Visual)
        handleIntersection(splitCurr1, Visual)
        handleIntersection(splitCurr2, Visual)
        handleIntersection(splitLine1, Visual)
        handleIntersection(splitLine2, Visual)
    }


  def dispatchEvent(mouseEvent: MouseEvent): Unit =
    mouseEvent.getEventType match
      case MouseEvent.MOUSE_PRESSED =>
        pointBuffer += (mouseEvent.getX -> mouseEvent.getY)
        resetLine(mouseEvent.getX, mouseEvent.getY)
        changeLike.addNode(tmpFigure)
      case MouseEvent.MOUSE_DRAGGED =>
        setBounds(mouseEvent.getX, mouseEvent.getY)
      case MouseEvent.MOUSE_RELEASED =>
        val roadLine = tmpFigure.copy
        roadLine.setId(System.currentTimeMillis().toString)
        changeLike.addShapeWithAnchors(roadLine)
        ResizableLine.addLine(roadLine)
        val road = Road(
          id = roadLine.getId,
          start = RoadManager.getOrCreateNode(roadLine.getAnchors(0).getId),
          end = RoadManager.getOrCreateNode(roadLine.getAnchors(1).getId),
          roadType = roadTypeArg.getOrElse(RoadType.Normal)
        )
        RoadManager.addRoad(road)
        handleIntersection(roadLine, true)
        reset()
      case _ => // ignore
