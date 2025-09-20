/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.models

import java.nio.file.{Files, Path}
import java.util.Locale
import javafx.scene.Node
import javafx.scene.paint._
import javafx.scene.shape.{LineTo, MoveTo, QuadCurveTo}

import de.thm.move.Global._
import de.thm.move.util.{GeometryUtils, ResourceUtils}
import de.thm.move.util.GeometryUtils._
import de.thm.move.views.shapes._

import scala.jdk.CollectionConverters._
import scala.xml.{Elem, Null, PrettyPrinter, UnprefixedAttribute}

/** Codegenerator for SVG-Images
  *
  * @note
  *   Useful informations about SVG can be found here:
  *   - [[https://www.w3.org/TR/SVG/ W3C Specification]]
  *   - [[http://www.w3schools.com/svg/default.asp W3CSchools]]
  */
class SvgCodeGenerator:

  val lineWidth = 100 // maximum line-width of generated SVG-XML
  val indentation = 2 // indentation of generated SVG-XML

  /** Converts the given color into a css-style color (e.g. rgb(255,0,0)) */
  def colorToCssColor(p: Paint): String = p match
    case c: Color =>
      val red = (c.getRed * 255).toInt
      val green = (c.getGreen * 255).toInt
      val blue = (c.getBlue * 255).toInt
      s"rgb($red,$green,$blue)"
    case null => "white"
    case _ =>
      throw new IllegalArgumentException(s"Can't generate color for: $p")

  private def shapesWithIds(shapes: List[Node]) = shapes.zipWithIndex.map {
    case (shape, idx) => (shape, idx.toString)
  }

  /** Generates an SVG Image from the given shapes with the given width & height
    */
  def generateShapes(
      shapes: List[Node],
      width: Double,
      height: Double
  ): Elem =
    <svg
      width={width.toString}
      height={height.toString}
      xmlns="http://www.w3.org/2000/svg"
      xmlns:xlink="http://www.w3.org/1999/xlink"
      >
      <defs>
        {
      shapesWithIds(shapes).flatMap {
        case (shape: ColorizableShape, id) => generateFillPattern(shape, id)
        case _                             => None
      }
    }
      </defs>
      {
      shapesWithIds(shapes).map { case (shape, id) =>
        generateShape(shape, id)
      }
    }
    </svg>

  def generateShape(shape: Node, id: String): Elem = shape match
    case rect: ResizableRectangle        => genRectangle(rect, id)
    case ellipse: ResizableCircle        => genCircle(ellipse, id)
    case line: ResizableLine             => genLine(line)
    case polygon: ResizablePolygon       => genPolygon(polygon, id)
    case path: ResizablePath             => genPath(path, id)
    case img: ResizableImage             => genImage(img)
    case curvedPolygon: QuadCurvePolygon => genCurvedPolygon(curvedPolygon, id)
    case curvedPath: QuadCurvePath       => genCurvedPath(curvedPath, id)
    case text: ResizableText             => genText(text)
    case _ =>
      throw new IllegalArgumentException(s"Can't generate svg code for: $shape")

  /** Generates an SVG with pretty-printed XML.
    *
    * @see
    *   [[de.thm.move.models.SvgCodeGenerator#generateShapes]]
    */
  def generatePrettyPrinted(
      shapes: List[Node],
      width: Double,
      height: Double
  ): String =
    val xml = generateShapes(shapes, width, height)
    val printer = new PrettyPrinter(lineWidth, indentation)
    printer.format(xml)

  private def genRectangle(rectangle: ResizableRectangle, id: String): Elem =
    <rect
      x={rectangle.getX.toString}
      y={rectangle.getY.toString}
      width={rectangle.getWidth.toString}
      height={rectangle.getHeight.toString}
      style={genColorStyle(rectangle)}
      stroke-dasharray = {rectangle.getStrokeDashArray.asScala.mkString(",")}
      /> %
      fillAttribute(rectangle, id) %
      fillOpacityAttribute(rectangle, id) %
      transformationAttribute(rectangle)

  private def genCircle(ellipse: ResizableCircle, id: String): Elem =
    <ellipse
      cx={ellipse.getCenterX.toString}
      cy={ellipse.getCenterY.toString}
      rx={asRadius(ellipse.getWidth).toString}
      ry={asRadius(ellipse.getHeight).toString}
      style={genColorStyle(ellipse)}
      stroke-dasharray = {ellipse.getStrokeDashArray.asScala.mkString(",")}
      /> %
      fillAttribute(ellipse, id) %
      fillOpacityAttribute(ellipse, id) %
      transformationAttribute(ellipse)

  private def genLine(line: ResizableLine): Elem =
    <line
      x1={line.getStartX.toString}
      y1={line.getStartY.toString}
      x2={line.getEndX.toString}
      y2={line.getEndY.toString}
      style={genColorStyle(line)}
      stroke-dasharray = {line.getStrokeDashArray.asScala.mkString(",")}
      /> %
      transformationAttribute(line)

  private def genPolygon(polygon: ResizablePolygon, id: String): Elem =
    <polygon
      points={
      polygon.getPoints.asScala.map(_.toInt).mkString(",")
    }
      style={genColorStyle(polygon)}
      stroke-dasharray = {polygon.getStrokeDashArray.asScala.mkString(",")}
      /> %
      fillAttribute(polygon, id) %
      fillOpacityAttribute(polygon, id)

  private def genPath(path: ResizablePath, id: String): Elem =
    <polyline
      points={
      path.getPoints
        .flatMap { case (x, y) =>
          List(x, y)
        }
        .mkString(",")
    }
      style={genColorStyle(path)}
      stroke-dasharray = {path.getStrokeDashArray.asScala.mkString(",")}
      fill = "none"
      /> %
      transformationAttribute(path)

  /** Generates a curved-like element.
    *
    * @note
    *   Informations to the SVG-Path element:
    *   - [[http://www.w3schools.com/svg/svg_path.asp W3Schools-Path]]
    *   - [[https://www.w3.org/TR/SVG/paths.html#PathData W3CSpecification-PathData]]
    */
  private def genCurveLike(pathlike: AbstractQuadCurveShape): Elem =
    <path
      d={
      pathlike.getElements.asScala
        .map {
          case move: MoveTo => s"M ${move.getX} ${move.getY}"
          case line: LineTo => s"L ${line.getX} ${line.getY}"
          case curved: QuadCurveTo =>
            s"Q ${curved.getControlX} ${curved.getControlY} ${curved.getX} ${curved.getY}"
        }
        .mkString(" ")
    }
      style={genColorStyle(pathlike)}
      stroke-dasharray = {pathlike.getStrokeDashArray.asScala.mkString(",")}
      /> %
      transformationAttribute(pathlike)

  private def genCurvedPath(curvedPath: QuadCurvePath, id: String): Elem =
    // generate a curved path & remove default fill
    genCurveLike(curvedPath) % new UnprefixedAttribute("fill", "none", Null)

  private def genCurvedPolygon(
      curvedPolygon: QuadCurvePolygon,
      id: String
  ): Elem =
    genCurveLike(curvedPolygon) %
      fillAttribute(curvedPolygon, id) %
      fillOpacityAttribute(curvedPolygon, id) %
      transformationAttribute(curvedPolygon)

  private def genImage(img: ResizableImage): Elem =
    <image
      x={img.getX.toString}
      y={img.getY.toString}
      width={img.getWidth.toString}
      height={img.getHeight.toString}
      xlink:href={
      img.srcEither match
        case Left(uri) =>
          uri.toString
        case Right(bytes) =>
          val byteStr = ResourceUtils.encodeBase64String(bytes)
          s"data:image/png;base64,$byteStr"
    }
      /> %
      transformationAttribute(img)

  private def genText(text: ResizableText): Elem =
    <text
      x={text.getX.toString}
      y={text.getY.toString}
      fill={colorToCssColor(text.getFontColor)}
      style={
      List(
        s"font-family: ${text.getFont.getFamily}",
        s"font-size: ${text.getSize.toInt}pt",
        s"font-weight: ${if text.getBold then "bold" else "normal"}",
        s"font-style: ${if text.getBold then "italic" else "normal"}",
        s"text-decoration: ${if text.isUnderline then "underline" else "none"}"
      ).mkString(";")
    }
      >
      {text.getText}
    </text> % transformationAttribute(text)

  private def fillAttribute(shape: ColorizableShape, id: String) =
    val fill = shape.fillPatternProperty.get match
      case FillPattern.None  => "white"
      case FillPattern.Solid => colorToCssColor(shape.oldFillColorProperty.get)
      case _                 => s"url(#$id)"
    new UnprefixedAttribute("fill", fill, Null)

  private def fillOpacityAttribute(shape: ColorizableShape, id: String) =
    val opacity = shape.fillPatternProperty.get match
      case FillPattern.None => "0.0"
      case _ =>
        "%.2f".formatLocal(Locale.US, shape.oldFillColorProperty.get.getOpacity)
    new UnprefixedAttribute("fill-opacity", opacity, Null)

  private def transformationAttribute(node: Node) =
    new UnprefixedAttribute(
      "transform",
      generateRotation(node).getOrElse(""),
      Null
    )

  private def generateRotation(node: Node): Option[String] =
    val rotation = node.getRotate
    if rotation == 0 | rotation == 360 then None
    else
      val degree = rotation.toInt
      val bounds = node.getBoundsInLocal
      val (x, y) = GeometryUtils.middleOfLine(
        bounds.getMinX,
        bounds.getMinY,
        bounds.getMaxX,
        bounds.getMaxY
      )
      Some(s"rotate($degree $x $y)")

  /** Creates a line that's used inside structures/patterns */
  private def structureLine(
      x1: Double,
      y1: Double,
      x2: Double,
      y2: Double,
      lineColor: Paint
  ): Elem =
    <line
      x1={x1.toString}
      x2={x2.toString}
      y1={y1.toString}
      y2={y2.toString}
      style={
      List(s"stroke:${colorToCssColor(lineColor)}", s"stroke-width: 1")
        .mkString(";")
    }
      />

  val lineDistance = config
    .getInt("structure-distance")
    .getOrElse(5) // distance between lines/cells

  private def horizontalLines(
      width: Double,
      height: Double,
      lineColor: Paint
  ): Seq[Elem] =
    for
      i <- 1 to (height / lineDistance).toInt
      y = (i * lineDistance)
      x = 0
      endX = width
    yield structureLine(x, y, endX, y, lineColor)

  private def verticalLines(
      width: Double,
      height: Double,
      lineColor: Paint
  ): Seq[Elem] =
    for
      i <- 1 to (width / lineDistance).toInt
      x = (i * lineDistance)
      y = 0
      endY = height
    yield structureLine(x, y, x, endY, lineColor)

  private def forwardLines(
      width: Double,
      height: Double,
      lineColor: Paint
  ): Seq[Elem] =
    val max = width max height
    val doubled = max * 2
    var endX = 0 // distance to last line
    // create lines from bottom-left to middle
    val firstHalf = for i <- (doubled / lineDistance).toInt to 0 by -1 yield
      val startX = 0
      val startY = i * 5
      val endY = doubled
      val line =
        structureLine(startX, startY, endX, endY, lineColor)
      endX += 5 // 5px between this and the next line
      line
    // create lines from middle to top-right
    val secondHalf = for i <- 1 to (doubled / lineDistance).toInt yield
      val startX = i * lineDistance
      val startY = 0
      val endY = doubled
      val line =
        structureLine(startX, startY, endX, endY, lineColor)
      endX += lineDistance // 5px between this and the next line
      line
    firstHalf ++ secondHalf

  private def backwardLines(
      width: Double,
      height: Double,
      lineColor: Paint
  ): Seq[Elem] =
    val max = width max height
    // create lines going from top-left to bottom-right
    for
      i <- 1 to ((max * 2) / lineDistance).toInt
      x = i * lineDistance
      y = i * lineDistance
    yield structureLine(0, y, x, 0, lineColor)

  private def generateFillPattern(
      shape: Node with ColorizableShape,
      id: String
  ): Option[Elem] =
    def generateStructurePattern(
        xs: Seq[Elem],
        width: Double,
        height: Double
    ): Elem =
      <pattern id={id} patternUnits="userSpaceOnUse" width={
        width.toString
      } height={height.toString}>
        <rect
          x={0.toString}
          y={0.toString}
          width={width.toString}
          height={height.toString}
          fill={colorToCssColor(shape.oldFillColorProperty.get)}
          style="stroke:none;"
          />
          {xs}
      </pattern>

    val bounds = shape.getBoundsInLocal()
    val width = bounds.getWidth
    val height = bounds.getHeight

    shape.fillPatternProperty.get match
      case FillPattern.VerticalCylinder =>
        Some(<linearGradient id={id.toString} x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" style={
          s"stop-color:${colorToCssColor(shape.getStrokeColor)};stop-opacity:1"
        }/>
          <stop offset="45%" style={
          s"stop-color:${colorToCssColor(shape.oldFillColorProperty.get)};stop-opacity:1"
        }/>
          <stop offset="55%" style={
          s"stop-color:${colorToCssColor(shape.oldFillColorProperty.get)};stop-opacity:1"
        }/>
          <stop offset="100%" style={
          s"stop-color:${colorToCssColor(shape.getStrokeColor)};stop-opacity:1"
        }/>
        </linearGradient>)
      case FillPattern.HorizontalCylinder =>
        Some(<linearGradient id={id.toString} x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" style={
          s"stop-color:${colorToCssColor(shape.getStrokeColor)};stop-opacity:1"
        }/>
          <stop offset="45%" style={
          s"stop-color:${colorToCssColor(shape.oldFillColorProperty.get)};stop-opacity:1"
        }/>
          <stop offset="55%" style={
          s"stop-color:${colorToCssColor(shape.oldFillColorProperty.get)};stop-opacity:1"
        }/>
          <stop offset="100%" style={
          s"stop-color:${colorToCssColor(shape.getStrokeColor)};stop-opacity:1"
        }/>
        </linearGradient>)
      case FillPattern.Sphere =>
        Some(<radialGradient id={
          id.toString
        } cx="50%" cy="50%" r="50%" fx="50%" fy="50%">
          <stop offset="0%" style={
          s"stop-color:${colorToCssColor(shape.oldFillColorProperty.get)}; stop-opacity:1"
        } />
          <stop offset="20%" style={
          s"stop-color:${colorToCssColor(shape.oldFillColorProperty.get)}; stop-opacity:1"
        } />
          <stop offset="100%" style={
          s"stop-color:${colorToCssColor(shape.getStrokeColor)};stop-opacity:1"
        } />
        </radialGradient>)
      case FillPattern.Horizontal =>
        val lines = horizontalLines(width, height, shape.getStrokeColor)
        Some(generateStructurePattern(lines, width, height))
      case FillPattern.Vertical =>
        val lines = verticalLines(width, height, shape.getStrokeColor)
        Some(generateStructurePattern(lines, width, height))
      case FillPattern.Cross =>
        val verticals = verticalLines(width, height, shape.getStrokeColor)
        val horizontals = horizontalLines(width, height, shape.getStrokeColor)
        val lines = verticals ++ horizontals
        Some(generateStructurePattern(lines, width, height))
      case FillPattern.Backward =>
        val lines = backwardLines(width, height, shape.getStrokeColor)
        Some(generateStructurePattern(lines, width, height))
      case FillPattern.Forward =>
        val lines = forwardLines(width, height, shape.getStrokeColor)
        Some(generateStructurePattern(lines, width, height))
      case FillPattern.CrossDiag =>
        val backward = backwardLines(width, height, shape.getStrokeColor)
        val forward = forwardLines(width, height, shape.getStrokeColor)
        val lines = backward ++ forward
        Some(generateStructurePattern(lines, width, height))
      case _ => None

  private def genColorStyle(shape: ColorizableShape): String =
    List(
      "stroke: " + colorToCssColor(shape.getStrokeColor),
      "stroke-width: " + shape.getStrokeWidth.toInt,
      "stroke-opacity: " + (if shape.getStrokeColor == null then "0.0"
                            else "1.0")
    ).mkString(";")

  def writeToFile(str: String)(target: Path): Unit =
    val writer = Files.newBufferedWriter(target, encoding)
    try
      writer.write(str)
    finally
      writer.close()
