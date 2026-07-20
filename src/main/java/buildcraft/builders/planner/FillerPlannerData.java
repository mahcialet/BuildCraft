package buildcraft.builders.planner;

import buildcraft.builders.FillerPattern;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;

/** Persistent configuration of a filler preview attached to a volume box. */
public record FillerPlannerData(FillerPattern pattern, boolean inverted, Direction verticalDirection,
                                Direction horizontalDirection, int pyramidCenter, boolean hollow,
                                Direction sphereFacing, int sphereRotation, Direction.Axis shapeAxis,
                                int shapeRotation) {
    public static final Codec<FillerPlannerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.xmap(FillerPattern::valueOf, FillerPattern::name).fieldOf("pattern").forGetter(FillerPlannerData::pattern),
        Codec.BOOL.optionalFieldOf("inverted", false).forGetter(FillerPlannerData::inverted),
        Direction.CODEC.optionalFieldOf("vertical_direction", Direction.UP).forGetter(FillerPlannerData::verticalDirection),
        Direction.CODEC.optionalFieldOf("horizontal_direction", Direction.EAST).forGetter(FillerPlannerData::horizontalDirection),
        Codec.intRange(0, 8).optionalFieldOf("pyramid_center", 4).forGetter(FillerPlannerData::pyramidCenter),
        Codec.BOOL.optionalFieldOf("hollow", false).forGetter(FillerPlannerData::hollow),
        Direction.CODEC.optionalFieldOf("sphere_facing", Direction.DOWN).forGetter(FillerPlannerData::sphereFacing),
        Codec.intRange(0, 3).optionalFieldOf("sphere_rotation", 0).forGetter(FillerPlannerData::sphereRotation),
        Direction.Axis.CODEC.optionalFieldOf("shape_axis", Direction.Axis.Y).forGetter(FillerPlannerData::shapeAxis),
        Codec.intRange(0, 3).optionalFieldOf("shape_rotation", 0).forGetter(FillerPlannerData::shapeRotation)
    ).apply(instance, FillerPlannerData::new));

    public static FillerPlannerData defaults() {
        return new FillerPlannerData(FillerPattern.FILL, false, Direction.UP, Direction.EAST, 4,
            false, Direction.DOWN, 0, Direction.Axis.Y, 0);
    }

    public FillerPlannerData pattern(FillerPattern value) { return new FillerPlannerData(value, inverted, verticalDirection, horizontalDirection, pyramidCenter, hollow, sphereFacing, sphereRotation, shapeAxis, shapeRotation); }
    public FillerPlannerData inverted(boolean value) { return new FillerPlannerData(pattern, value, verticalDirection, horizontalDirection, pyramidCenter, hollow, sphereFacing, sphereRotation, shapeAxis, shapeRotation); }
    public FillerPlannerData vertical(Direction value) { return new FillerPlannerData(pattern, inverted, value, horizontalDirection, pyramidCenter, hollow, sphereFacing, sphereRotation, shapeAxis, shapeRotation); }
    public FillerPlannerData horizontal(Direction value) { return new FillerPlannerData(pattern, inverted, verticalDirection, value, pyramidCenter, hollow, sphereFacing, sphereRotation, shapeAxis, shapeRotation); }
    public FillerPlannerData center(int value) { return new FillerPlannerData(pattern, inverted, verticalDirection, horizontalDirection, value, hollow, sphereFacing, sphereRotation, shapeAxis, shapeRotation); }
    public FillerPlannerData hollow(boolean value) { return new FillerPlannerData(pattern, inverted, verticalDirection, horizontalDirection, pyramidCenter, value, sphereFacing, sphereRotation, shapeAxis, shapeRotation); }
    public FillerPlannerData sphereFacing(Direction value) { return new FillerPlannerData(pattern, inverted, verticalDirection, horizontalDirection, pyramidCenter, hollow, value, sphereRotation, shapeAxis, shapeRotation); }
    public FillerPlannerData sphereRotation(int value) { return new FillerPlannerData(pattern, inverted, verticalDirection, horizontalDirection, pyramidCenter, hollow, sphereFacing, Math.floorMod(value, 4), shapeAxis, shapeRotation); }
    public FillerPlannerData shapeAxis(Direction.Axis value) { return new FillerPlannerData(pattern, inverted, verticalDirection, horizontalDirection, pyramidCenter, hollow, sphereFacing, sphereRotation, value, shapeRotation); }
    public FillerPlannerData shapeRotation(int value) { return new FillerPlannerData(pattern, inverted, verticalDirection, horizontalDirection, pyramidCenter, hollow, sphereFacing, sphereRotation, shapeAxis, Math.floorMod(value, 4)); }
}
