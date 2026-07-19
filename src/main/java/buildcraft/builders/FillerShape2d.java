package buildcraft.builders;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/** Rasterises the historical Filler 2D outlines and extrudes them along one axis. */
public final class FillerShape2d {
    private FillerShape2d() {}

    public static Mask create(FillerPattern pattern, BlockPos min, BlockPos max,
                              Direction.Axis axis, int rotation, boolean hollow) {
        int sizeA = switch (axis) {
            case X -> max.getY() - min.getY() + 1;
            case Y, Z -> max.getX() - min.getX() + 1;
        };
        int sizeB = switch (axis) {
            case X, Y -> max.getZ() - min.getZ() + 1;
            case Z -> max.getY() - min.getY() + 1;
        };
        boolean[][] values = new boolean[sizeA][sizeB];
        int maxA = sizeA - 1;
        int maxB = sizeB - 1;
        int normalized = Math.floorMod(rotation, 4);
        int shapeMaxA = normalized % 2 == 0 ? maxA : maxB;
        int shapeMaxB = normalized % 2 == 0 ? maxB : maxA;
        Set<Long> outline = new HashSet<>();
        Shape shape = new Shape((a, b) -> {
            int outA;
            int outB;
            switch (normalized) {
                case 1 -> { outA = maxA - b; outB = a; }
                case 2 -> { outA = maxA - a; outB = maxB - b; }
                case 3 -> { outA = b; outB = maxB - a; }
                default -> { outA = a; outB = b; }
            }
            if (outA >= 0 && outA <= maxA && outB >= 0 && outB <= maxB) outline.add(point(outA, outB));
        });
        generate(pattern, shapeMaxA, shapeMaxB, shape);
        Set<Long> included = outline;
        if (!hollow && shape.fillA >= 0) {
            int fillA;
            int fillB;
            switch (normalized) {
                case 1 -> { fillA = maxA - shape.fillB; fillB = shape.fillA; }
                case 2 -> { fillA = maxA - shape.fillA; fillB = maxB - shape.fillB; }
                case 3 -> { fillA = shape.fillB; fillB = maxB - shape.fillA; }
                default -> { fillA = shape.fillA; fillB = shape.fillB; }
            }
            included = new HashSet<>(outline);
            floodAll(fillA, fillB, maxA, maxB, outline, included);
        }
        for (long packed : included) {
            int a = (int) (packed >> 32);
            int b = (int) packed;
            values[a][b] = true;
        }
        return new Mask(axis, min, values);
    }

    private static void floodAll(int fillA, int fillB, int maxA, int maxB,
                                 Set<Long> outline, Set<Long> included) {
        ArrayDeque<Long> open = new ArrayDeque<>();
        open.add(point(fillA, fillB));
        while (!open.isEmpty()) {
            long packed = open.removeFirst();
            int a = (int) (packed >> 32);
            int b = (int) packed;
            if (a < 0 || a > maxA || b < 0 || b > maxB || outline.contains(packed) || !included.add(packed)) continue;
            open.add(point(a + 1, b)); open.add(point(a - 1, b));
            open.add(point(a, b + 1)); open.add(point(a, b - 1));
        }
    }

    public static final class Mask {
        private final Direction.Axis axis;
        private final BlockPos min;
        private final boolean[][] values;

        private Mask(Direction.Axis axis, BlockPos min, boolean[][] values) {
            this.axis = axis;
            this.min = min;
            this.values = values;
        }

        public boolean includes(BlockPos pos) {
            int a = switch (axis) {
                case X -> pos.getY() - min.getY();
                case Y, Z -> pos.getX() - min.getX();
            };
            int b = switch (axis) {
                case X, Y -> pos.getZ() - min.getZ();
                case Z -> pos.getY() - min.getY();
            };
            return a >= 0 && a < values.length && b >= 0 && b < values[a].length && values[a][b];
        }
    }

