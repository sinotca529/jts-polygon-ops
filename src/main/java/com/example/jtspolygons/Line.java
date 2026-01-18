
package com.example.jtspolygons;

import java.util.Optional;

import org.locationtech.jts.geom.CoordinateXY;

public final record Line(double a, double b, double c) {

  private static final double EPS = 1e-12;

  // p1 から p2 までの線分 (ただし p1 を含み、 p2 を含まない) とこの直線との交点を得る。
  Optional<CoordinateXY> crossPoint(CoordinateXY p1, CoordinateXY p2) {
    // 線分上の点は p1 + t(p2 - p1) t \in [0, 1) と表せる。
    // x座標 : x1 + t dx
    // y座標 : y1 + t dy
    // これを ax + by = c に代入すると次の通り。
    // a(x1 + t dx) + b(y1 + t dy) = c
    // t について整理して次を得る。
    // t(a dx + b dy) = c - a x1 - b y1
    // ^^^^^^^^^^^^^    ^^^^^^^^^^^^^^^
    //     denom            nom
    //
    double dx = p2.x - p1.x;
    double dy = p2.y - p1.y;

    double denom = a * dx + b * dy;
    double nom = c - a * p1.x - b * p1.y;

    boolean parallel = Math.abs(denom) < EPS;
    if (parallel)
      return Optional.empty();

    double t = nom / denom;
    if (0 <= t && t < 1)
      return Optional.of(new CoordinateXY(p1.x + t * dx, p1.y + t * dy));
    else
      return Optional.empty();
  }
}
