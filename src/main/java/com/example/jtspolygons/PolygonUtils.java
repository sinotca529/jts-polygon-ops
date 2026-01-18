package com.example.jtspolygons;

import org.locationtech.jts.geom.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * JTSライブラリを用いて多角形を構築および合成するためのユーティリティメソッドです。
 *
 * <p>
 * このクラスでは、一次不等式の集合や明示的な頂点リストから多角形を生成したり、
 * 複数の多角形を和集合（足し合わせ）や積集合（共通部分）で合成したり、
 * 多角形から頂点座標を抽出したり、多角形を簡易なSVG表現へ変換するメソッドを提供します。
 * 実装ではJTSの基本的な型のみを使用しており、JTSコアライブラリ以外の依存関係はありません。
 * </p>
 */
public final class PolygonUtils {
    /**
     * インスタンス化を防ぐための非公開コンストラクタです。
     */
    private PolygonUtils() {
    }

    /**
     * 頂点リストから多角形を構築します。頂点の順序は多角形の境界に沿っている必要があります。
     * 最後の頂点は最初の頂点を繰り返す必要はありません。メソッド内で自動的に閉じられます。
     *
     * @param vertices 多角形の境界を定義する {@link Coordinate} オブジェクトのリスト
     * @return 指定された頂点から構築された JTS の {@link Polygon}
     */
    public static Polygon fromVertices(List<CoordinateXY> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return new GeometryFactory().createPolygon();
        }

