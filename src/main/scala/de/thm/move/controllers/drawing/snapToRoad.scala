package de.thm.move.controllers.drawing

import de.thm.move.views.anchors.Anchor
import de.thm.move.views.shapes.{Line, ResizableLine, ResizablePolygon}
import de.thm.move.Roads.{Road, RoadManager, RoadNode}
import de.thm.move.Roads.RoadType.{Normal, UnPaved}



trait snapToRoad extends ShrinkStrategy:


  /**Checks for all the anchors of the land lot if the anchors are close enough the road and snaps them if this is the case.
   * @param lot
   *    Land lot that will be potentially snapped to the road.
   * @param roadLine
   *    Line of the road that is used in the calculations.
   * @return
   *    Boolean
   * */
  def closeEnough(lot: ResizablePolygon, roadLine: ResizableLine): Boolean =
    val threshold = 50
    var snappedAnchors: Int = 0
    val beginLine = roadLine.getAnchors.head
    val endLine = roadLine.getAnchors.tail.head

    val x1 = beginLine.getCenterX
    val y1 = beginLine.getCenterY

    val x2 = endLine.getCenterX
    val y2 = endLine.getCenterY

    val line = new Line(x1, y1, x2, y2)


    lot.getAnchors.foreach((anchor: Anchor) =>
      val anchorX = anchor.getCenterX
      val anchorY = anchor.getCenterY


      //distance of a point to a line when the two end points of the line are known
      val distanceToRoad = Math.abs((y2 - y1) * anchorX - (x2 - x1) * anchorY + x2*y1 - y2*x1)/Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2))


      //if the anchor is between the two ends of the line and the y coordinates are very close to each other => snap it
      if(distanceToRoad <= threshold)
        then
          snapToRoad(x1, y1, x2, y2, anchorX, anchorY, distanceToRoad, anchor, lot)
          snappedAnchors += 1
    )

    snappedAnchors == 2 || snappedAnchors == 4

  /** Snaps an anchor to a road.
   *  @param x1
    *   X coordinate of first point of road
    * @param y1
    *   Y coordinate of first point of road
    * @param x2
    *   X coordinate of second point of road
   * @param y2
   *    Y coordinate of second point of road
   * @param pointX
   *    X coordinate of anchor to be snapped
   * @param pointY
   *    Y coordinate of anchor to be snapped
   * @param distanceToLine
   *    Distance of anchor to road
   * @param anchor
   *    Anchor which will be snapped
   * @param lot
   *    Lot that will be changed.
    * @return
    *   Unit
  */
  def snapToRoad(x1: Double, y1: Double, x2: Double, y2: Double, pointX: Double, pointY: Double, distanceToLine: Double, anchor: Anchor, lot: ResizablePolygon): Unit =

    var pointWithLowestY: (Double, Double) = (0.0, 0.0)


    //first check if the line is very close to being vertical, if this is the case go with y and x + distance val of the point.
    if(Math.abs(x1 - x2) < 10.0)
      then
        val snappedPoint = (x1, pointY)
        anchor.setCenterX(snappedPoint._1)
        anchor.setCenterY(snappedPoint._2)

        // Find and update the corresponding points in the polygon
        val pointIndex = lot.getAnchors.indexOf(anchor)
        if pointIndex >= 0 then
          val polyPointIndex = pointIndex * 2
          lot.getPoints.set(polyPointIndex, snappedPoint._1)
          lot.getPoints.set(polyPointIndex + 1, snappedPoint._2)
        anchor.autosize()

    else
      //pick point that will be used to be the beginning of the horizontal line that will be rotated by the angle of the road to get the correct snapping point.
      if(x1 < x2)
        then pointWithLowestY = (x1, y1)
      else
        pointWithLowestY = (x2, y2)

      //get this length to use pythagoras to determine the length of the lowest point to where the snapped point on this line is.
      val distanceToLowestPoint = Math.sqrt(Math.pow(pointWithLowestY._1 - pointX, 2) + Math.pow(pointWithLowestY._2 - pointY, 2))
      //pythagoras theorem;
      val angledLineLength = Math.sqrt(Math.pow(distanceToLowestPoint, 2) - Math.pow(distanceToLine, 2))


      val slopeOfRoad: Double = (y2 - y1) / (x2 - x1)

      //use this to rotate line starting in the lowest point, until point at x + angledLineLength.
      val angleOfLineToXAxis = Math.atan(slopeOfRoad)

      //apply rotation of that line, by doing this the end of the line will have to x and y coordinates of the point you want to use as a snapping point on the line.
      val snappedPoint = (pointWithLowestY._1 + angledLineLength * Math.cos(angleOfLineToXAxis), pointWithLowestY._2 + angledLineLength * Math.sin(angleOfLineToXAxis))



      anchor.setCenterX(snappedPoint._1)
      anchor.setCenterY(snappedPoint._2)

      val pointIndex = lot.getAnchors.indexOf(anchor)
      if pointIndex >= 0 then
        val polyPointIndex = pointIndex * 2
        lot.getPoints.set(polyPointIndex, snappedPoint._1)
        lot.getPoints.set(polyPointIndex + 1, snappedPoint._2)

      anchor.autosize()


  /**Chooses two anchors to snap. Only anchors for which their edge does not cross the road can be chosen.
   * @param anchors
   *    anchors that can be snapped.
   * @param road
   *    road on which the anchors can be snapped
   *
   * @return
   *    List of length two that contains the indices of the anchors that can be snapped to the road.
  * */
  private def chooseTwoAnchors(anchors: List[Anchor], road: Line): List[Int] =
    var indices: List[Int] = List()
    var found: Boolean = false

      /*
      check all option of edges, find the non-crossing one.
      */
    for(i <- anchors.indices)
      for(j <- i + 1 until anchors.length - 1)
        if(!found && !edgeCrossesRoad(anchors(i), anchors(j), road))
          then
            indices = List(i, j)
            found = true
    indices


  /**Checks if an edge between two anchors crosses a road
  * @param anchor1
   *    First anchor of the pair of anchors that may be snapped.
   * @param anchor2
   *    Second anchor of the pair of anchors that may be snapped.
  * @return Boolean
  * */
  private def edgeCrossesRoad(anchor1: Anchor, anchor2: Anchor, road: Line): Boolean =
    val anchorEdge = new Line(anchor1.getCenterX, anchor1.getCenterY, anchor2.getCenterX, anchor2.getCenterY)
    Console.println("crosses")
    intersection(anchorEdge, road).nonEmpty


  /**Checks for each road if the lot can be snapped to it. Once a lot gets snapped to a road it can not be snapped to another one.
   * @param lot
   *    Lot to be snapped.
   * @param roads
   *    List of roads that is used to check if the lot can be snapped to one of them.
  * @return (Boolean, road)
  * */
  def checkCloseEnough(lot: ResizablePolygon, roads: List[ResizableLine]): (Boolean, Road) =
    var closeEnoughAlready: Boolean = false
    var newAnchors: List[Anchor] = List()
    var result: (Boolean, Road) = (false, new Road("null", new RoadNode("null1"), new RoadNode("null2"), Normal))
    for(roadLine <- roads)
      val snapped = closeEnough(lot, roadLine)
      if(!(closeEnoughAlready) && snapped)
          then
            closeEnoughAlready = true
            val road = RoadManager.getRoad(roadLine.getId).get
            result = (true, road)
    result
