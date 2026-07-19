package buildcraft.builders;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum FillerPattern implements StringRepresentable {
    NONE, CLEAR, FILL, BOX, FRAME, PYRAMID, STAIRS,
    SPHERE, HEMISPHERE, QUARTER_SPHERE, EIGHTH_SPHERE,
    ARC, CIRCLE, HEXAGON, OCTAGON, PENTAGON, SEMICIRCLE, SQUARE, TRIANGLE;

    public static final Codec<FillerPattern> CODEC = StringRepresentable.fromEnum(FillerPattern::values);

    public boolean includes(BlockPos pos, BlockPos min, BlockPos max) {
        if (this == NONE || this == PYRAMID || this == STAIRS || isSphere() || isShape2d()) return false;
        if (this == CLEAR || this == FILL) return true;
        int boundaries = (pos.getX() == min.getX() || pos.getX() == max.getX() ? 1 : 0)
                + (pos.getY() == min.getY() || pos.getY() == max.getY() ? 1 : 0)
                + (pos.getZ() == min.getZ() || pos.getZ() == max.getZ() ? 1 : 0);
        return this == BOX ? boundaries >= 1 : boundaries >= 2;
    }

    public boolean clears() { return this == CLEAR; }

    public boolean isSphere() {
        return this == SPHERE || this == HEMISPHERE || this == QUARTER_SPHERE
                || this == EIGHTH_SPHERE;
    }

    public boolean isShape2d() {
        return switch (this) {
            case ARC, CIRCLE, HEXAGON, OCTAGON, PENTAGON, SEMICIRCLE, SQUARE, TRIANGLE -> true;
            default -> false;
        };
    }

    public int openFaces() {
        return switch (this) {
            case HEMISPHERE -> 1;
            case QUARTER_SPHERE -> 2;
            case EIGHTH_SPHERE -> 3;
            default -> 0;
        };
    }

    @Override public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
