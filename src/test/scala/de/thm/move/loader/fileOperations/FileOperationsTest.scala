package de.thm.move.loader.fileOperations

import de.thm.move.MoveSpec
import de.thm.move.Roads.{Road, RoadManager, RoadNode, RoadType}
import de.thm.move.controllers.{JsonCtrl, MoveCtrl}
import de.thm.move.types.*
import de.thm.move.views.shapes.*
import javafx.application.{Application, Platform}
import javafx.scene.Node
import javafx.scene.paint.Color
import javafx.stage.Stage

import java.io.{File, PrintWriter}
import scala.jdk.CollectionConverters.*
import org.scalactic.Prettifier.default

/**
 * Initializes a JavaFX environment for testing purposes.
 */
object TestJavaFXInitializer {
  private val initialized = new java.util.concurrent.atomic.AtomicBoolean(false)
  private val latch = new java.util.concurrent.CountDownLatch(1)

  /**
   * Initializes the JavaFX application thread.
   */
  def initialize(): Unit = {
    if !initialized.getAndSet(true) then
      new Thread(() => {
        Application.launch(classOf[TestJavaFXApp])
      }).start()
      latch.await()
  }

  /**
   * JavaFX application for testing purposes.
   */
  class TestJavaFXApp extends Application {
    override def start(primaryStage: Stage): Unit = {
      latch.countDown()
    }
  }
}

class FileOperationsTest extends MoveSpec {

