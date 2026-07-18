package buildcraft.core.marker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One ordered path-marker chain. The order is significant to path consumers. */
public final class PathConnection {
    public static final Codec<PathConnection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.listOf().fieldOf("positions").forGetter(PathConnection::positions),
        Codec.BOOL.optionalFieldOf("loop", false).forGetter(PathConnection::loop)
    ).apply(instance, PathConnection::new));

    private final ArrayList<BlockPos> positions;
    private boolean loop;

    public PathConnection(List<BlockPos> positions, boolean loop) {
        if (positions.size() < 2) {
            throw new IllegalArgumentException("A path connection requires at least two markers");
        }
        if (positions.stream().distinct().count() != positions.size()) {
            throw new IllegalArgumentException("A path connection cannot contain duplicate markers");
        }
        this.positions = new ArrayList<>(positions);
        this.loop = loop;
    }

    public List<BlockPos> positions() {
        return List.copyOf(positions);
    }

    public boolean loop() {
        return loop;
    }

    public boolean contains(BlockPos pos) {
        return positions.contains(pos);
    }

    public boolean isEndpoint(BlockPos pos) {
        return !loop && (positions.getFirst().equals(pos) || positions.getLast().equals(pos));
    }

    public void reverse() {
        Collections.reverse(positions);
    }

    /** Removes a marker, returning the zero, one, or two surviving chains. */
    public List<PathConnection> without(BlockPos pos) {
        int index = positions.indexOf(pos);
        if (index < 0) {
            return List.of(this);
        }
        if (loop) {
            ArrayList<BlockPos> opened = new ArrayList<>(positions.size() - 1);
            for (int offset = 1; offset < positions.size(); offset++) {
                opened.add(positions.get((index + offset) % positions.size()));
            }
            return opened.size() >= 2 ? List.of(new PathConnection(opened, false)) : List.of();
        }
        ArrayList<PathConnection> survivors = new ArrayList<>(2);
        addIfConnection(survivors, positions.subList(0, index));
        addIfConnection(survivors, positions.subList(index + 1, positions.size()));
        return List.copyOf(survivors);
    }

    private static void addIfConnection(List<PathConnection> output, List<BlockPos> positions) {
        if (positions.size() >= 2) {
            output.add(new PathConnection(positions, false));
        }
    }

    void prepend(BlockPos pos) {
        positions.addFirst(pos);
    }

    void append(BlockPos pos) {
        positions.addLast(pos);
    }

    void appendAll(List<BlockPos> other) {
        positions.addAll(other);
    }

    void closeLoop() {
        loop = true;
    }
}
