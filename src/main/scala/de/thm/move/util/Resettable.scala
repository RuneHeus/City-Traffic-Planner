package de.thm.move.util

/** Used by some drawing strategies that need to reset their state */
trait Resettable:
  def reset(): Unit
