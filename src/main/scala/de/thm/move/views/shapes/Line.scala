package de.thm.move.views.shapes


//Normal line class representing y = mx + b
class Line(val x1: Double, val y1: Double, val x2: Double, val y2: Double) {
  var slope: Double = 0
  var b: Double = 0
  //slope can be infinite, so put this as zero so we don't devide by zero.
  if(x2 - x1 == 0)
    then
    slope = Double.MaxValue
    b = 0
    else
      slope = (y2 - y1) / (x2 - x1)
      b = y1 - (slope * x1)
}
