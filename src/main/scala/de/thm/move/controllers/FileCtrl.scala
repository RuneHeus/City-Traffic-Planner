/** Copyright (C) 2016 Nicola Justus <nicola.justus@mni.thm.de>
  *
  * This Source Code Form is subject to the terms of the Mozilla Public License,
  * v. 2.0. If a copy of the MPL was not distributed with this file, You can
  * obtain one at http://mozilla.org/MPL/2.0/.
  */

package de.thm.move.controllers

import java.net.URI
import java.nio.file.{Files, Path, Paths}
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Node
import javafx.scene.control.{ButtonType, ChoiceDialog}
import javafx.stage.Window

import javax.imageio.ImageIO
import upickle.default.*
import de.thm.move.Global.*
import de.thm.move.Roads.RoadNode
import de.thm.move.implicits.MonadImplicits.*
import de.thm.move.loader.ShapeConverter
import de.thm.move.loader.parser.ModelicaParserLike
import de.thm.move.loader.parser.ast.{JsonModel, Model}
import de.thm.move.models.ModelicaCodeGenerator.FormatSrc.*
import de.thm.move.models.{ModelicaCodeGenerator, SrcFile, SvgCodeGenerator, UserInputException}
import de.thm.move.types.*
import de.thm.move.util.converters.Marshaller.*
import de.thm.move.views.dialogs.{Dialogs, ExternalChangesDialog, SrcFormatDialog}
import de.thm.move.views.shapes.{ResizableCircle, ResizableShape}

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/** Controller for interaction with files. (opening, saving, exporting) This
  * controller asks the user to select a appropriate file and uses the selected
  * file for it's functions.
  */
