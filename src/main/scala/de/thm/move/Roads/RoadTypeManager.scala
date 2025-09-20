package de.thm.move.Roads

import de.thm.move.models.SelectedShape
import de.thm.move.models.SelectedShape.SelectedShape
import javafx.scene.paint.{Color, Paint}

/** Manages the various road types. Primarily meant to save the properties of a
  * certain road type so they can be easily accessed
  */
object RoadTypeManager {

  /** @return
    *   Returns a list: each element contains a shape, the equivalent road type,
    *   its fill color, its stroke color and its stroke width.
    */
  val allRoadTypes: List[(SelectedShape, RoadType, Color, Color, Int)] = List(
    (SelectedShape.RoadNormal, RoadType.Normal, Color.BLACK, Color.BLACK, 4),
    (SelectedShape.RoadDouble, RoadType.Double, Color.BLUE, Color.BLUE, 4),
    (SelectedShape.RoadUnpaved, RoadType.UnPaved, Color.BROWN, Color.BROWN, 4)
  )

  /** @param shape
    *   A Shape for which we want to retrieve its equivalent road type value.
    * @return
    *   The road type linked to that shape
    * @throws Exception
    *   throw an exception if the given road type is not in allRoadTypes.
    */
  def getRoadTypeForShape(shape: SelectedShape): RoadType = {
    allRoadTypes.collectFirst {
      case (s, t, _, _, _) if s == shape => t
    } getOrElse {
      throw new NoSuchElementException(
        "No road type found for given shape"
      )
    }
  }

  /** @param roadType
    *   A road type for which we want to retrieve its equivalent shape.
    * @return
    *   The shape linked to that road type
    * @throws Exception
    *   throw an exception if the given shape is not in allRoadTypes.
    */
  def getShapeForRoadType(roadType: RoadType): SelectedShape = {
    allRoadTypes.collectFirst {
      case (s, t, _, _, _) if t == roadType => s
    } getOrElse {
      throw new NoSuchElementException(
        "No shape found for given road type"
      )
    }
  }

  /** @param road:
    *   The road type of which we want the properties.
    * @return
    *   A list of the fill color, stroke color and stroke width of that road
    *   type.
    */
  def roadTypeProperties(road: RoadType): List[(Paint, Paint, Int)] = {
    // Use filter to find matching road types and convert them into a List of properties
    allRoadTypes.collect {
      case (shape, roadType, fillColor, strokeColor, thickness)
          if roadType == road =>
        (fillColor, strokeColor, thickness)
    }
  }
}
