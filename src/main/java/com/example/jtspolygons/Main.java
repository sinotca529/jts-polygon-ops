package com.example.jtspolygons;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.List;

/**
 * PolygonUtilsの機能を試す簡単なデモプログラムです。
 * 半平面の係数による多角形生成、頂点リストからの多角形生成、
 * 和集合および積集合の計算、頂点リストの抽出、SVG変換を行います。
 */
public class Main {
    /**
     * エントリポイント。各ユーティリティメソッドの使い方を示すサンプルコードを実行します。
     *
     * @param args コマンドライン引数（使用しません）
     */
    public static void main(String[] args) {
        // 三角形を表す半平面制約を定義します（x >= 0, y >= 0, x + y <= 10）。
        List<HalfPlane> constraints = new ArrayList<>();
        constraints.add(new HalfPlane(-1, 0, 0));
        constraints.add(new HalfPlane(0, -1, 0));
        constraints.add(new HalfPlane(1, 1, 10));
        Polygon triangle = PolygonUtils.fromHalfPlanes(constraints).get();

        // 長方形を表す頂点のリストを定義します（(0,0), (3,0), (3,2), (0,2)）。
        List<CoordinateXY> rectVertices = new ArrayList<>();
        rectVertices.add(new CoordinateXY(0, 0));
        rectVertices.add(new CoordinateXY(6, 0));
        rectVertices.add(new CoordinateXY(6, 5));
        rectVertices.add(new CoordinateXY(0, 5));
        Polygon rectangle = PolygonUtils.fromVertices(rectVertices);

        // 三角形と長方形の和集合および積集合を計算します。
        List<Geometry> shapes = new ArrayList<>();
        shapes.add(triangle);
        shapes.add(rectangle);
        Geometry union = PolygonUtils.union(shapes);
        Geometry intersection = PolygonUtils.intersection(shapes);

        // 三角形の頂点を表示します
        System.out.println("三角形の頂点:");
        for (List<Coordinate> ring : PolygonUtils.getVertices(triangle)) {
            for (Coordinate c : ring) {
                System.out.println(c.x + ", " + c.y);
            }
            System.out.println("---");
        }

        // 長方形の頂点を表示します
        System.out.println("長方形の頂点:");
        for (List<Coordinate> ring : PolygonUtils.getVertices(rectangle)) {
            for (Coordinate c : ring) {
                System.out.println(c.x + ", " + c.y);
            }
            System.out.println("---");
        }

        // 和集合の頂点を表示します
        System.out.println("和集合の頂点:");
        for (List<Coordinate> ring : PolygonUtils.getVertices(union)) {
            for (Coordinate c : ring) {
                System.out.println(c.x + ", " + c.y);
            }
            System.out.println("---");
        }

        // 積集合の頂点を表示します
        System.out.println("積集合の頂点:");
        for (List<Coordinate> ring : PolygonUtils.getVertices(intersection)) {
            for (Coordinate c : ring) {
                System.out.println(c.x + ", " + c.y);
            }
            System.out.println("---");
        }

        // SVG 文字列を生成して表示します
        System.out.println("三角形のSVG:\n" + PolygonUtils.toSVG(triangle));
        System.out.println("長方形のSVG:\n" + PolygonUtils.toSVG(rectangle));
        System.out.println("和集合のSVG:\n" + PolygonUtils.toSVG(union));
        System.out.println("積集合のSVG:\n" + PolygonUtils.toSVG(intersection));
    }
}
