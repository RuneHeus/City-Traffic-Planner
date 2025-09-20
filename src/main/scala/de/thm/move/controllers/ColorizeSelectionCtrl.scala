/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.controllers

import javafx.scene.paint.Color
import de.thm.move.Global._
import de.thm.move.models.{FillPattern, LinePattern}
import de.thm.move.views.GroupLike
import de.thm.move.views.shapes.{ColorizableShape, ResizableShape}

import scala.jdk.CollectionConverters._

/** Behaviour for colorizing selected ResizableShapes. That is, changing the
  * fill colour, stroke colour, stroke width
  */
trait ColorizeSelectionCtrl:
  this: SelectionCtrlLike =>

  /** Gets all shapes that are colorizable and removes groups if they exist */
  private def coloredSelectedShape: List[ResizableShape with ColorizableShape] =
    def findColorizables(
        xs: List[ResizableShape]
    ): List[ResizableShape with ColorizableShape] =
      xs flatMap {
        // filter non-colrizable shapes; they have no linepattern
        case colorizable: ColorizableShape => List(colorizable)
        case g: GroupLike                  => findColorizables(g.childrens)
        case _                             => Nil
      }

    findColorizables(getSelectedShapes)

  def setFillColor(color: Color): Unit = if !getSelectedShapes.isEmpty then
    zippedUndo(coloredSelectedShape)(_.getFillColor)(
      _.setFillColor(color),
      _.setFillColor
    )

  def setStrokeColor(color: Color): Unit = if !getSelectedShapes.isEmpty then
    zippedUndo(coloredSelectedShape)(_.getStrokeColor)(
      _.setStrokeColor(color),
      _.setStrokeColor
    )

  def setStrokeWidth(width: Int): Unit =
    zippedUndo(coloredSelectedShape)(_.getStrokeWidth)(
      _.setStrokeWidth(width),
      _.setStrokeWidth
    )

  def setStrokePattern(linePattern: LinePattern.LinePattern): Unit =
    LinePattern.linePatternToCssClass.get(linePattern) foreach { cssClass =>
      val cssOpt = coloredSelectedShape.map(
        _.getStyleClass.asScala.find(_.`matches`(LinePattern.cssRegex))
      )
      val linePatterns = coloredSelectedShape.map(_.linePattern.get)
      val shapeAndCss = coloredSelectedShape zip (cssOpt zip linePatterns)

      history.execute {
        for shape <- coloredSelectedShape do
          LinePattern.removeOldCss(shape)
          shape.getStyleClass().add(cssClass)
          shape.linePattern.set(linePattern)
      } {
        for
          (shape, (oldCssOpt, oldLinePattern)) <- shapeAndCss
          if oldCssOpt.isDefined
          css = oldCssOpt.get
        do
          LinePattern.removeOldCss(shape)
          shape.getStyleClass().add(css)
          shape.linePattern.set(oldLinePattern)
      }
    }

  def setFillPattern(fillPattern: FillPattern.FillPattern): Unit =
    val coloredShapes = coloredSelectedShape map { shape =>
      (shape, shape.oldFillColorProperty.get, shape.getStrokeColor)
    } flatMap {
      case (shape, c1, c2: Color) => List((shape, c1, c2))
      case _                      => Nil
    }

    val shapeAndFillPattern =
      coloredSelectedShape zip (coloredSelectedShape.map(
        _.fillPatternProperty.get
      ) zip coloredSelectedShape.map(_.getFillColor))

    history.execute {
      for (shape, fillColor, strokeColor) <- coloredShapes do
        val width = shape.getBoundsInLocal.getWidth()
        val height = shape.getBoundsInLocal.getHeight()
        val newFillColor = FillPattern.getFillColor(
          fillPattern,
          fillColor,
          strokeColor,
          width,
          height
        )
        shape.setFillColor(newFillColor)
        shape.fillPatternProperty.set(fillPattern)
    } {
      for (shape, (oldFillProperty, oldFillGradient)) <- shapeAndFillPattern do
        shape.setFillColor(oldFillGradient)
        shape.fillPatternProperty.set(oldFillProperty)
    }
