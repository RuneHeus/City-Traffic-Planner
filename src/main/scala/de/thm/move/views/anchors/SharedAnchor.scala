package de.thm.move.views.anchors

import de.thm.move.views.shapes.{ResizableLine, SelectionRectangle}
import javafx.scene.text.Text

/**
 * Represents a shared anchor point that can connect and move multiple lines together.
 *
 * @param x the x-coordinate of the anchor
 * @param y the y-coordinate of the anchor
 */
class SharedAnchor(x: Double, y: Double) extends Anchor(x, y):
  this.getStyleClass.addAll("sharedAnchor")

  /** A list of listeners to be notified when the anchor's connections change. */
  private var connectionListeners: List[() => Unit] = List()

  /** A visual rectangle to indicate selection of the anchor. */
  val selectionRectangle = new SelectionRectangle(this)
  hideSelectionRectangle() // Ensure the rectangle is initially hidden

  /** An optional traffic light associated with this anchor. */
  private var trafficLight: Option[Text] = None

  /**
   * List of lines connected to this anchor and their respective indices.
   * Each tuple contains a reference to the `ResizableLine` and an index:
   *  - `0` for the start of the line
   *  - `1` for the end of the line
   */
  var connectedLines: List[(ResizableLine, Int)] = List()

  /**
   * Associates a traffic light with this anchor.
   *
   * @param icon The traffic light icon to associate.
   */
  def setTrafficLight(icon: Text): Unit = trafficLight = Some(icon)

  /**
   * Retrieves the current traffic light associated with the anchor.
   *
   * @return An `Option` containing the traffic light, if one exists.
   */
  def getTrafficLight: Option[Text] = trafficLight

  /**
   * Removes the traffic light associated with the anchor, if any.
   *
   * This method hides the traffic light and clears its association with the anchor.
   */
  def removeTrafficLight(): Unit =
    trafficLight.foreach(_.setVisible(false))
    trafficLight = None

  /**
   * Displays the selection rectangle around the anchor.
   */
  def showSelectionRectangle(): Unit =
    selectionRectangle.setVisible(true)

  /**
   * Hides the selection rectangle.
   */
  def hideSelectionRectangle(): Unit =
    selectionRectangle.setVisible(false)

  /**
   * Returns the current position of the anchor.
   *
   * @return A tuple `(x, y)` representing the anchor's position.
   */
  def getPosition: (Double, Double) =
    (getLayoutX, getLayoutY)

  /**
   * Adds a connection between this anchor and a line.
   *
   * @param line the `ResizableLine` to connect
   * @param idx  the index of the connection (`0` for start, `1` for end)
   * @throws IllegalArgumentException if `idx` is not 0 or 1
   */
  def addConnection(line: ResizableLine, idx: Int): Unit =
    require(idx == 0 || idx == 1, "Index must be 0 (start) or 1 (end)")
    require(!connectedLines.contains((line, idx)), "Connection already exists")

    val initialSize = connectedLines.size
    connectedLines ::= (line, idx)
    notifyConnectionChange()
    assert(connectedLines.size == initialSize + 1, "Connection was not added correctly")

  /**
   * Removes a specific connection to a line from the shared anchor.
   *
   * @param line The `ResizableLine` to be removed from the list of connections.
   */
  def removeConnection(line: ResizableLine): Unit =
    connectedLines = connectedLines.filterNot(_._1 == line)
    notifyConnectionChange()

  /**
   * Checks if the shared anchor has any connections to other lines.
   *
   * @return `true` if there are connected lines, otherwise `false`.
   */
  def hasConnections: Boolean =
    connectedLines.nonEmpty

  /**
   * Moves all lines connected to this anchor by a specified delta.
   *
   * @param delta a tuple `(dx, dy)` specifying the change in coordinates
   */
  def moveWithSharedAnchor(delta: (Double, Double)): Unit =
    connectedLines.foreach { case (line, idx) =>
      line.resize(idx, delta)
    }

  /**
   * Checks if this anchor is connected to the same line as another anchor.
   *
   * @param other the other `SharedAnchor` to check against
   * @return `true` if the anchors share at least one line, `false` otherwise
   */
  def isConnectedToLine(other: SharedAnchor): Boolean =
    connectedLines.exists((line, _) =>
      other.connectedLines.exists(_._1 == line)
    )

  /**
   * Adds dynamic movement listeners to all attached elements (e.g., traffic lights).
   *
   * @param updateFunction A function to execute whenever the anchor's position changes.
   */
  def addDynamicListener(updateFunction: () => Unit): Unit =
    layoutXProperty().addListener((_, _, _) => updateFunction())
    layoutYProperty().addListener((_, _, _) => updateFunction())
    centerXProperty().addListener((_, _, _) => updateFunction())
    centerYProperty().addListener((_, _, _) => updateFunction())

  /**
   * Adds a listener to be notified when connections change.
   *
   * @param listener A function to invoke when the anchor's connections change.
   */
  def addConnectionChangeListener(listener: () => Unit): Unit =
    connectionListeners = listener :: connectionListeners

  /**
   * Removes a specific connection change listener.
   *
   * @param listener The listener to remove.
   */
  def removeConnectionChangeListener(listener: () => Unit): Unit =
    connectionListeners = connectionListeners.filterNot(_ == listener)

  /**
   * Notifies all registered listeners of a connection change.
   */
  private def notifyConnectionChange(): Unit =
    connectionListeners.foreach(_())