  /**
   * Test: Save a canvas with shapes to a JSON file and verify its content.
   */
  "save_json_file" should "save canvas and shapes to a JSON file" in {
    val polygon1Points: List[Point] = List(
      (222.0, 138.0), (312.0, 168.0), (289.0, 231.0), (191.0, 264.0), (116.0, 170.0), (119.0, 106.0), (213.0, 82.0)
    )
    val polygon2Points: List[Point] = List(
      (315.0, 241.0), (289.6015415259375, 229.35229929851909), (289.0, 231.0),
      (191.0, 264.0), (122.18694362017804, 177.75430267062313), (113.0, 180.0),
      (72.0, 227.0), (100.0, 342.0), (306.0, 328.0), (315.0, 241.0)
    )

    val polygon1 = ResizablePolygon(polygon1Points)
    polygon1.setFillColor(Color(0, 0, 1, 1)) // Blue fill color
    polygon1.setStrokeColor(Color(0, 0, 0, 1)) // Black stroke color
    polygon1.setStrokeWidth(1)

    val polygon2 = ResizablePolygon(polygon2Points)
    polygon2.setFillColor(Color(0, 0, 1, 1)) // Blue fill color
    polygon2.setStrokeColor(Color(0, 0, 0, 1)) // Black stroke color
    polygon2.setStrokeWidth(1)

    val line1 = ResizableLine((387.0, 269.0), (464.1873001874104, 149.8630801455187), 4)
    line1.setFillColor(Color(0, 0, 0, 1)) // Black fill color
    line1.setStrokeColor(Color(0, 0, 0, 1)) // Black stroke color
    line1.setId("1735909221676")

    val line2 = ResizableLine((464.1873001874104, 149.8630801455187), (525.0, 56.0), 4)
    line2.setFillColor(Color(0, 0, 0, 1)) // Black fill color
    line2.setStrokeColor(Color(0, 0, 0, 1)) // Black stroke color
    line2.setId("80a384f1-58c0-4990-b2bc-c63aef708178")

    val line3 = ResizableLine((333.0, 48.0), (464.1873001874104, 149.8630801455187), 4)
    line3.setFillColor(Color(0, 0, 0, 1)) // Black fill color
    line3.setStrokeColor(Color(0, 0, 0, 1)) // Black stroke color
    line3.setId("1735909220957")

    val line4 = ResizableLine((464.1873001874104, 149.8630801455187), (588.0, 246.0), 4)
    line4.setFillColor(Color(0, 0, 0, 1)) // Black fill color
    line4.setStrokeColor(Color(0, 0, 0, 1)) // Black stroke color
    line4.setId("0c2c4681-ba4f-4344-90de-8d2f08f07a98")

    val road1 = new Road(
      id = "1735909220957",
      start = new RoadNode("f3121315-5981-43d9-964d-6b2b6b7095c9"),
      end = new RoadNode("a70f51f3-e525-4f40-95bb-23b719b2a8a7"),
      roadType = RoadType.Normal
    )

    val road2 = new Road(
      id = "0c2c4681-ba4f-4344-90de-8d2f08f07a98",
      start = new RoadNode("a70f51f3-e525-4f40-95bb-23b719b2a8a7"),
      end = new RoadNode("f841db6b-2b1e-48ea-a111-46097499e85f"),
      roadType = RoadType.Normal
    )

    val road3 = new Road(
      id = "1735909221676",
      start = new RoadNode("3aaeaf06-f5d1-40e9-97fc-7b49ea4f24c5"),
      end = new RoadNode("a70f51f3-e525-4f40-95bb-23b719b2a8a7"),
      roadType = RoadType.Normal
    )

    val road4 = new Road(
      id = "80a384f1-58c0-4990-b2bc-c63aef708178",
      start = new RoadNode("a70f51f3-e525-4f40-95bb-23b719b2a8a7"),
      end = new RoadNode("dc1338eb-76ad-4a39-8086-838d32b40eb3"),
      roadType = RoadType.Normal
    )

    val shapes: List[Any] = List(polygon1, polygon2, line1, line2, line3, line4, road1, road2, road3, road4)
    val canvasWidth = 800.0
    val canvasHeight = 600.0
    val savePath = os.pwd / "test_shapes.json"

    try{
      JsonCtrl.save_json_file(shapes, canvasWidth, canvasHeight, savePath)

      val savedContent = os.read(savePath)

      val expectedContent =
        """
      {
        "Move": {
          "Canvas": {
            "Width": 800,
            "Height": 600
          },
          "Objects": [
            {
              "type": "ResizablePolygon",
              "points": [
                222,
                138,
                312,
                168,
                289,
                231,
                191,
                264,
                116,
                170,
                119,
                106,
                213,
                82
              ],
              "fillColor": {
                "r": 0,
                "g": 0,
                "b": 1,
                "a": 1
              },
              "strokeColor": {
                "r": 0,
                "g": 0,
                "b": 0,
                "a": 1
              },
              "strokeWidth": 1,
              "linePattern": "Solid",
              "fillPattern": "Solid"
            },
            {
              "type": "ResizablePolygon",
              "points": [
                315,
                241,
                289.6015415259375,
                229.35229929851909,
                289,
                231,
                191,
                264,
                122.18694362017804,
                177.75430267062313,
                113,
                180,
                72,
                227,
                100,
                342,
                306,
                328,
                315,
                241
              ],
              "fillColor": {
                "r": 0,
                "g": 0,
                "b": 1,
                "a": 1
              },
              "strokeColor": {
                "r": 0,
                "g": 0,
                "b": 0,
                "a": 1
              },
              "strokeWidth": 1,
              "linePattern": "Solid",
              "fillPattern": "Solid"
            },
            {
              "type": "ResizableLine",
              "id": "1735909221676",
              "start": [
                387,
                269
              ],
              "end": [
                464.1873001874104,
                149.8630801455187
              ],
              "strokeSize": 4,
              "fillColor": {
                "r": 0,
                "g": 0,
                "b": 0,
                "a": 1
              },
              "strokeColor": {
                "r": 0,
                "g": 0,
                "b": 0,
                "a": 1
              }
            },
            {
              "type": "ResizableLine",
              "id": "80a384f1-58c0-4990-b2bc-c63aef708178",
              "start": [
                464.1873001874104,
                149.8630801455187
              ],
              "end": [
                525,
                56
              ],
              "strokeSize": 4,
              "fillColor": {
                "r": 0,
                "g": 0,
                "b": 0,
                "a": 1
              },
              "strokeColor": {
                "r": 0,
                "g": 0,
                "b": 0,
                "a": 1
              }
            },
            {
              "type": "ResizableLine",
              "id": "1735909220957",
              "start": [
                333,
                48
              ],
              "end": [
                464.1873001874104,
                149.8630801455187
              ],
              "strokeSize": 4,
              "fillColor": {
                "r": 0,
                "g": 0,
                "b": 0,
                "a": 1
              },
              "strokeColor": {
                "r": 0,
                "g": 0,
                "b": 0,
                "a": 1
              }
            },
            {
              "type": "ResizableLine",
              "id": "0c2c4681-ba4f-4344-90de-8d2f08f07a98",
              "start": [
                464.1873001874104,
                149.8630801455187
              ],
              "end": [
                588,
                246
              ],
              "strokeSize": 4,
              "fillColor": {
                "r": 0,
                "g": 0,
                "b": 0,
                "a": 1
              },
              "strokeColor": {
                "r": 0,
                "g": 0,
                "b": 0,
                "a": 1
              }
            },
            {
              "type": "Road",
              "id": "1735909220957",
              "start": "f3121315-5981-43d9-964d-6b2b6b7095c9",
              "end": "a70f51f3-e525-4f40-95bb-23b719b2a8a7",
              "roadType": "Normal",
              "one_way": "None"
            },
            {
              "type": "Road",
              "id": "0c2c4681-ba4f-4344-90de-8d2f08f07a98",
              "start": "a70f51f3-e525-4f40-95bb-23b719b2a8a7",
              "end": "f841db6b-2b1e-48ea-a111-46097499e85f",
              "roadType": "Normal",
              "one_way": "None"
            },
            {
              "type": "Road",
              "id": "1735909221676",
              "start": "3aaeaf06-f5d1-40e9-97fc-7b49ea4f24c5",
              "end": "a70f51f3-e525-4f40-95bb-23b719b2a8a7",
              "roadType": "Normal",
              "one_way": "None"
            },
            {
              "type": "Road",
              "id": "80a384f1-58c0-4990-b2bc-c63aef708178",
              "start": "a70f51f3-e525-4f40-95bb-23b719b2a8a7",
              "end": "dc1338eb-76ad-4a39-8086-838d32b40eb3",
              "roadType": "Normal",
              "one_way": "None"
            }
          ]
        }
      }
      """

      val normalizedSavedContent = savedContent.replaceAll("\\s+", "").trim
      val normalizedExpectedContent = expectedContent.replaceAll("\\s+", "").trim
      assert(normalizedSavedContent == normalizedExpectedContent)
    }finally {
      os.remove(savePath)
    }
  }

