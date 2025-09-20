package de.thm.move.controllers.drawing
import javafx.collections.ObservableList

import scala.jdk.CollectionConverters.*
import javafx.scene.input.MouseEvent
import javafx.scene.paint.{Color, Paint}
import de.thm.move.controllers.ChangeDrawPanelLike
import de.thm.move.loader.parser.ast.PathElement
import de.thm.move.types.*
import de.thm.move.views
import de.thm.move.views.anchors.Anchor
import de.thm.move.views.shapes.{Line, ResizablePolygon, ResizableShape}
import de.thm.move.views.shapes.PathLike

import scala.collection.immutable
import scala.util.control.Breaks
import scala.util.matching.UnanchoredRegex
import javafx.scene.shape.{LineTo, MoveTo, Path, Shape}
import org.reactfx.Observable
import scala.jdk.CollectionConverters._
import java.util.function.Consumer

/**
 * Trait that implements the shrinking algorithm. Whenever two polygons overlap have to be changed. The old one
 * has to shrink; so this trait implements that operation.
 */
trait ShrinkStrategy:



  /**
   * Every anchor that gets added is connected to the previous one. This order is used to give back a list of edges.
   * @return List of anchors that represent edges of a polygon.
   * @param immutable.List[Anchor] a list of anchors from a polygon.
   */
  def getEdges(anchors: List[Anchor]): List[List[Anchor]] =
    var edges: List[List[Anchor]] = List()

    for (i <- anchors.indices)
      if (i == anchors.length - 1)
      then edges = edges.::(List(anchors(i), anchors.head))
      else
        edges = edges.::(List(anchors(i), anchors(i + 1)))
    edges


  /**
   * @param edges
   * List that represent the edges of a polygon.
   * @param point
   * Anchor for which the algorithm will determine if it is inside a polygon or not.
   * @return Boolean that indicates if the given point lies in the polygon.
   * This algorithm will loop over every existing polygon and check for each of its anchors if it is contained in the new polygon. This can be done
   * by conceptually drawing a horizontal line to the right for every anchor point. If it crosses through an edge of the polygon an odd number of times,
   * the point is contained inside the polygon. This algorithm is known as ray casting.
   *
   * 3 conditions have to be met for a ray to cross the edge of a polygon.
   *         1)The y coordinate of the point should be in between the y coordinates of the two points of an edge.
   *         2)The x coordinate of the point can not be higher than the highest x coordinate of the points of an edge.
   *         3)If the x coordinate is in the middle of the x coordinates of the points of the edge, then the ray will cross
   *           depending on the value of the y coordinate of the point.
   *           You have to check if the x coordinate of the point is not bigger than the highest value for x (call it x0) such that
   *           the point (x0, y) is the point that lies on the edge of the polygon. If the x coordinate of the point you want to check is higher
   *           than x0, the ray cast from that point does not cross the edge of the polygon. This last condition also takes care of the second one.
   */
  def pointInPolygon(edges: List[List[Anchor]], point: Anchor): Boolean =
    //variable that will count how many times the ray crosses an edge of the polygon.
    var count: Int = 0

    val x = point.getCenterX
    val y = point.getCenterY

    //Loop over every edge and check if the cast ray crosses this edge.
    for (edge <- edges)
      val x1 = edge.head.getCenterX
      val y1 = edge.head.getCenterY
      val x2 = edge.tail.head.getCenterX
      val y2 = edge.tail.head.getCenterY

      //x0 from condition 3.
      val x0 = x1 + ((y - y1) * (x2 - x1) / (y2 - y1))

      if (
        (y1 > y) != (y2 > y) && x < x0
        )
      then
        count += 1

    count % 2 == 1


  /**
   * @param polygons
   *  existing polygons
   * @param newPolygon
   *  polygon to be drawn
   * @return
   * the polygons the new one overlaps with
   * */
  //loops over all the existing polygons and checks if they overlap with the new one.
  def checkOverlappingPolygonsDeprecated(polygons: List[ResizablePolygon], newPolygon: ResizablePolygon): List[ResizablePolygon] =
    var overlappingPolygons: List[ResizablePolygon] = List()

    for (polygon <- polygons)
      val polygonAnchors = polygon.getAnchors
      if(polygonAnchors.forall((anchor: Anchor)=>
        pointInPolygon(getEdges(newPolygon.getAnchors), anchor)))
        then removeOldPolygon(polygon)
      else
        for (anchor <- polygonAnchors)
          val intersectionAnchors = newAnchorsFromIntersection(polygon, newPolygon)
          if (pointInPolygon(getEdges(newPolygon.getAnchors), anchor) ||
            (intersectionAnchors.nonEmpty && intersectionAnchors.length <= 2))
          then if (!overlappingPolygons.contains(polygon))
          then

            overlappingPolygons = overlappingPolygons.::(polygon)
    overlappingPolygons


  private def removeOldPolygon(polygon: ResizablePolygon): Unit =
    ResizablePolygon.unregisterPolygon(polygon)
  /**
   * @param polygon
   *  polygon to be drawn
   *
   * @return
   *  A boolean that reflects if the polygon has edges that intersect with each other.
   * */
  def checkOverlappingWithOwnEdges(polygon: ResizablePolygon): Boolean =
    val polygonEdges = getEdges(polygon.getAnchors)
    var intersects = false
    for(i <- polygonEdges.indices)
      if(!(i == polygonEdges.length - 1))
        then
          for(j <- i + 1 until polygonEdges.length)

            val anchor1 = polygonEdges(i).head
            val anchor2 = polygonEdges(i).tail.head
            val anchor3 = polygonEdges(j).head
            val anchor4 = polygonEdges(j).tail.head

            val line1 = new Line(anchor1.getCenterX, anchor1.getCenterY, anchor2.getCenterX, anchor2.getCenterY)
            val line2 = new Line(anchor3.getCenterX, anchor3.getCenterY, anchor4.getCenterX, anchor4.getCenterY)

            if(!intersects && !anchorsInCommon(anchor1, anchor2, anchor3, anchor4) && intersection(line1, line2).nonEmpty)
              then intersects = true

    intersects

  /**
   * @return
   * A boolean which indicates if there are common anchors.
   * */
  private def anchorsInCommon(anchor1: Anchor, anchor2: Anchor, anchor3: Anchor, anchor4: Anchor): Boolean =
    anchor1 == anchor2 || anchor1 == anchor3 || anchor1 == anchor4 || anchor2 == anchor3 || anchor2 == anchor4 || anchor3 == anchor4



  /**
   * @return
   * A list that will be empty if there is the two lines do not intersect, otherwise it will be of length two because it holds
   * the x and y coordinates of the intersection.
   * */
  def intersection(line1: Line, line2: Line): List[Double] =
    var intersectionPoint: List[Double] = List()

    //used an intersection method from linear algebra, but I still get a bug sometimes that I can almost never recreate.
    //https://mathworld.wolfram.com/Line-LineIntersection.html
    val intersect = intersectionLinearAlgebra(line1.x1, line1.y1, line1.x2, line1.y2, line2.x1, line2.y1, line2.x2, line2.y2)
    if (intersect.nonEmpty && validIntersection(line1, line2, intersect))
      then
        intersectionPoint = intersect

    intersectionPoint



  private def intersectionLinearAlgebra(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, x4: Double, y4: Double): List[Double] =


    def determinant2x2(a: Double, b: Double, c: Double, d: Double): Double =
      a * d - b * c


    var intersectionResult: List[Double] = List()
    val detLine1 = determinant2x2(x1, y1, x2, y2)
    val detLine2 = determinant2x2(x3, y3, x4, y4)
    val x1mx2 = x1 - x2
    val x3mx4 = x3 - x4
    val y1my2 = y1 - y2
    val y3my4 = y3 - y4

    val xNom = determinant2x2(detLine1, x1mx2, detLine2, x3mx4)
    val yNom = determinant2x2(detLine1, y1my2, detLine2, y3my4)
    val denominator = determinant2x2(x1mx2, y1my2, x3mx4, y3my4)

    if(!(denominator == 0))
      then intersectionResult = List(xNom/denominator, yNom/denominator)

    intersectionResult



  /**
   * @return
   * A boolean which is set to true the intersection is valid.
    An intersection is valid if the intersection is within the bounds of every anchor point. The equation for a line
    describes an infinite line, but in our case the edges are finite. This means the lines will intersect "out of bounds".
    These intersections do not count. This function determines if an intersection is valid.
   */

  private def validIntersection(line1: Line, line2: Line, intersection: List[Double]): Boolean =
    //Two points of an edge
    val x1 = line1.x1
    val y1 = line1.y1
    val x2 = line1.x2
    val y2 = line1.y2

    val x3 = line2.x1
    val y3 = line2.y1
    val x4 = line2.x2
    val y4 = line2.y2

    //intersection coordinates
    val x = intersection.head
    val y = intersection.tail.head


    ((x >= math.min(x1, x2) && x <= math.max(x1, x2)) && (x >= math.min(x3, x4) && (x <= math.max(x3, x4)))) &&
      ((y >= math.min(y1, y2) && y <= math.max(y1, y2)) && (y >= math.min(y3, y4) && (y <= math.max(y3, y4))))








  /**
   * @param oldPolygon
   * older polygon that overlaps with the polygon that is to be drawn.
   * @param newPolygon
   * Polygon that is to be drawn.
   *
   * @return
   * Unit. It constructs a new old polygon that has been shrunk. It does this by collecting all the necessary anchors changing its point list to the new anchors.
   * */
  def constructNewOldPolygon(oldPolygon: ResizablePolygon, newPolygon: ResizablePolygon): Unit =

    //anchors from new polygon present in old polygon. These are used in the new old polygon.
    val internalAnchors: List[Anchor] = newInOldPolygon(oldPolygon, newPolygon)

    //anchors form the old polygon that are not inside the new one. These have to remain.
    val externalAnchors: List[Anchor] = anchorsNotInPolygon(newPolygon, oldPolygon)

    //new edges connecting existing anchors to intersecting anchors.
    val newEdgesFromIntersections: List[List[Anchor]] = newEdgesFromIntersection(oldPolygon, newPolygon)



    val oldAnchorsWithIntersection: List[Anchor] = newEdgesFromIntersections.flatten///

    val oldRemainingEdges: List[List[Anchor]] = edgesRemaining(oldPolygon, externalAnchors, oldAnchorsWithIntersection)


    //construct edges from intersection anchors to internal anchors of the new polygon inside the old polygon.
    //first I get the intersection anchors, to afterward use these to construct edges between intersections and internal anchors.
    val intersectionAnchors: List[Anchor] = extractIntersection(newEdgesFromIntersections, getEdges(oldPolygon.getAnchors))
    val edgesFromInternalAnchors: List[List[Anchor]] = internalEdges(internalAnchors, intersectionAnchors)

    //next I have to create the ordered list (order of drawing) containing new anchors points for the new old polygon.
    //first I combine all the newly found edges.
    val edges = oldRemainingEdges.appendedAll(newEdgesFromIntersections.appendedAll(edgesFromInternalAnchors))

    //this function still doesn't work properly.
    val newAnchorList = createNewAnchorList(edges)

    val newPointList = newAnchorList.flatMap((anchor: Anchor) =>
      List(anchor.getCenterX, anchor.getCenterY)
    )


    //change the polygons point list
    val newPol = new ResizablePolygon(newPointList)
    newPol.copyColors(oldPolygon)
    newPol.setFill(oldPolygon.getFill)

    //register the newly constructed polygon and unregister old polygon that got shrunk.
    ResizablePolygon.registerPolygon(newPol)
    ResizablePolygon.unregisterPolygon(oldPolygon)
    newPol.toBack()
  


  //I chose the convention of always having the new anchor at the end of the edge list. This allows for easy extraction of new anchors = anchors from intersections.
  private def extractIntersection(newEdges: List[List[Anchor]], oldEdges: List[List[Anchor]]): List[Anchor] =
    var intersectionAnchors: List[Anchor] = List()
    for (edge <- newEdges)
      intersectionAnchors = intersectionAnchors.::(edge.tail.head)

    intersectionAnchors





  /**
   * @param newEdges
   * List of lists containing the new anchors for the polygon that has to be shrunk
   * @returns
   * A flat list of anchors that are used for the shrinking of the polygon.
   * */
  //Polygons expect a list that is ordered based on the order of points the user placed in the grid. This means I have to construct a
  //list of anchors in this order.
  private def createNewAnchorList(newEdges: List[List[Anchor]]): List[Anchor] =
    //ordered list of new anchors.
    var newAnchors: List[Anchor] = List()

    //choose a starting edge, from the tail of the edge, find another anchor from another edge and so on ...
    var currenEdgeIndex = -1
    var indexInEdge = -1

    var placeHolderEdges: List[List[Anchor]] = newEdges

    val firstAnchor = new Anchor(newEdges.head.head.getCenterX, newEdges.head.head.getCenterY)
    var lastAddedAnchor: Anchor = new Anchor(newEdges.head.tail.head.getCenterX, newEdges.head.tail.head.getCenterY)
    newAnchors = newAnchors.appendedAll(List(lastAddedAnchor, firstAnchor))
    placeHolderEdges = placeHolderEdges.splitAt(currenEdgeIndex)._1.appendedAll(placeHolderEdges.splitAt(currenEdgeIndex)._2.tail)


    for(i <- newEdges.indices)
      if(placeHolderEdges.nonEmpty)
      then
        val newAnchorIndices = calculateNextAnchorIndex(lastAddedAnchor, placeHolderEdges)
        currenEdgeIndex = newAnchorIndices.head
        indexInEdge = newAnchorIndices.tail.head
        val firstNewAnchor = placeHolderEdges(currenEdgeIndex)(indexInEdge)
        //add other anchor
        newAnchors = newAnchors.::(firstNewAnchor)
        lastAddedAnchor = firstNewAnchor
        placeHolderEdges = placeHolderEdges.splitAt(currenEdgeIndex)._1.appendedAll(placeHolderEdges.splitAt(currenEdgeIndex)._2.tail)

    newAnchors


  /**
   * @param anchor
   * Current anchor from the new edges list.
   * @param edges
   * New current list of new edges that have not yet been processed
   * @return
   * Index of the anchor that has been used in one edge and will now be used in the other edge. Every anchor has two outgoing
   * edges, So every anchor appears twice in the new edges list.
   * */
  private def calculateNextAnchorIndex(anchor: Anchor, edges: List[List[Anchor]]): List[Int] =
    var nextIndex: Int = -1
    var indexInEdge: Int = -1
    for(i <- edges.indices)
      val firstAnchor: Anchor = edges(i).head
      val secondAnchor: Anchor = edges(i).tail.head
      if(firstAnchor.getCenterX == anchor.getCenterX && firstAnchor.getCenterY == anchor.getCenterY)
      then
        nextIndex = i
        indexInEdge = 1
      else if(secondAnchor.getCenterX == anchor.getCenterX && secondAnchor.getCenterY == anchor.getCenterY)
      then
        nextIndex = i
        indexInEdge = 0
    List(nextIndex, indexInEdge)





  /**
   * @param internalAnchors
   * Anchors of the new polygon that are in the older one.
   * @param interSectionAnchors
   * Anchors that are present at the locations of the intersection of edges between the old and new polygon
   * @return
   * List of lists of anchors that represent edges. These edges go from one intersection and make a path from that point throughout the old polygon using the internal anchors. This
   * created a nice partition of the old polygon.
   *
   */
  private def internalEdges(internalAnchors: List[Anchor], interSectionAnchors: List[Anchor]): List[List[Anchor]] =
    var newEdges: List[List[Anchor]] = List()
    Console.println(interSectionAnchors)
    if(internalAnchors.nonEmpty)
    then
      //I can choose one of the intersection anchors to begin with. First I calculate the distance to every internal anchor, then I construct new edges based on these distances.
      var firstAnchor = interSectionAnchors.head
      var placeHolderList = internalAnchors
      for(i <- 0 to internalAnchors.length)
        //in This case there is only one internal anchor left => connect it to the other intersection anchor.
        if(i == internalAnchors.length)
        then
          val newAnchor = new Anchor(firstAnchor.getCenterX, firstAnchor.getCenterY)
          val newEdge = List(interSectionAnchors.tail.head, newAnchor)
          newEdges = newEdges.::(newEdge)
        else
          val indexOfMinimum = extractMinimumDistance(placeHolderList, firstAnchor)
          val newAnchor: Anchor = new Anchor(placeHolderList(indexOfMinimum).getCenterX, placeHolderList(indexOfMinimum).getCenterY)
          val newEdge = List(firstAnchor, newAnchor)
          newEdges = newEdges.::(newEdge)
          firstAnchor = newAnchor
          placeHolderList = placeHolderList.splitAt(indexOfMinimum)._1.appendedAll(placeHolderList.splitAt(indexOfMinimum)._2.tail)
    else
      newEdges = List(List(interSectionAnchors.head, interSectionAnchors.tail.head))


    newEdges


  /**
   * @param anchors
   * Internal anchors
   * @param intersection
   * Anchors from one intersection.
   * @return
   * Distance between the two points in the grid.
   * */
  private def extractMinimumDistance(anchors: List[Anchor], intersection: Anchor): Int =
    var minimumIndex = -1
    var minDistance = Double.MaxValue
    for(i <- anchors.indices)
      val distanceToAnchor = distance(anchors(i), intersection)
      if(distanceToAnchor < minDistance)
      then
        minimumIndex = i
        minDistance = distanceToAnchor
    minimumIndex





  /**
   * @param oldPolygon
   * Polygon to be shrunk.
   * @param anchors
   * External anchors of the old polygon. External means they are not contained in the new polygon.
   * @param intersectingAnchors
   * Anchors from the intersections.
   * @return
   * Edges from the old polygon that are not contained in the new one.
   * */
  private def edgesRemaining(oldPolygon: ResizablePolygon, anchors: List[Anchor], intersectingAnchors: List[Anchor]): List[List[Anchor]] =
    var remainingAnchors: List[List[Anchor]] = List()

    for(edge <- getEdges(oldPolygon.getAnchors))
      val anchor1 = edge.head
      val anchor2 = edge.tail.head
      //if the edge is contained in the list of anchors that are not inside the new polygon and none of the two anchors is used for an intersection anchor => found an edge that must remain.
      if((anchors.contains(anchor1) && anchors.contains(anchor2)) && !(intersectingAnchors.contains(anchor1) && intersectingAnchors.contains(anchor2)))
      then remainingAnchors = remainingAnchors.::(edge)

    remainingAnchors

  /**
   * @param oldPolygon
   * Polygon to be shrunk.
   * @param newPolygon
   * polygon to be drawn
   * @return
   * Edges constructed by connecting intersection point to the point on the edge that is not inside the new zone.
   * */
  private def newEdgesFromIntersection(oldPolygon: ResizablePolygon, newPolygon: ResizablePolygon): List[List[Anchor]] =
    var intersectionEdges: List[List[Anchor]] = List()
    val oldPolygonEdges = getEdges(oldPolygon.getAnchors)
    val newPolygonEdges = getEdges(newPolygon.getAnchors)

    for (newEdge <- newPolygonEdges)
      for (oldEdge <- oldPolygonEdges)

        val newLine = new Line(
          newEdge.head.getCenterX,
          newEdge.head.getCenterY,
          newEdge.tail.head.getCenterX,
          newEdge.tail.head.getCenterY
        )

        val oldLine = new Line(
          oldEdge.head.getCenterX,
          oldEdge.head.getCenterY,
          oldEdge.tail.head.getCenterX,
          oldEdge.tail.head.getCenterY
        )

        val newIntersection = intersection(newLine, oldLine)
        //if the intersection is valid, connect the intersection with the anchor that is not inside the new polygon, because this one will be deleted.
        if(newIntersection.nonEmpty)
        then
          val newAnchor = new Anchor(newIntersection.head, newIntersection.tail.head)
          if(pointInPolygon(newPolygonEdges, oldEdge.head))
          then
            val newEdge = List(oldEdge.tail.head, newAnchor)
            intersectionEdges = intersectionEdges.::(newEdge)
          else if(pointInPolygon(newPolygonEdges, oldEdge.tail.head))
          then
            val newEdge = List(oldEdge.head, newAnchor)
            intersectionEdges = intersectionEdges.::(newEdge)

          //if they're both not inside the polygon, connect the intersection to the anchor that is closest to it.
          //determine which anchor by calculating the distance between the intersection point and the anchors of the edge.
          else
            val distance1 = distance(oldEdge.head, newAnchor)
            val distance2 =  distance(oldEdge.tail.head, newAnchor)
            if(distance1 < distance2)
            then
              val newEdge = List(oldEdge.head, newAnchor)
              intersectionEdges = intersectionEdges.::(newEdge)
            else
              val newEdge = List(oldEdge.tail.head, newAnchor)
              intersectionEdges = intersectionEdges.::(newEdge)

    intersectionEdges



  /**
   * returns all the anchors from  the new polygon that are inside the old polygon
   * */
  private def newInOldPolygon(oldPolygon: ResizablePolygon, newPolygon: ResizablePolygon): List[Anchor] =
    var newOldAnchors: List[Anchor] = List()
    for (anchor <- newPolygon.getAnchors)
      if (pointInPolygon(getEdges(oldPolygon.getAnchors), anchor))
      then newOldAnchors = newOldAnchors.::(anchor)

    newOldAnchors

  /**
   * returns all the anchors of the old polygon that are not inside the new polygon.
   * */
  private def anchorsNotInPolygon(newPolygon: ResizablePolygon, oldPolygon: ResizablePolygon): List[Anchor] =
    var oldAnchors: List[Anchor] = List()
    for (anchor <- oldPolygon.getAnchors)
      if (!pointInPolygon(getEdges(newPolygon.getAnchors), anchor))
      then oldAnchors = oldAnchors.::(anchor)

    oldAnchors

  /**
   * Computes all the intersections between the edges of the old and new polygon and makes anchors of them.
   * */
  private def newAnchorsFromIntersection(oldPolygon: ResizablePolygon, newPolygon: ResizablePolygon): List[Anchor] =
    var newAnchors: List[Anchor] = List()

    val oldPolygonEdges = getEdges(oldPolygon.getAnchors)
    val newPolygonEdges = getEdges(newPolygon.getAnchors)

    for (newEdge <- newPolygonEdges)
      for (oldEdge <- oldPolygonEdges)

        val newLine = new Line(
          newEdge.head.getCenterX,
          newEdge.head.getCenterY,
          newEdge.tail.head.getCenterX,
          newEdge.tail.head.getCenterY
        )

        val oldLine = new Line(
          oldEdge.head.getCenterX,
          oldEdge.head.getCenterY,
          oldEdge.tail.head.getCenterX,
          oldEdge.tail.head.getCenterY
        )

        val intersectionAnchor = intersection(oldLine, newLine)

        if (intersectionAnchor.nonEmpty)
        then

          newAnchors = newAnchors.::(new Anchor(intersectionAnchor.head, intersectionAnchor.tail.head))
    newAnchors


  /**
   * Checks if there are more than four intersections between the edges of the old and new polygon. If this is the case the shrinking algorithm will
   * not calculate the two partitions of the old polygon.
   * */
  private def checkPolygonPartitioned(oldPolygon: ResizablePolygon, newPolygon: ResizablePolygon): Boolean =
    var amountOfIntersection: Int = 0

    val oldPolygonEdges = getEdges(oldPolygon.getAnchors)
    val newPolygonEdges = getEdges(newPolygon.getAnchors)

    for (newEdge <- newPolygonEdges)
      for (oldEdge <- oldPolygonEdges)
        val newLine = new Line(
          newEdge.head.getCenterX,
          newEdge.head.getCenterY,
          newEdge.tail.head.getCenterX,
          newEdge.tail.head.getCenterY
        )

        val oldLine = new Line(
          oldEdge.head.getCenterX,
          oldEdge.head.getCenterY,
          oldEdge.tail.head.getCenterX,
          oldEdge.tail.head.getCenterY
        )

        val possibleIntersection = intersection(newLine, oldLine)

        if(possibleIntersection.nonEmpty && validIntersection(newLine, oldLine, possibleIntersection))
        then amountOfIntersection += 1

    amountOfIntersection >= 4


  /**
   * Distance between two points in a grid.
   * */
  def distance(anchor1: Anchor, anchor2: Anchor): Double =
    val x1: Double = anchor1.getCenterX
    val y1: Double  = anchor1.getCenterY

    val x2: Double  = anchor2.getCenterX
    val y2: Double  = anchor2.getCenterY

    Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2))