        int n = vertices.size();
        Coordinate[] coords = new Coordinate[n + 1];
        for (int i = 0; i < n; i++) {
            coords[i] = new Coordinate(vertices.get(i));
        }
        // 最初の頂点を繰り返して輪を閉じます
        coords[n] = new Coordinate(vertices.get(0));
        return new GeometryFactory().createPolygon(coords);
    }

    /**
     * 一次不等式の集合から凸多角形を構築。
     *
     * <p>
     * 内部では非常に大きな境界ボックスを用いて無限平面を近似し、
     * 全ての半平面と交差させていくことで最終的な凸多角形を求めます。交差領域が空になると空のポリゴンが返されます。
     * </p>
     *
     * @param constraints 半平面を表す {@link HalfPlane} のリスト。各要素が null の場合は無視されます。
     * @return すべての半平面の交差領域を表すJTSの {@link Polygon}。領域が空の場合は空の多角形を返します。
     */
    public static Optional<Polygon> fromHalfPlanes(List<HalfPlane> constraints) {
        // 巨大な四角形を用意
        final double BOUND = 1e9;
        List<CoordinateXY> poly = new ArrayList<>();
        poly.add(new CoordinateXY(-BOUND, -BOUND));
        poly.add(new CoordinateXY(BOUND, -BOUND));
        poly.add(new CoordinateXY(BOUND, BOUND));
        poly.add(new CoordinateXY(-BOUND, BOUND));

        if (constraints == null)
            return Optional.empty();

        for (final var c : constraints) {
            if (c == null)
                return Optional.empty();

            poly = clipPolygonAgainstHalfPlane(poly, c);
            if (poly.isEmpty())
                return Optional.empty();
        }

        return Optional.of(fromVertices(poly));
    }

    /**
     * 複数の多角形の和集合（合成）を計算します。和集合は、すべての入力ジオメトリに含まれる点を含む単一のジオメトリになります。
     * 2つ以上の多角形が与えられた場合は、順に結合していきます。
     *
     * @param geos 結合するジオメトリのリスト
     * @return 全てのジオメトリの和集合
     */
    public static Geometry union(List<Geometry> geos) {
        GeometryFactory gf = new GeometryFactory();
        return geos.stream()
                .reduce(gf.createGeometryCollection(), Geometry::union);
    }

    /**
     * 複数の多角形の積集合（共通部分）を計算します。積集合は、すべての入力ジオメトリに共通する領域になります。
     * 計算途中で結果が空になった場合はそこで処理を終了します。
     *
     * @param geometries 交差させるジオメトリのリスト
     * @return 全てのジオメトリの共通部分
     */
    public static Geometry intersection(List<Geometry> geos) {
        return geos.stream()
                .reduce(Geometry::intersection)
                .orElseGet(() -> new GeometryFactory().createGeometryCollection());
    }

    /**
     * ジオメトリに含まれるすべての多角形境界の座標列を抽出します。
     * 単純な多角形の場合、返されるリストには外周のリングを表す1つの座標列が含まれます。
     * 複数の多角形や穴を持つ多角形の場合は、外周と内周のそれぞれのリングに対する座標列が含まれます。
     * 各座標列の最後の座標は最初の座標を繰り返して閉じたことを示します（JTSの慣例に従います）。
     *
     * @param geometry 一つ以上の多角形を含む可能性のあるジオメトリ
     * @return 座標リストのリスト。各内側のリストが1つのリングを表します
     */
    public static List<List<Coordinate>> getVertices(Geometry geometry) {
        List<List<Coordinate>> result = new ArrayList<>();
        if (geometry == null || geometry.isEmpty()) {
            return result;
        }
        // マルチポリゴンやジオメトリコレクションを分解します
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            Geometry g = geometry.getGeometryN(i);
            if (g instanceof Polygon) {
                Polygon poly = (Polygon) g;
                // 外周リング
                addRing(poly.getExteriorRing().getCoordinates(), result);
                // 内周リング（穴）
                for (int j = 0; j < poly.getNumInteriorRing(); j++) {
                    addRing(poly.getInteriorRingN(j).getCoordinates(), result);
                }
            }
        }
        return result;
    }

    // 座標配列を結果リストに追加するためのヘルパー
    private static void addRing(Coordinate[] coords, List<List<Coordinate>> result) {
        List<Coordinate> ring = new ArrayList<>();
        for (Coordinate c : coords) {
            ring.add(new Coordinate(c));
        }
        result.add(ring);
    }

    /**
     * ジオメトリを基本的なSVG表現に変換します。
     * このメソッドが生成するSVGは単一の {@code <svg>}
     * 要素を含み、そのviewBoxはジオメトリのバウンディングボックスの大きさに合わせられます。
     * 多角形の輪郭を表す複数の {@code <path>} 要素が含まれます。
     * 座標は、最小のxとyの値がviewBoxの原点になるように平行移動され、y軸を反転させることで、正のyが上方向になるように描画されます。
     * ポリゴンは塗りなしで黒い線として描画されます。
     *
     * @param geometry 変換するジオメトリ
     * @return ジオメトリを表すSVG文字列
     */
    public static String toSVG(Geometry geometry) {
        if (geometry == null || geometry.isEmpty()) {
            return "";
        }
        Envelope env = geometry.getEnvelopeInternal();
        double minX = env.getMinX();
        double maxX = env.getMaxX();
        double minY = env.getMinY();
        double maxY = env.getMaxY();
        double width = maxX - minX;
        double height = maxY - minY;
        if (width == 0 || height == 0) {
            // 退化したジオメトリの場合は空文字列を返す
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ")
                .append(width).append(' ').append(height).append("'>\n");
        sb.append("  <g stroke='black' fill='none'>\n");
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            Geometry g = geometry.getGeometryN(i);
            if (g instanceof Polygon) {
                Polygon poly = (Polygon) g;
                // 外周リング
                sb.append(pathForRing(poly.getExteriorRing().getCoordinates(), minX, maxY));
                // 内周リング（穴）
                for (int j = 0; j < poly.getNumInteriorRing(); j++) {
                    sb.append(pathForRing(poly.getInteriorRingN(j).getCoordinates(), minX, maxY));
                }
            }
        }
        sb.append("  </g>\n</svg>");
        return sb.toString();
    }

    // 座標リングからSVGのpath要素を構築します。
    // 座標はminXを基準に平行移動し、y方向はmaxYを基準に反転して
    // SVGの原点がバウンディングボックスの左下に来るようにします。
    private static String pathForRing(Coordinate[] coords, double minX, double maxY) {
        StringBuilder sb = new StringBuilder();
        if (coords.length < 2) {
            return "";
        }
        sb.append("    <path d='");
        for (int i = 0; i < coords.length; i++) {
            Coordinate c = coords[i];
            double x = c.x - minX;
            double y = maxY - c.y;
            if (i == 0) {
                sb.append('M').append(x).append(' ').append(y);
            } else {
                sb.append('L').append(x).append(' ').append(y);
            }
            if (i < coords.length - 1) {
                sb.append(' ');
            }
        }
        sb.append(" z' />\n");
        return sb.toString();
    }

    // a*x + b*y <= c という形の制約で表される半平面に対して凸多角形をクリップします。
    // Sutherland–Hodgman アルゴリズムにより新しい凸多角形を生成します。
    // 境界上の点は保持されます。多角形が半平面の外側に完全に位置する場合は空のリストを返します。
    private static List<CoordinateXY> clipPolygonAgainstHalfPlane(List<CoordinateXY> poly,
            HalfPlane hp) {
        List<CoordinateXY> output = new ArrayList<>();
        int n = poly.size();

        final var border = hp.border();
        for (int i = 0; i < n; i++) {
            final var p1 = poly.get(i);
            final var p2 = poly.get((i + 1) % n);
            final var cp = border.crossPoint(p1, p2);

            // [p1, p2) が境界を跨いだなら交点を追加
            cp.ifPresent(output::add);
            // p2 が境界内なら終点を追加
            if (hp.contains(p2)) output.add(p2);
        }
        return output;
    }

    public static Ranges xDomainIntervals(Geometry geometry) {
        Ranges rs = Ranges.empty();
        if (geometry == null || geometry.isEmpty())
            return rs;

        Deque<Geometry> stack = new ArrayDeque<>();
        stack.push(geometry);

        while (!stack.isEmpty()) {
            Geometry g = stack.pop();
            if (g == null || g.isEmpty())
                continue;
            if (g instanceof GeometryCollection) {
                for (int i = 0; i < g.getNumGeometries(); i++) {
                    stack.push(g.getGeometryN(i));
                }
            } else {
                Envelope e = g.getEnvelopeInternal();
                rs = rs.union(Ranges.of(e.getMinX(), e.getMaxX()));
            }
        }

        return rs;
    }
}
