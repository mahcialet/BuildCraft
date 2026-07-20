package buildcraft.builders;

import buildcraft.builders.block.entity.FillerBlockEntity;
import buildcraft.silicon.gate.GateAction;
import java.util.List;
import net.minecraft.core.Direction;

/** Historical Filler statement actions and their typed parameters. */
public final class BuildersGateActions {
    private BuildersGateActions() {}

    public static FillerPattern pattern(GateAction action) {
        if (!action.name().startsWith("FILLER_")) return null;
        return FillerPattern.valueOf(action.name().substring("FILLER_".length()));
    }

    public static int optionCount(GateAction action) {
        FillerPattern pattern = pattern(action);
        if (pattern == null) return 0;
        if (pattern == FillerPattern.PYRAMID || pattern == FillerPattern.STAIRS) return 2;
        if (pattern == FillerPattern.SPHERE) return 1;
        if (pattern.openFaces() > 0 || pattern.isShape2d()) return 3;
        return 0;
    }

    public static int optionSize(GateAction action, int index) {
        FillerPattern pattern = pattern(action);
        if (pattern == null) return 0;
        if (pattern == FillerPattern.PYRAMID) return index == 0 ? 2 : index == 1 ? 9 : 0;
        if (pattern == FillerPattern.STAIRS) return index == 0 ? 2 : index == 1 ? 4 : 0;
        if (pattern == FillerPattern.SPHERE) return index == 0 ? 3 : 0;
        if (pattern.openFaces() > 0) return switch (index) { case 0 -> 3; case 1 -> 6; case 2 -> 4; default -> 0; };
        if (pattern.isShape2d()) return switch (index) { case 0, 1 -> 3; case 2 -> 4; default -> 0; };
        return 0;
    }

    public static int option(GateAction action, List<Integer> options, int index) {
        int size = optionSize(action, index);
        int fallback = pattern(action) != null && pattern(action).isShape2d() && index == 1 ? 2 : 0;
        return size == 0 ? 0 : Math.floorMod(index < options.size() ? options.get(index) : fallback, size);
    }

    public static String optionLabel(GateAction action, List<Integer> options, int index) {
        int value = option(action, options, index);
        FillerPattern pattern = pattern(action);
        if (pattern == FillerPattern.PYRAMID && index == 1) return Integer.toString(value + 1);
        if ((pattern == FillerPattern.PYRAMID || pattern == FillerPattern.STAIRS) && index == 0)
            return value == 0 ? "U" : "D";
        if (pattern == FillerPattern.STAIRS && index == 1) return new String[] {"E", "S", "W", "N"}[value];
        if ((pattern != null && pattern.isSphere()) && index == 0) return new String[] {"I", "O", "H"}[value];
        if (pattern != null && pattern.openFaces() > 0 && index == 1)
            return new String[] {"D", "U", "N", "S", "W", "E"}[value];
        if (pattern != null && pattern.isShape2d() && index == 0) return new String[] {"X", "Y", "Z"}[value];
        if (pattern != null && pattern.isShape2d() && index == 1) return new String[] {"I", "O", "H"}[value];
        return "R" + value;
    }

    public static void apply(FillerBlockEntity filler, GateAction action, List<Integer> options) {
        FillerPattern pattern = pattern(action);
        if (pattern == null) return;
        filler.setPattern(pattern);
        if (pattern == FillerPattern.PYRAMID) {
            filler.setVerticalDirection(option(action, options, 0) == 0 ? Direction.UP : Direction.DOWN);
            filler.setPyramidCenter(option(action, options, 1));
        } else if (pattern == FillerPattern.STAIRS) {
            filler.setVerticalDirection(option(action, options, 0) == 0 ? Direction.UP : Direction.DOWN);
            filler.setHorizontalDirection(new Direction[] {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH}[option(action, options, 1)]);
        } else if (pattern.isSphere()) {
            filler.setFillMode(FillerFillMode.values()[option(action, options, 0)]);
            if (pattern.openFaces() > 0) {
                filler.setSphereFacing(Direction.values()[option(action, options, 1)]);
                filler.setSphereRotation(option(action, options, 2));
            }
        } else if (pattern.isShape2d()) {
            filler.setShapeAxis(Direction.Axis.values()[option(action, options, 0)]);
            filler.setFillMode(FillerFillMode.values()[option(action, options, 1)]);
            filler.setShapeRotation(option(action, options, 2));
        }
    }
}
