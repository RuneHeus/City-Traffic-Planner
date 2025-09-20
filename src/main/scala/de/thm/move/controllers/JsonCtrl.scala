package de.thm.move.controllers

import de.thm.move.Roads.{Road, RoadManager, RoadNode}
import de.thm.move.views.shapes.*
import upickle.default.*
import upickle.legacy.ReadWriter as LegacyRW
import os.Path
import upickle.default.readwriter as defaultRW

import de.thm.move.Roads.Road.rw
import de.thm.move.views.shapes.ResizablePolygon.rw
import upickle.default._

import java.nio.file.{Path, Paths}

/** Object for handling JSON file operations related to canvas and shape data.
 * Provides methods for saving shapes and canvas dimensions to JSON files and
 * reading them.
 */

trait JsonCtrlBehavior {
  def save_json_file(shapes: List[Any], width: Double, height: Double, path: os.Path): Unit
}

object JsonCtrl extends JsonCtrlBehavior {

  /** Implicit JSON writer for `Any` object
   * to JSON using their respective upickle Read/Write.
   *
   * @throws IllegalArgumentException if an unsupported `Any` type is encountered.
   */
  implicit val nodeWriter: Writer[Any] = writer[ujson.Value].comap[Any] {
    case circle: ResizableCircle =>
      writeJs(circle)(ResizableCircle.rw)
    case rectangle: ResizableRectangle =>
      writeJs(rectangle)(ResizableRectangle.rw)
    case line: ResizableLine =>
      writeJs(line)(ResizableLine.rw)
    case polygon: ResizablePolygon =>
      writeJs(polygon)(ResizablePolygon.rw)
    case road: Road =>
      writeJs(road)(Road.rw)
    case obj =>
      throw new IllegalArgumentException(
        s"Unknown type: ${obj.getClass.getName}, unable to serialize."
      )
  }
  
  /** Implicit JSON writer for a list of `Any`s.
   * Serializes each `Any` in the list to JSON using the `nodeWriter` and
   * wraps them in a JSON array.
   */
  implicit val nodesWriter: Writer[List[Any]] = writer[ujson.Arr].comap[List[Any]] { nodes =>
    ujson.Arr(nodes.map(node => writeJs(node)(nodeWriter)): _*)
  }

  /** Converts a Java NIO path (`java.nio.file.Path`) to an OS-Lib path (`os.Path`).
   *
   * @param path the Java NIO path to convert
   * @return the corresponding OS-Lib path
   */
  def path_conversion(path: java.nio.file.Path): os.Path = {
    val javaPath: java.nio.file.Path = Paths.get(path.toString)
    os.Path(javaPath)
  }

  /** Saves canvas data and a list of shapes as JSON to the specified path.
   *
   * @param nodes the list of shapes (`Any` elements) to save
   * @param width the width of the canvas
   * @param height the height of the canvas
   * @param path the file path where the JSON will be saved
   */
  def save_json_file(nodes: List[Any], width: Double, height: Double, path: os.Path): Unit = {
    // Wraps canvas dimensions and shape data in a JSON object.
    val wrappedJson = ujson.Obj(
      "Move" -> ujson.Obj(
        "Canvas" -> ujson.Obj(
          "Width" -> width,
          "Height" -> height
        ),
        "Objects" -> writeJs(nodes)(nodesWriter)
      )
    )

    // Serialize JSON object to a formatted string.
    val json: String = wrappedJson.render(indent = 2)

    // Writes JSON string to specified file path, overwriting if exists.
    os.write.over(path, json)
  }

  /** Reads JSON data from the specified path and reconstructs canvas dimensions
   * and a list of shapes.
   *
   * @param path the file path from which to read the JSON
   * @return a tuple containing:
   *         - the canvas width (Double)
   *         - the canvas height (Double)
   *         - a list of `Any` objects reconstructed from JSON data
   * @throws IllegalArgumentException if an unsupported shape type is encountered
   */
  def open_json_file(path: os.Path): (Double, Double, List[Any]) = {
    val jsonContent = os.read(path)

    val parsedJson = ujson.read(jsonContent)

    val canvas = parsedJson("Move")("Canvas")
    val width = canvas("Width").num
    val height = canvas("Height").num

    val objectsJson = parsedJson("Move")("Objects").arr

    val shapes: List[Any] = objectsJson.map { objJson => //Dit zijn de voorlopige shapes
      objJson("type").str match {
        case "ResizablePolygon" =>
          read[ResizablePolygon](objJson) // Uses ResizablePolygon's reader/writer
        case "ResizableLine" =>
          read[ResizableLine](objJson)
        case "Road" =>
          read[Road](objJson)
        case _ =>
          throw new IllegalArgumentException(s"Unknown shape type: ${objJson("type").str}")
      }
    }.toList

    (width, height, shapes)
  }
}
