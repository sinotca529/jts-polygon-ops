package com.example.jtspolygons;

import org.locationtech.jts.geom.CoordinateXY;

/**
 * 一次不等式 a*x + b*y <= c を表します。
 */
public final record HalfPlane(double a, double b, double c) {

  private static final double EPS = 1e-12;

  public boolean contains(double x, double y) {
    return a * x + b * y <= c + EPS;
  }

  public boolean contains(CoordinateXY p) {
    return a * p.getX() + b * p.getY() <= c + EPS;
  }

  /** 線分と境界線 a*x + b*y = c の交点（なければ null） */
  public CoordinateXY intersectSegment(CoordinateXY p1, CoordinateXY p2, double eps) {
    double dx = p2.x - p1.x;
    double dy = p2.y - p1.y;
    double denom = a * dx + b * dy;
    if (Math.abs(denom) < eps)
      return null;

    double t = (c - a * p1.x - b * p1.y) / denom;
    if (t < -eps || t > 1 + eps)
      return null;

    return new CoordinateXY(p1.x + t * dx, p1.y + t * dy);
  }

  public Line border() {
    return new Line(a, b, c);
  }

}
