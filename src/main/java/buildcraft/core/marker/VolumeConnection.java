package buildcraft.core.marker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Unordered marker set defining the inclusive corners of an axis-aligned volume. */
public final class VolumeConnection {
    public static final Codec<VolumeConnection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.listOf().fieldOf("positions").forGetter(VolumeConnection::positions)
    ).apply(instance, VolumeConnection::new));

    private final LinkedHashSet<BlockPos> positions;

    public VolumeConnection(Collection<BlockPos> positions) {
        if (positions.size() < 2) throw new IllegalArgumentException("A volume connection requires two markers");
        this.positions = new LinkedHashSet<>(positions);
        if (this.positions.size() != positions.size()) throw new IllegalArgumentException("Duplicate volume marker");
    }

    public List<BlockPos> positions() {
        return List.copyOf(positions);
    }

    public boolean contains(BlockPos pos) {
        return positions.contains(pos);
    }

    public BlockPos min() {
        int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE, z = Integer.MAX_VALUE;
        for (BlockPos pos : positions) {
            x = Math.min(x, pos.getX()); y = Math.min(y, pos.getY()); z = Math.min(z, pos.getZ());
        }
        return new BlockPos(x, y, z);
    }

    public BlockPos max() {
        int x = Integer.MIN_VALUE, y = Integer.MIN_VALUE, z = Integer.MIN_VALUE;
        for (BlockPos pos : positions) {
            x = Math.max(x, pos.getX()); y = Math.max(y, pos.getY()); z = Math.max(z, pos.getZ());
        }
        return new BlockPos(x, y, z);
    }

    public EnumSet<Direction.Axis> connectedAxes() {
        EnumSet<Direction.Axis> axes = EnumSet.noneOf(Direction.Axis.class);
        for (BlockPos a : positions) for (BlockPos b : positions) {
            Direction.Axis axis = alignedAxis(a, b);
            if (axis != null) axes.add(axis);
        }
        return axes;
    }

    public boolean isCorner(BlockPos pos) {
        BlockPos min = min();
        BlockPos max = max();
        return coordinateAtEnd(pos.getX(), min.getX(), max.getX())
            && coordinateAtEnd(pos.getY(), min.getY(), max.getY())
            && coordinateAtEnd(pos.getZ(), min.getZ(), max.getZ());
    }

    private static boolean coordinateAtEnd(int value, int min, int max) {
        return value == min || value == max;
    }

    public boolean canAdd(BlockPos to) {
        if (positions.contains(to)) return false;
        Set<Direction.Axis> taken = connectedAxes();
        for (BlockPos from : positions) {
            Direction.Axis axis = alignedAxis(from, to);
            if (axis != null && !taken.contains(axis)) return true;
        }
        return isCorner(to);
    }

    public boolean canMerge(VolumeConnection other) {
        EnumSet<Direction.Axis> ours = connectedAxes();
        EnumSet<Direction.Axis> theirs = other.connectedAxes();
        if (ours.size() != 1 || theirs.size() != 1 || ours.equals(theirs)) return false;
        EnumSet<Direction.Axis> blocked = EnumSet.copyOf(ours);
        blocked.addAll(theirs);
        for (BlockPos from : positions) for (BlockPos to : other.positions) {
            Direction.Axis axis = alignedAxis(from, to);
            if (axis != null && !blocked.contains(axis)) return true;
        }
        return false;
    }

    void add(BlockPos pos) {
        positions.add(pos.immutable());
    }

    void addAll(VolumeConnection other) {
        positions.addAll(other.positions);
    }

    List<VolumeConnection> without(BlockPos pos) {
        ArrayList<BlockPos> remaining = new ArrayList<>(positions);
        remaining.remove(pos);
        return remaining.size() < 2 ? List.of() : List.of(new VolumeConnection(remaining));
    }

    public static Direction.Axis alignedAxis(BlockPos from, BlockPos to) {
        int differences = 0;
        Direction.Axis axis = null;
        if (from.getX() != to.getX()) { differences++; axis = Direction.Axis.X; }
        if (from.getY() != to.getY()) { differences++; axis = Direction.Axis.Y; }
        if (from.getZ() != to.getZ()) { differences++; axis = Direction.Axis.Z; }
        return differences == 1 ? axis : null;
    }
}
