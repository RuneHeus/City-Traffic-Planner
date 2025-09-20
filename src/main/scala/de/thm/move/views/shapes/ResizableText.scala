/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.views.shapes

import javafx.scene.paint.Paint
import javafx.scene.text.{Font, FontPosture, FontWeight, Text}

import de.thm.move.types._
import de.thm.move.views.anchors.Anchor

import upickle.default._
import javafx.scene.paint.Color

class ResizableText(
    txt: String,
    x: Double,
    y: Double,
    font: Font = Font.getDefault
) extends Text(x, y, txt)
    with ResizableShape:
  setFont(font)
  override val getAnchors: List[Anchor] = Nil

  private var isBold = false
  private var isItalic = false

  private def createNewFont(
      name: String,
      size: Double,
      isBold: Boolean,
      isItalic: Boolean
  ): Font =
    Font.font(
      name,
      if isBold then FontWeight.BOLD else FontWeight.NORMAL,
      if isItalic then FontPosture.ITALIC else FontPosture.REGULAR,
      size
    )

  def setFontName(name: String): Unit = setFont(
    createNewFont(name, getSize, isBold, isItalic)
  )
  def getFontName: String = getFont.getFamily

  def setSize(pt: Double): Unit =
    setFont(createNewFont(getFontName, pt, isBold, isItalic))

  def setFontColor(color: Paint): Unit = setFill(color)

  def setBold(flag: Boolean): Unit =
    isBold = flag
    setFont(createNewFont(getFontName, getSize, isBold, isItalic))

  def setItalic(flag: Boolean): Unit =
    isItalic = flag
    setFont(createNewFont(getFontName, getSize, isBold, isItalic))

  def getBold: Boolean = isBold
  def getItalic: Boolean = isItalic
  def getSize: Double = getFont.getSize
  def getFontColor: Paint = getFill
  def copy: ResizableText =
    val txt = new ResizableText(getText, getX, getY, getFont)
    txt.setFontColor(getFontColor)
    txt.setRotate(getRotate)
    txt

  override def move(delta: Point): Unit =
    val (x, y) = delta
    setX(getX + x)
    setY(getY + y)


object ResizableText:
  implicit val rw: ReadWriter[ResizableText] = readwriter[ujson.Value].bimap[ResizableText](
    // Serialization
    text => ujson.Obj(
      "type" -> "ResizableText",
      "txt" -> text.getText,
      "x" -> text.getX,
      "y" -> text.getY,
      "fontName" -> text.getFontName,
      "fontSize" -> text.getSize,
      "isBold" -> text.getBold,
      "isItalic" -> text.getItalic,
      "color" -> text.getFontColor.toString
    ),
    json => {
      if json("type").str != "ResizableText" then
        throw new IllegalArgumentException("Unexpected shape type")

      val txt = json("txt").str
      val x = json("x").num
      val y = json("y").num
      val fontName = json("fontName").str
      val fontSize = json("fontSize").num
      val isBold = json("isBold").bool
      val isItalic = json("isItalic").bool
      val color = Color.valueOf(json("color").str)

      val font = Font.font(
        fontName,
        if isBold then FontWeight.BOLD else FontWeight.NORMAL,
        if isItalic then FontPosture.ITALIC else FontPosture.REGULAR,
        fontSize
      )

      val resizableText = new ResizableText(txt, x, y, font)
      resizableText.setFontColor(color)
      resizableText.setBold(isBold)
      resizableText.setItalic(isItalic)
      resizableText
    }
  )