  /**
   * Test: Save an empty canvas to a JSON file and verify its content.
   */
  "save_json_file" should "save an empty canvas to a JSON file with no shapes" in {
    val shapes: List[Node] = List()
    val canvasWidth = 800.0
    val canvasHeight = 600.0
    val savePath = os.pwd / "test_empty_canvas.json"

    try {
      JsonCtrl.save_json_file(shapes, canvasWidth, canvasHeight, savePath)

      val savedContent = os.read(savePath)

      val expectedContent =
        """
        {
          "Move": {
            "Canvas": {
              "Width": 800,
              "Height": 600
            },
            "Objects": []
          }
        }
        """

      val normalizedSavedContent = savedContent.replaceAll("\\s+", "").trim
      val normalizedExpectedContent = expectedContent.replaceAll("\\s+", "").trim
      assert(normalizedSavedContent == normalizedExpectedContent)
    } finally {
      os.remove(savePath)
    }
  }

  /**
   * Test: Load shapes from a JSON file and verify they are correctly rendered.
   */
  "open_json_file" should "load a JSON file and render lines and polygons correctly on the canvas" in {
    val canvasWidthBefore: Double = 800.0
    val canvasHeightBefore: Double = 600.0

    val line = ResizableLine(
      start = (387.0, 269.0),
      end = (464.0, 149.0),
      strokeSize = 4
    )
    line.setFillColor(Color(0, 0, 0, 1.0))
    line.setStrokeColor(Color(0, 0, 0, 1.0))
    line.setId("1735909221676")

    val polygon = ResizablePolygon(
      points = List((161.0, 44.0), (344.0, 195.0), (319.0, 241.0), (129.0, 282.0), (77.0, 170.0), (140.0, 81.0))
    )
    polygon.setFillColor(Color(0, 0, 0, 1.0))
    polygon.setStrokeColor(Color(0, 0, 0, 0.0))
    polygon.setStrokeWidth(1)

    val shapeList: List[ResizableShape] = List(line, polygon)

    val tempFilePath = os.pwd / "test_open_shapes.json"

    try {
      JsonCtrl.save_json_file(shapeList, canvasWidthBefore, canvasHeightBefore, tempFilePath)

      val (canvasWidth, canvasHeight, shapes) = JsonCtrl.open_json_file(tempFilePath)

      assert(canvasWidth == 800)
      assert(canvasHeight == 600)

      assert(shapes.length == 2)

      shapes.head match {
        case line: ResizableLine =>
          assert(line.getStartX == 387)
          assert(line.getStartY == 269)
          assert(line.getEndX == 464)
          assert(line.getEndY == 149)
          assert(line.getStrokeWidth == 4)
          assert(line.getFillColor == Color(0, 0, 0, 1.0))
          assert(line.getStrokeColor == Color(0, 0, 0, 1.0))
        case _ => fail("First shape is not a ResizableLine")
      }

      shapes(1) match {
        case polygon: ResizablePolygon =>
          val expectedPoints = List(
            161.0, 44.0,
            344.0, 195.0,
            319.0, 241.0,
            129.0, 282.0,
            77.0, 170.0,
            140.0, 81.0
          )
          assert(polygon.getPoints.asScala.toList == expectedPoints)
          assert(polygon.getFillColor == Color(0, 0, 0, 1.0))
          assert(polygon.getStrokeColor == Color(0, 0, 0, 0.0))
          assert(polygon.getStrokeWidth == 1)
        case _ => fail("Second shape is not a ResizablePolygon")
      }
    } finally {
      os.remove(tempFilePath)
    }
  }

