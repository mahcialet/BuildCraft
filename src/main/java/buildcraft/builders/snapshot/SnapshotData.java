package buildcraft.builders.snapshot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Portable inline representation shared by Architect, Builder, and Replacer. */
public record SnapshotData(SnapshotKind kind, BlockPos size, Direction facing, BlockPos offset,
                           List<BlockState> palette, List<Integer> blocks, String name) {
    public static final int MAX_VOLUME = 64 * 64 * 64;
    public static final Codec<SnapshotData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SnapshotKind.CODEC.fieldOf("kind").forGetter(SnapshotData::kind),
            BlockPos.CODEC.fieldOf("size").forGetter(SnapshotData::size),
            Direction.CODEC.fieldOf("facing").forGetter(SnapshotData::facing),
            BlockPos.CODEC.fieldOf("offset").forGetter(SnapshotData::offset),
            BlockState.CODEC.listOf().fieldOf("palette").forGetter(SnapshotData::palette),
            Codec.INT.listOf().fieldOf("blocks").forGetter(SnapshotData::blocks),
            Codec.STRING.fieldOf("name").forGetter(SnapshotData::name)
    ).apply(instance, SnapshotData::new));

    public SnapshotData {
        palette = List.copyOf(palette);
        blocks = List.copyOf(blocks);
        name = name == null ? "" : name;
    }

    public static SnapshotData capture(Level level, BlockPos min, BlockPos max, SnapshotKind kind,
                                       Direction facing, BlockPos machinePos, String name) {
        BlockPos size = max.subtract(min).offset(1, 1, 1);
        long volumeLong = (long) size.getX() * size.getY() * size.getZ();
        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0
                || size.getX() > 64 || size.getY() > 64 || size.getZ() > 64 || volumeLong > MAX_VOLUME) {
            throw new IllegalArgumentException("Snapshot bounds must be ordered and no larger than 64 cubed");
        }
        int volume = (int) volumeLong;
        List<BlockState> palette = new ArrayList<>();
        palette.add(Blocks.AIR.defaultBlockState());
        List<Integer> blocks = new ArrayList<>(volume);
        for (int z = min.getZ(); z <= max.getZ(); z++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (kind == SnapshotKind.TEMPLATE) state = state.isAir()
                            ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    int paletteIndex = palette.indexOf(state);
                    if (paletteIndex < 0) {
                        paletteIndex = palette.size();
                        palette.add(state);
                    }
                    blocks.add(paletteIndex);
                }
            }
        }
        return new SnapshotData(kind, size, facing, min.subtract(machinePos), palette, blocks, name);
    }

    public boolean valid() {
        long volume = (long) size.getX() * size.getY() * size.getZ();
        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0
                || size.getX() > 64 || size.getY() > 64 || size.getZ() > 64
                || volume > MAX_VOLUME || blocks.size() != volume || palette.isEmpty()) return false;
        return blocks.stream().allMatch(index -> index >= 0 && index < palette.size());
    }

    public BlockState stateAt(BlockPos local) {
        if (local.getX() < 0 || local.getY() < 0 || local.getZ() < 0
                || local.getX() >= size.getX() || local.getY() >= size.getY() || local.getZ() >= size.getZ()) {
            throw new IndexOutOfBoundsException(local.toString());
        }
        int index = ((local.getZ() * size.getY()) + local.getY()) * size.getX() + local.getX();
        return palette.get(blocks.get(index));
    }

    public BlockPos worldPosition(BlockPos builderPos, BlockPos local, Rotation rotation) {
        return builderPos.offset(offset.rotate(rotation)).offset(local.rotate(rotation));
    }
}