    public static boolean includes(FillerPattern pattern, BlockPos pos, BlockPos min, BlockPos max,
                                   Direction.Axis axis, int rotation, boolean hollow) {
        int actualA;
        int actualB;
        int maxA;
        int maxB;
        switch (axis) {
            case X -> {
                actualA = pos.getY() - min.getY(); actualB = pos.getZ() - min.getZ();
                maxA = max.getY() - min.getY(); maxB = max.getZ() - min.getZ();
            }
            case Y -> {
                actualA = pos.getX() - min.getX(); actualB = pos.getZ() - min.getZ();
                maxA = max.getX() - min.getX(); maxB = max.getZ() - min.getZ();
            }
            case Z -> {
                actualA = pos.getX() - min.getX(); actualB = pos.getY() - min.getY();
                maxA = max.getX() - min.getX(); maxB = max.getY() - min.getY();
            }
            default -> throw new IllegalStateException("Unknown axis " + axis);
        }

        Set<Long> outline = new HashSet<>();
        int normalized = Math.floorMod(rotation, 4);
        int shapeMaxA = normalized % 2 == 0 ? maxA : maxB;
        int shapeMaxB = normalized % 2 == 0 ? maxB : maxA;
        Plotter plotter = (a, b) -> {
            int outA;
            int outB;
            switch (normalized) {
                case 1 -> { outA = maxA - b; outB = a; }
                case 2 -> { outA = maxA - a; outB = maxB - b; }
                case 3 -> { outA = b; outB = maxB - a; }
                default -> { outA = a; outB = b; }
            }
            if (outA >= 0 && outA <= maxA && outB >= 0 && outB <= maxB) outline.add(point(outA, outB));
        };
        Shape shape = new Shape(plotter);
        generate(pattern, shapeMaxA, shapeMaxB, shape);
        if (hollow || shape.fillA < 0) return outline.contains(point(actualA, actualB));

        int fillA;
        int fillB;
        switch (normalized) {
            case 1 -> { fillA = maxA - shape.fillB; fillB = shape.fillA; }
            case 2 -> { fillA = maxA - shape.fillA; fillB = maxB - shape.fillB; }
            case 3 -> { fillA = shape.fillB; fillB = maxB - shape.fillA; }
            default -> { fillA = shape.fillA; fillB = shape.fillB; }
        }
        return floodContains(actualA, actualB, fillA, fillB, maxA, maxB, outline);
    }

    private static boolean floodContains(int targetA, int targetB, int fillA, int fillB,
                                         int maxA, int maxB, Set<Long> outline) {
        long target = point(targetA, targetB);
        if (outline.contains(target)) return true;
        ArrayDeque<Long> open = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        open.add(point(fillA, fillB));
        while (!open.isEmpty()) {
            long packed = open.removeFirst();
            int a = (int) (packed >> 32);
            int b = (int) packed;
            if (a < 0 || a > maxA || b < 0 || b > maxB || outline.contains(packed) || !visited.add(packed)) continue;
            if (packed == target) return true;
            open.add(point(a + 1, b)); open.add(point(a - 1, b));
            open.add(point(a, b + 1)); open.add(point(a, b - 1));
        }
        return false;
    }