    /**
     * Test: Load a JSON file with roads and verify they are correctly rendered.
     */
  "The loading" should "properly make roads from lines and add them to the RoadManager" in {
    if System.getenv("CI") == null then

      TestJavaFXInitializer.initialize()
      val inputJson =
        """
            {
              "Move": {
                "Canvas": {
                  "Width": 800,
                  "Height": 600
                },
                "Objects": [
                  {
                    "type": "ResizableLine",
                    "start": [
                      100,
                      40
                    ],
                    "end": [
                      535,
                      36
                    ],
                    "strokeSize": 4,
                    "fillColor": {
                      "r": 0,
                      "g": 0,
                      "b": 0,
                      "a": 1
                    },
                    "strokeColor": {
                      "r": 0,
                      "g": 0,
                      "b": 0,
                      "a": 1
                    }
                  },
                  {
                    "type": "ResizableLine",
                    "start": [
                      90,
                      116
                    ],
                    "end": [
                      550,
                      119
                    ],
                    "strokeSize": 4,
                    "fillColor": {
                      "r": 0.6470588445663452,
                      "g": 0.16470588743686676,
                      "b": 0.16470588743686676,
                      "a": 1
                    },
                    "strokeColor": {
                      "r": 0.6470588445663452,
                      "g": 0.16470588743686676,
                      "b": 0.16470588743686676,
                      "a": 1
                    }
                  },
                  {
                    "type": "ResizableLine",
                    "start": [
                      91,
                      186
                    ],
                    "end": [
                      560,
                      190
                    ],
                    "strokeSize": 8,
                    "fillColor": {
                      "r": 0,
                      "g": 0,
                      "b": 0,
                      "a": 1
                    },
                    "strokeColor": {
                      "r": 0,
                      "g": 0,
                      "b": 0,
                      "a": 1
                    }
                  }
                ]
              }
            }"""

      val parsedJson = ujson.read(inputJson.trim)
      val formattedJson = ujson.write(parsedJson, indent = 2)

      val tempFilePath = os.pwd / "test_open_shapes.json"
      val writer = new PrintWriter(new File(tempFilePath.toString))

      try {
        writer.write(formattedJson)
        writer.flush()
      } finally {
        writer.close()
      }

      val latch = new java.util.concurrent.CountDownLatch(1)

      Platform.runLater(() => {
        try {
          val (width, height, shapes) = JsonCtrl.open_json_file(tempFilePath)
          MoveCtrl().addShapesWithLogic(shapes, MoveCtrl().drawPanelCtrl)

          val roadList = RoadManager.roads.values.toList

          val road1: Road = roadList.head
          val road2: Road = roadList(1)
          val road3: Road = roadList(2)

          val road1Line = ResizableLine.allLines.find(_.getId == road1.id).get
          val road2Line = ResizableLine.allLines.find(_.getId == road2.id).get
          val road3Line = ResizableLine.allLines.find(_.getId == road3.id).get
          
          assert(RoadManager.roads.size == 3)

          assert(road1.roadType == RoadType.UnPaved)
          assert(road2.roadType == RoadType.Double)
          assert(road3.roadType == RoadType.Normal)

          assert((road1Line.getStartX, road1Line.getStartY) == (90.0,116.0) && (road1Line.getEndX, road1Line.getEndY) == (550.0,119.0))
          assert((road2Line.getStartX, road2Line.getStartY) == (100.0,40.0) && (road2Line.getEndX, road2Line.getEndY)  == (535.0,36.0))
          assert((road3Line.getStartX, road3Line.getStartY)== (91.0,186.0) && (road3Line.getEndX, road3Line.getEndY) == (560.0,190.0))

          latch.countDown()
        } catch {
          case e: Exception =>
            e.printStackTrace()
            latch.countDown()
        }finally {
          os.remove(tempFilePath)
        }
      })
      assert(latch.await(5, java.util.concurrent.TimeUnit.SECONDS), "JavaFX test did not complete in time")
    else
      cancel("JavaFX tests are skipped in CI")
  }
}