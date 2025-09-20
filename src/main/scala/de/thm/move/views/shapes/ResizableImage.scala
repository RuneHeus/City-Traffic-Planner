/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.views.shapes

import java.net.URI
import javafx.scene.image.{Image, ImageView}
import upickle.default.*

import java.util.Base64

/** An image with either an URI as '''underlying image-path''' or an array of
  * bytes indicating that its image is '''base64-encoded''' and an image used
  * for displaying the image itself.
  */
class ResizableImage(val srcEither: Either[URI, Array[Byte]], val img: Image)
    extends ImageView(img)
    with ResizableShape
    with RectangleLike:
  setPreserveRatio(true)

  /* When preserving-ratio resize only 1 side; the other gets adjusted */
  val resizeWidth = img.getWidth > img.getHeight
  if resizeWidth then setFitWidth(200)
  else setFitHeight(200)

  override def getWidth: Double =
    if resizeWidth then getFitWidth // get the fitting width
    else getBoundsInLocal.getWidth // get the calculated with
  override def getHeight: Double =
    if !resizeWidth then getFitHeight // get the fitting height
    else getBoundsInLocal.getHeight // get the calculated height

  override def setWidth(w: Double): Unit =
    if resizeWidth then setFitWidth(w) // set fitting width
    else
      (
    ) // don't do anything; this side gets calculated according to the height
  override def setHeight(h: Double): Unit =
    if !resizeWidth then setFitHeight(h)
    else ()

  override def copy: ResizableImage =
    val duplicate = new ResizableImage(srcEither, img)
    duplicate.copyPosition(this)
    duplicate

object ResizableImage:
  def apply(uri: URI, img: Image) = new ResizableImage(Left(uri), img)

  def apply(bytes: Array[Byte], img: Image) =
    new ResizableImage(Right(bytes), img)

  implicit val uriRW: ReadWriter[URI] = readwriter[String].bimap[URI](
    uri => uri.toString,
    str => new URI(str)
  )

  implicit val rw: ReadWriter[ResizableImage] = readwriter[ujson.Value].bimap[ResizableImage](
    // Serialization
    image => ujson.Obj(
      "type" -> "ResizableImage",
      "srcType" -> (if image.srcEither.isLeft then "URI" else "Base64"),
      "src" -> (image.srcEither match
        case Left(uri) => uri.toString
        case Right(bytes) => Base64.getEncoder.encodeToString(bytes)
        ),
      "width" -> image.getWidth,
      "height" -> image.getHeight
    ),
    // Deserialization
    json => {
      if json("type").str != "ResizableImage" then
        throw new IllegalArgumentException("Unexpected shape type")

      val srcEither = json("srcType").str match
        case "URI" => Left(new URI(json("src").str))
        case "Base64" =>
          val decodedBytes = Base64.getDecoder.decode(json("src").str)
          Right(decodedBytes)
        case _ =>
          throw new IllegalArgumentException("Unknown srcType for ResizableImage")

      val image = srcEither match
        case Left(uri) => new Image(uri.toString)
        case Right(bytes) => new Image(new java.io.ByteArrayInputStream(bytes))

      new ResizableImage(srcEither, image)
    }

  )
