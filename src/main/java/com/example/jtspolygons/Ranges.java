package com.example.jtspolygons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Ranges {
  private static final double EPS = 1e-12;

  private final List<Range> ranges;

  private Ranges(List<Range> normalized) {
    this.ranges = List.copyOf(normalized);
  }

  public static Ranges empty() {
    return new Ranges(List.of());
  }

  public static Ranges of(double min, double max) {
    return normalized(List.of(new Range(min, max)));
  }

  public List<double[]> toList() {
    var out = new ArrayList<double[]>(ranges.size());
    for (var r : ranges)
      out.add(new double[] { r.min, r.max });
    return out;
  }

  public boolean isEmpty() {
    return ranges.isEmpty();
  }

  public Ranges union(Ranges other) {
    var ranges = new ArrayList<Range>(this.ranges.size() + other.ranges.size());
    ranges.addAll(this.ranges);
    ranges.addAll(other.ranges);
    return normalized(ranges);
  }

  public Ranges intersect(Ranges other) {
    var a = this.ranges;
    var b = other.ranges;
    var out = new ArrayList<Range>();

    int i = 0;
    int j = 0;
    while (i < a.size() && j < b.size()) {
      var ra = a.get(i);
      var rb = b.get(j);

      double lo = Math.max(ra.min, rb.min);
      double hi = Math.min(ra.max, rb.max);
      if (lo <= hi + EPS)
        out.add(new Range(lo, hi));

      if (ra.max < rb.max)
        i++;
      else
        j++;
    }
    return normalized(out);
  }

  /** 差集合（this \ other） */
  public Ranges subtract(Ranges other) {
    var a = this.ranges;
    var b = other.ranges;
    int i = 0, j = 0;
    var out = new ArrayList<Range>();

    while (i < a.size()) {
      var ra = a.get(i);

      // b を ra に影響するところまで進める
      while (j < b.size() && b.get(j).max < ra.min - EPS)
        j++;

      double cur = ra.min;
      int jj = j;

      while (jj < b.size() && b.get(jj).min <= ra.max + EPS) {
        var rb = b.get(jj);

        // rb が [cur, ra.max] を削る
        if (rb.min > cur + EPS) {
          out.add(new Range(cur, Math.min(ra.max, rb.min)));
        }
        cur = Math.max(cur, rb.max);

        if (cur >= ra.max - EPS)
          break;
        jj++;
      }

      if (cur < ra.max - EPS) {
        out.add(new Range(cur, ra.max));
      }

      i++;
    }
    return normalized(out);
  }

  // ---- 内部 ----

  private static Ranges normalized(List<Range> input) {
    if (input.isEmpty())
      return empty();

    var sorted = new ArrayList<>(input);
    sorted.sort(Comparator.comparingDouble(r -> r.min));

    var out = new ArrayList<Range>();
    Range cur = sorted.get(0);

    for (int i = 1; i < sorted.size(); i++) {
      var nxt = sorted.get(i);
      if (nxt.min <= cur.max + EPS) {
        cur = new Range(cur.min, Math.max(cur.max, nxt.max));
      } else {
        out.add(cur);
        cur = nxt;
      }
    }
    out.add(cur);
    return new Ranges(out);
  }

  private static record Range(double min, double max) {
    Range {
      if (Double.isNaN(min) || Double.isNaN(max))
        throw new IllegalArgumentException("NaNは禁止");
      if (min > max)
        throw new IllegalArgumentException("min > max");
    }
  }
}
