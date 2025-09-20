package de.thm.move.controllers.drawing.unitTesting

import de.thm.move.MoveSpec
import de.thm.move.controllers.DrawPanelCtrl
import de.thm.move.controllers.drawing.{ZoneStrategy }
import de.thm.move.views.panes.DrawPanel
import de.thm.move.views.shapes.ResizablePolygon
import javafx.scene.input.{InputEvent, MouseEvent}
import org.mockito.Mockito.{verify, when}
import org.scalatest.{OneInstancePerTest, Outcome}
import org.scalatestplus.mockito.MockitoSugar


class polygonTest extends MoveSpec with MockitoSugar with OneInstancePerTest:

  val drawPanel = new DrawPanel()
  val dummyHandler = { (ev: InputEvent) => () }
  val drawPanelCtrl = new DrawPanelCtrl(drawPanel, dummyHandler)
  val testPolygonStrategy = new ZoneStrategy(drawPanelCtrl)

  //polygon 1 setup
  val P1x1 = 10
  val P1y1 = 10
  val P1x2 = 40
  val P1y2 = 10
  val P1x3 = 40
  val P1y3 = 40
  val P1x4 = 10
  val P1y4 = 40
  val polygon1 = new ResizablePolygon(List(P1x1, P1y1, P1x2, P1y2, P1x3, P1y3, P1x4, P1y4))

  //polygon 2 setup, this one overlaps with polygon 1.
  val P2x1 = 30
  val P2y1 = 20
  val P2x2 = 60
  val P2y2 = 20
  val P2x3 = 60
  val P2y3 = 50
  val P2x4 = 30
  val P2y4 = 50
  val polygon2 = new ResizablePolygon(List(P2x1, P2y1, P2x2, P2y2, P2x3, P2y3, P2x4, P2y4))

  //polygon 3 setup, this one will not overlap with any polygon.
  val P3x1 = 80
  val P3y1 = 20
  val P3x2 = 90
  val P3y2 = 20
  val P3x3 = 90
  val P3y3 = 50
  val P3x4 = 80
  val P3y4 = 50
  val polygon3 = new ResizablePolygon(List(P3x1, P3y1, P3x2, P3y2, P3x3, P3y3, P3x4, P3y4))


  //polygon 4 setup, this one will overlap with Polygon 1, 2 and 3.
  val P4x1 = 0
  val P4y1 = 30
  val P4x2 = 110
  val P4y2 = 30
  val P4x3 = 110
  val P4y3 = 70
  val P4x4 = 0
  val P4y4 = 70
  val polygon4 = new ResizablePolygon(List(P4x1, P4y1, P4x2, P4y2, P4x3, P4y3, P4x4, P4y4))


  //polygon that has intersecting edges with its own edges
  val P5x1 = 0
  val P5y1 = 30
  val P5x2 = 110
  val P5y2 = 30
  val P5x3 = 110
  val P5y3 = 70
  val P5x4 = 0
  val P5y4 = 70
  val polygon5 = new ResizablePolygon(List(P5x1, P5y1, P5x2, P5y2, P5x4, P5y4, P5x3, P5y3))


  override def withFixture(test: NoArgTest): Outcome =

    try test()

  "Polygon 2" should "overlap with Polygon 1" in {
    val overlappingPolygons = testPolygonStrategy.checkOverlappingPolygonsDeprecated(List(polygon2), polygon1)
    assert(overlappingPolygons.nonEmpty)
  }

  "Polygon 3" should "not overlap with Polygon 1" in {
    val overlappingPolygons = testPolygonStrategy.checkOverlappingPolygonsDeprecated(List(polygon3), polygon1)
    assert(overlappingPolygons.isEmpty)
  }


  "Polygon 3" should "not overlap with Polygon 2" in {
    val overlappingPolygons = testPolygonStrategy.checkOverlappingPolygonsDeprecated(List(polygon3), polygon2)
    assert(overlappingPolygons.isEmpty)
  }




  "Polygon 4" should "overlap with Polygon 1" in {
    val overlappingPolygons = testPolygonStrategy.checkOverlappingPolygonsDeprecated(List(polygon4), polygon1)
    assert(overlappingPolygons.nonEmpty)
  }

  "Polygon 4" should "overlap with Polygon 2" in {
    val overlappingPolygons = testPolygonStrategy.checkOverlappingPolygonsDeprecated(List(polygon4), polygon2)
    assert(overlappingPolygons.nonEmpty)
  }


  "Polygon 4" should "overlap with Polygon 3" in {
    val overlappingPolygons = testPolygonStrategy.checkOverlappingPolygonsDeprecated(List(polygon4), polygon3)
    assert(overlappingPolygons.nonEmpty)
  }


  "Polygon 5" should "not have internal intersecting edges" in {
    val overlapsInternally= testPolygonStrategy.checkOverlappingWithOwnEdges(polygon5)
    assert(overlapsInternally)//test that they actually intersect
  }