package de.thm.move.views.shapes

import javafx.beans.property.{ObjectProperty, SimpleObjectProperty}
import javafx.scene.paint.{Color, Paint}
import javafx.scene.shape.Shape
import de.thm.move.models.{FillPattern, LinePattern}
import de.thm.move.util.JFxUtils.*
import ujson.Obj
import upickle.default.*

import scala.language.implicitConversions

// Serializable color representation
case class SerializableColor(r: Double, g: Double, b: Double, a: Double)

object SerializableColor {
  implicit val colorRW: ReadWriter[SerializableColor] = macroRW

  // Convert Color to SerializableColor
  implicit def colorToSerializableColor(color: Color): SerializableColor =
    SerializableColor(color.getRed, color.getGreen, color.getBlue, color.getOpacity)

  // Convert SerializableColor back to Color
  implicit def serializableColorToColor(sColor: SerializableColor): Color =
    new Color(sColor.r, sColor.g, sColor.b, sColor.a)
}

/** A colorizable shape. */
trait ColorizableShape:
  self: Shape =>

  /** Pattern of the stroke */
  val linePattern: ObjectProperty[LinePattern.Value] =
    new SimpleObjectProperty(LinePattern.Solid)

  /** Pattern of the fill */
  val fillPatternProperty: ObjectProperty[FillPattern.Value] =
    new SimpleObjectProperty(FillPattern.Solid)

  /** The old/actual Color fill. */
  val oldFillColorProperty: ObjectProperty[Color] =
    new SimpleObjectProperty(null) // null = transparent

  /** Copies the style from other to this element */
  def copyColors(other: ColorizableShape): Unit =
    setFillColor(other.getFillColor)
    setStrokeColor(other.getStrokeColor)
    this.setStrokeWidth(other.getStrokeWidth)
    copyProperty(fillPatternProperty, other.fillPatternProperty)
    copyProperty(oldFillColorProperty, other.oldFillColorProperty)
    copyProperty(linePattern, other.linePattern)
    LinePattern.linePatternToCssClass.get(
      linePattern.get
    ) foreach getStyleClass.add

  /** Sets the fill and stroke color of this shape */
  def colorizeShape(fillColor: Paint, strokeColor: Paint): Unit =
    self.setFillColor(fillColor)
    self.setStrokeColor(strokeColor)

  def getStrokeWidth: Double
  def getFillColor: Paint = self.getFill
  def getStrokeColor: Paint = self.getStroke

  def setStrokeWidth(width: Double): Unit
  def setFillColor(c: Paint): Unit =
    self.setFill(c)
    c match
      case color: Color => oldFillColorProperty.set(color)
      case _            => // ignore
  def setStrokeColor(c: Paint): Unit =
    self.setStroke(c)

object ColorizableShape {

  /**
   * Implicit ReadWriter for `LinePattern` enum, handling its conversion to/from a string for serialization.
   *
   * LinePattern is serialized as a string representation (e.g., "Solid"), and deserialized
   * by matching the string with the corresponding `LinePattern.Value`.
   */
  implicit val linePatternRW: ReadWriter[LinePattern.Value] =
    readwriter[String].bimap[LinePattern.Value](
      _.toString, // Convert `LinePattern.Value` to a string for serialization
      LinePattern.withName // Convert a string back to `LinePattern.Value` during deserialization
    )

  /**
   * Implicit ReadWriter for `FillPattern` enum, handling its conversion to/from a string for serialization.
   *
   * Similar to `LinePattern`, `FillPattern` is serialized as a string, enabling serialization
   * to store the enum’s name and deserialization to reconstruct it from that name.
   */
  implicit val fillPatternRW: ReadWriter[FillPattern.Value] =
    readwriter[String].bimap[FillPattern.Value](
      _.toString, // Convert `FillPattern.Value` to a string
      FillPattern.withName // Convert a string back to `FillPattern.Value`
    )

  /**
   * Implicit ReadWriter for `ColorizableShape`, managing serialization and deserialization of shape properties.
   *
   * This ReadWriter handles:
   * - `fillColor`: Converts `Paint` to `SerializableColor` if it's of type `Color`, otherwise defaults to transparent color.
   * - `strokeColor`: Similar to `fillColor`, converts to `SerializableColor` or defaults to transparent.
   * - `strokeWidth`: Serialized as a numeric value.
   * - `linePattern` and `fillPattern`: Serialized using their respective ReadWriters (above).
   *
   * On deserialization, this ReadWriter reconstructs a new `ColorizableShape` instance
   * with the saved fill/stroke colors, patterns, and stroke width.
   */
  implicit val colorizableShapeRW: ReadWriter[ColorizableShape] = readwriter[ujson.Obj].bimap[ColorizableShape](
    shape => {
      // Serialize `ColorizableShape` fields
      val fillColor = shape.getFillColor match {
        case color: Color => writeJs(SerializableColor.colorToSerializableColor(color))
        case _ => writeJs(SerializableColor(0, 0, 0, 0)) // default transparent
      }
      val strokeColor = shape.getStrokeColor match {
        case color: Color => writeJs(SerializableColor.colorToSerializableColor(color))
        case _ => writeJs(SerializableColor(0, 0, 0, 0)) // default transparent
      }
      Obj(
        "fillColor" -> fillColor,
        "strokeColor" -> strokeColor,
        "strokeWidth" -> ujson.Num(shape.getStrokeWidth),
        "linePattern" -> writeJs(shape.linePattern.get), // Correct reference to `linePattern`
        "fillPattern" -> writeJs(shape.fillPatternProperty.get) // Correct reference to `fillPattern`
      )
    },
    json => {
      // Deserialize and create a new `ColorizableShape` instance
      val fillColor = read[SerializableColor](json("fillColor"))
      val strokeColor = read[SerializableColor](json("strokeColor"))
      val strokeWidth = json("strokeWidth").num
      val lineptrn = read[LinePattern.Value](json("linePattern"))
      val fillPattern = read[FillPattern.Value](json("fillPattern"))

      // Instantiate a concrete shape, such as a `Rectangle`, and mix in `ColorizableShape`
      new javafx.scene.shape.Rectangle() with ColorizableShape {
        setFillColor(fillColor)
        setStrokeColor(strokeColor)
        setStrokeWidth(strokeWidth)
        this.linePattern.set(lineptrn) // Correctly set the `linePattern`
        this.fillPatternProperty.set(fillPattern) // Correctly set the `fillPattern`
      }
    }
  )
}