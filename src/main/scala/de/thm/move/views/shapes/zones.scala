package de.thm.move.views.shapes

class residentialZone(override val points: List[Double]) extends ResizablePolygon(points)

class commercialZone(override val points: List[Double]) extends ResizablePolygon(points) 

class industrialZone(override val points: List[Double]) extends ResizablePolygon(points)