class FileCtrl(owner: Window):
  case class FormatInfos(pxPerMm: Int, srcFormat: Option[FormatSrc]) //Pixel per milimeter, scr heeft of een value of niets

  /** Informations about the current open file */
  private var openedFile: Option[SrcFile] = None

  /** Sourcecode format specified by the user - either Oneline or Pretty */
  private var formatInfos: Option[FormatInfos] = None

  private def showSrcCodeDialog(): FormatSrc = //Niets aanpassen
    val dialog = new SrcFormatDialog
    val selectOpt: Option[ButtonType] = dialog.showAndWait()
    selectOpt.map {
      case dialog.onelineBtn => Oneline
      case dialog.prettyBtn  => Pretty
      case _                 => Pretty
    } getOrElse (Pretty)

  private def showScaleDialog(): Try[Int] = //Niets aanpassen
    val dialog = Dialogs.newScaleDialog()
    val scaleOp: Option[List[Int]] = dialog.showAndWait()
    scaleOp
      .map(_.head)
      .filter(x => x >= minScaleFactor && x <= maxScaleFactor) match
      case Some(x) => Success(x)
      case _ =>
        Failure(
          UserInputException("Specify a valid scale-factor between 1 and 100!")
        )

  private def chooseModelDialog(xs: List[Model]): Model =
    if xs.size > 1 then
      val names = xs.map(_.name)
      val dialog: ChoiceDialog[String] =
        new ChoiceDialog(names.head, names.asJava)
      val opt: Option[String] = dialog.showAndWait()
      opt match
        case Some(name) => xs.find(_.name == name).get
        case _          => chooseModelDialog(xs)
    else xs.head

  private def parseFile(path: Path): Try[SrcFile] = //Zouden verwijderd kunnen worden met gebruik van json\
    val parser = ModelicaParserLike()
    for modelList <- parser.parse(path)
    yield
      val model = chooseModelDialog(modelList)
      SrcFile(path, Left(model))

  private def parseFileExc(path: Path): SrcFile = //Zouden verwijderd kunnen worden met gebruik van json
    parseFile(path).get

  /** Let the user choose a modelica file; parses this file and returns the path
    * to the file, coordinate-system bounds & the shapes of the modelica model.
    */
  def openFile: Try[(Path, Point, List[Any])] =
    val chooser = Dialogs.newJsonFileChooser()
    chooser.setTitle("Open..")

    val fileTry = Option(chooser.showOpenDialog(owner)) match
      case Some(x) => Success(x)
      case _ => Failure(UserInputException("Select a JSON file to open!"))
    for
      file <- fileTry
      path = Paths.get(file.toURI)
      (point, shapes) <- openFile(path)
    yield (path, point, shapes)

  def openFile(path: Path): Try[(Point, List[Any])] =
    val existsTry =
      if Files.exists(path) then Success(path)
      else Failure(UserInputException(s"$path doesn't exist!"))

    val parsedJson: (Double, Double, List[Any]) = JsonCtrl.open_json_file(JsonCtrl.path_conversion(path))

    val width: Double = parsedJson(0)
    val height: Double = parsedJson(1)
    val shapes: List[Any] = parsedJson(2)

    for
      _ <- existsTry
      scaleFactor <- showScaleDialog() // Optional scaling factor dialog
    yield
      val scaledCanvasSize = (width * scaleFactor, height * scaleFactor)
      openedFile = Some(SrcFile(path, Right(JsonModel()))) // Placeholder for actual tracking logic
      formatInfos = Some(FormatInfos(scaleFactor, None))

      (scaledCanvasSize, shapes)

  /** Warns the user that the given SrcFile got changed from another program and
    * let the user decide if he wants to reparse the file or cancel the
    * operation.
    *   - If the user wants to reparse the file, the file is reparsed and the
    *     returned SrcFile is the reparsed filecontent. (openedFile variable
    *     wasn't changed)
    *   - If the user chooses cancel None is returned
    */
  private def warnExternalChanges(src: SrcFile): Option[SrcFile] =
    val dialog = new ExternalChangesDialog(src.file.toString)
    val selectedOption: Option[ButtonType] = dialog.showAndWait()
    selectedOption.filter { _ == dialog.overwriteAnnotationsBtn }.map { _ =>
      parseFile(src.file)
    } flatMap {
      case Success(src) =>
        Some(src)
      case Failure(ex) =>
        Dialogs
          .newExceptionDialog(ex, "Error while reparsing file")
          .showAndWait()
        None
    }

  /** Check for external file changes and react on it before calling `f`. */
  private def awareExternalChanges[A](
      srcOpt: Option[SrcFile]
  )(f: Option[SrcFile] => Try[A]): Try[A] =
    srcOpt match
      case Some(src) if src.noExternalChanges =>
        f(srcOpt) // no changes; just call f
      case Some(src) =>
       f(srcOpt) //Changed, cause we dont use parsing
      case None => f(srcOpt) // no opened file; just call f


  /** Saves the icon represented by the shapes and their width, height to an
    * existing file. If there is no existing file the user get asked to save a
    * new file.
    */
  def saveFile(shapes: List[Any],width: Double, height: Double): Try[Path] =

    //shapes is de lijst met alle figuren en hun positie en grootes
    //de width en height zijn de afmetingen van de canvas

    awareExternalChanges(openedFile) { newFile =>
      val codeGen = generateCodeAndWriteToFile(shapes, width, height)
      (newFile, formatInfos) match
        case (
              Some(src @ SrcFile(filepath, modelAst)),
              Some(FormatInfos(pxPerMm, Some(format)))
            ) => // file was opened & saved before
          println("file was opened & saved before")
          saveAsFile(shapes, width, height, Some(filepath))
          val newSrc =
            parseFileExc(filepath) // reparse for getting new positional values
          openedFile = Some(newSrc) // update timestamp
          Success(filepath)
        case (
              Some(src @ SrcFile(filepath, modelAst)),
              Some(FormatInfos(pxPerMm, None))
            ) => // file was opened but not saved before; we need a formating
          println("file was opened but not saved before; we need a formating")
          saveAsFile(shapes, width, height, Some(filepath))
        case (None, None) => // never saved this file; we need all informations
          saveAsFile(shapes, width, height)
        case _ =>
          println(
            s"Developer WARNING: saveFile() both None: $openedFile $formatInfos"
          )
          Failure(
            new IllegalStateException(
              "Internal state crashed! Reopen file and try again."
            )
          )
    }

  def saveAsFile(
        shapes: List[Any],
        width: Double,
        height: Double,
        givenPath: Option[Path] = None
      ): Try[Path] = {
    val codeGen = generateCodeAndWriteToFile(shapes, width, height) _

    // Determine the file path to use
    val fileTry = givenPath match {
      case Some(path) => Success(path) // Use the provided path
      case None => // Show file chooser if no path is provided
        val chooser = Dialogs.newJsonFileChooser()
        chooser.setTitle("Save as..")
        Option(chooser.showSaveDialog(owner)) match {
          case Some(file) => Success(Paths.get(file.toURI))
          case None => Failure(UserInputException("Select a file for saving!"))
        }
    }

    // Process the file saving logic
    for {
      file <- fileTry
      pxPerMm <- showScaleDialog()
    } yield {
      // Deleted Parsing (Not needed for JSON)
      codeGen(Right(file), pxPerMm)
      file
    }
  }

  private def generateCodeAndWriteToFile(
    shapes: List[Any],
    width: Double,
    height: Double
  )(srcEither: Either[SrcFile, Path], pxPerMm: Int): Unit =

    //Deleted the ModelicaGenerator (Not needed anymore)

    srcEither match
      case Left(src) => //Writes to an existing file
      //Still dont know when it ever gets here

      case Right(filepath) => //Writes to a new file
        JsonCtrl.save_json_file(shapes, width, height, JsonCtrl.path_conversion(filepath))

  /** Exports the given Icon represented by the given shapes and width,height
    * into an user-selected svg-file
    */
  def exportAsSvg( //This is a image
      shapes: List[Node],
      width: Double,
      height: Double
  ): Try[Unit] =
  
    val chooser = Dialogs.newSvgFileChooser()
    chooser.setTitle(fontBundle.getString("export.svg"))
    val fileTry = Option(chooser.showSaveDialog(owner)) match
      case Some(x) => Success(x)
      case _       => Failure(UserInputException("Select a file for export!"))
    for
      file <- fileTry
      path = Paths.get(file.toURI)
    yield
      val generator = new SvgCodeGenerator
      val str = generator.generatePrettyPrinted(shapes, width, height)
      generator.writeToFile(str)(path)

  /** Exports the given Icon represented by the given shapes and width,height
    * into an user-selected png-file
    */
  def exportAsBitmap(root: Node): Try[Unit] = //This is a image
    val chooser = Dialogs.newPngFileChooser()
    chooser.setTitle(fontBundle.getString("export.jpg"))
    val fileTry = Option(chooser.showSaveDialog(owner)) match
      case Some(x) => Success(x)
      case _       => Failure(UserInputException("Select a file for export!"))
    for
      file <- fileTry
      image = root.snapshot(null, null)
      filename = file.getName
      suffix = filename.substring(filename.lastIndexOf(".") + 1)
      _ <- Try(
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), suffix, file)
      )
    yield ()

  /** Lets the user pick an image and returns the URI of the selected file */
  def openImage: Option[URI] =
    val chooser = Dialogs.newBitmapFileChooser()
    chooser.setTitle(fontBundle.getString("open.image"))
    val fileOp = Option(chooser.showOpenDialog(owner))
    fileOp map { file =>
      file.toURI
    }