    private static void generate(FillerPattern pattern, int maxA, int maxB, Shape shape) {
        switch (pattern) {
            case ARC -> {
                if (maxA == 0 || maxB == 0) shape.line(0, 0, maxA, maxB);
                else { shape.fill(maxA, maxB); shape.arc(maxA, maxB, maxA, maxB, 0, 0, ArcType.ARC); }
            }
            case CIRCLE -> {
                if (maxA == 0 || maxB == 0) shape.line(0, 0, maxA, maxB);
                else {
                    int halfA = maxA / 2; int halfB = maxB / 2;
                    shape.fill(halfA, halfB);
                    shape.arc(halfA, halfB, maxA / 2.0, maxB / 2.0,
                        maxA - 2 * halfA, maxB - 2 * halfB, ArcType.FULL);
                }
            }
            case HEXAGON -> {
                int indent = maxA / 4; int halfB = maxB / 2;
                shape.path(indent, 0, maxA - indent, 0, maxA, halfB,
                    maxA - indent, maxB, indent, maxB, 0, maxB - halfB, 0, halfB, indent, 0);
                shape.fill(maxA / 2, maxB / 2);
            }
            case OCTAGON -> {
                int indentA = clippedIndent(maxA); int indentB = clippedIndent(maxB);
                shape.path(indentA, 0, maxA - indentA, 0, maxA, indentB, maxA, maxB - indentB,
                    maxA - indentA, maxB, indentA, maxB, 0, maxB - indentB, 0, indentB, indentA, 0);
                shape.fill(maxA / 2, maxB / 2);
            }
            case PENTAGON -> {
                int halfA = maxA / 2;
                int indentA = (int) Math.round(maxA * Math.sin(Math.toRadians(18)));
                double vertical = Math.cos(Math.toRadians(54)) / Math.cos(Math.toRadians(18));
                int indentB = (int) Math.round(maxB * vertical);
                shape.path(indentA, 0, maxA - indentA, 0, maxA, indentB,
                    maxA - halfA, maxB, halfA, maxB, 0, indentB, indentA, 0);
                shape.fill(halfA, maxB / 2);
            }
            case SEMICIRCLE -> {
                if (maxA == 0 || maxB == 0) shape.line(0, 0, maxA, maxB);
                else {
                    int halfA = maxA / 2;
                    shape.fill(halfA, maxB);
                    shape.arc(halfA, maxB, maxA / 2.0, maxB, maxA - 2 * halfA, 0, ArcType.SEMI);
                }
            }
            case SQUARE -> {
                shape.path(0, 0, maxA, 0, maxA, maxB, 0, maxB, 0, 0);
                shape.fill(maxA / 2, maxB / 2);
            }
            case TRIANGLE -> {
                int halfA = maxA / 2;
                shape.path(maxA, maxB, 0, maxB, halfA, 0, maxA - halfA, 0, maxA, maxB);
                shape.fill(halfA, maxB / 2);
            }
            default -> throw new IllegalArgumentException("Not a 2D shape: " + pattern);
        }
    }

    private static int clippedIndent(int max) {
        if (max < 2) return 0;
        return Math.max(1, Math.min(max / 2, (int) Math.round(max * (1.0 - 1.0 / Math.sqrt(2.0)))));
    }

    private static long point(int a, int b) { return ((long) a << 32) | (b & 0xffffffffL); }

    @FunctionalInterface
    private interface Plotter { void plot(int a, int b); }
    private enum ArcType { ARC, SEMI, FULL }

    private static final class Shape {
        private final Plotter plotter;
        private int fillA = -1;
        private int fillB = -1;

        private Shape(Plotter plotter) { this.plotter = plotter; }
        private void fill(int a, int b) { fillA = a; fillB = b; }

        private void path(int... points) {
            for (int i = 0; i + 3 < points.length; i += 2) line(points[i], points[i + 1], points[i + 2], points[i + 3]);
        }

        private void line(int a1, int b1, int a2, int b2) {
            int da = Math.abs(a2 - a1); int db = Math.abs(b2 - b1);
            int stepA = Integer.compare(a2, a1); int stepB = Integer.compare(b2, b1);
            int error = da - db;
            while (true) {
                plotter.plot(a1, b1);
                if (a1 == a2 && b1 == b2) return;
                int twice = error * 2;
                if (twice > -db) { error -= db; a1 += stepA; }
                if (twice < da) { error += da; b1 += stepB; }
            }
        }

        private void arc(int ca, int cb, double ra, double rb, int da, int db, ArcType type) {
            double ra2 = ra * ra; double rb2 = rb * rb;
            double sigma = 2 * rb2 + ra2 * (1 - 2 * rb);
            for (int a = 0, b = (int) rb; rb2 * a <= ra2 * b; a++) {
                arcPoints(ca, cb, a, b, da, db, type);
                if (sigma >= 0) { sigma += 4 * ra2 * (1 - b); b--; }
                sigma += rb2 * (4 * a + 6);
            }
            sigma = 2 * ra2 + rb2 * (1 - 2 * ra);
            for (int a = (int) ra, b = 0; ra2 * b <= rb2 * a; b++) {
                arcPoints(ca, cb, a, b, da, db, type);
                if (sigma >= 0) { sigma += 4 * rb2 * (1 - a); a--; }
                sigma += ra2 * (4 * b + 6);
            }
        }

        private void arcPoints(int ca, int cb, int a, int b, int da, int db, ArcType type) {
            plotter.plot(ca - a, cb - b);
            if (type != ArcType.ARC) {
                plotter.plot(ca + a + da, cb - b);
                if (type == ArcType.FULL) {
                    plotter.plot(ca - a, cb + b + db);
                    plotter.plot(ca + a + da, cb + b + db);
                }
            }
        }
    }
}
