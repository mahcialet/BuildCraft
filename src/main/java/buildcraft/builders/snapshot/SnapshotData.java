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
                           List<BlockState> palette, List<Integer> blocks, String name,
                           boolean rotate, boolean excavate, boolean allowCreative, boolean creativeOnly) {
    public static final int MAX_VOLUME = 64 * 64 * 64;
    public static final Codec<SnapshotData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SnapshotKind.CODEC.fieldOf("kind").forGetter(SnapshotData::kind),
            BlockPos.CODEC.fieldOf("size").forGetter(SnapshotData::size),
            Direction.CODEC.fieldOf("facing").forGetter(SnapshotData::facing),
            BlockPos.CODEC.fieldOf("offset").forGetter(SnapshotData::offset),
            BlockState.CODEC.listOf().fieldOf("palette").forGetter(SnapshotData::palette),
            Codec.INT.listOf().fieldOf("blocks").forGetter(SnapshotData::blocks),
            Codec.STRING.fieldOf("name").forGetter(SnapshotData::name),
            Codec.BOOL.optionalFieldOf("rotate", true).forGetter(SnapshotData::rotate),
            Codec.BOOL.optionalFieldOf("excavate", true).forGetter(SnapshotData::excavate),
            Codec.BOOL.optionalFieldOf("allow_creative", false).forGetter(SnapshotData::allowCreative),
            Codec.BOOL.optionalFieldOf("creative_only", false).forGetter(SnapshotData::creativeOnly)
        ).apply(instance, SnapshotData::new));

    public SnapshotData(SnapshotKind kind, BlockPos size, Direction facing, BlockPos offset,
                        List<BlockState> palette, List<Integer> blocks, String name) {
        this(kind, size, facing, offset, palette, blocks, name, true, true, false, false);
    }

    public SnapshotData(SnapshotKind kind, BlockPos size, Direction facing, BlockPos offset,
                        List<BlockState> palette, List<Integer> blocks, String name,
                        boolean rotate, boolean excavate, boolean allowCreative) {
        this(kind, size, facing, offset, palette, blocks, name, rotate, excavate, allowCreative, false);
    }

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
        return new SnapshotData(kind, size, facing, min.subtract(machinePos), palette, blocks, name,
                true, true, false, false);
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

    public BlockState rotatedStateAt(BlockPos local, Rotation rotation) {
        return stateAt(local).rotate(rotation);
    }

    public SnapshotData normalized(Rotation rotation) {
        if (rotation == Rotation.NONE) return this;
        BlockPos min = null;
        BlockPos max = null;
        for (int index = 0; index < blocks.size(); index++) {
            BlockPos rotated = offset.offset(local(index)).rotate(rotation);
            min = min == null ? rotated : new BlockPos(Math.min(min.getX(), rotated.getX()),
                    Math.min(min.getY(), rotated.getY()), Math.min(min.getZ(), rotated.getZ()));
            max = max == null ? rotated : new BlockPos(Math.max(max.getX(), rotated.getX()),
                    Math.max(max.getY(), rotated.getY()), Math.max(max.getZ(), rotated.getZ()));
        }
        if (min == null || max == null) return this;
        BlockPos normalizedSize = max.subtract(min).offset(1, 1, 1);
        List<BlockState> normalizedPalette = new ArrayList<>();
        normalizedPalette.add(Blocks.AIR.defaultBlockState());
        List<Integer> normalizedBlocks = new ArrayList<>(java.util.Collections.nCopies(blocks.size(), 0));
        for (int index = 0; index < blocks.size(); index++) {
            BlockPos target = offset.offset(local(index)).rotate(rotation).subtract(min);
            BlockState state = palette.get(blocks.get(index)).rotate(rotation);
            int paletteIndex = normalizedPalette.indexOf(state);
            if (paletteIndex < 0) { paletteIndex = normalizedPalette.size(); normalizedPalette.add(state); }
            int targetIndex = ((target.getZ() * normalizedSize.getY()) + target.getY()) * normalizedSize.getX()
                    + target.getX();
            normalizedBlocks.set(targetIndex, paletteIndex);
        }
        return new SnapshotData(kind, normalizedSize, Direction.WEST, min, normalizedPalette, normalizedBlocks,
                name, rotate, excavate, allowCreative, creativeOnly);
    }

    private BlockPos local(int index) {
        int x = index % size.getX();
        int y = (index / size.getX()) % size.getY();
        int z = index / (size.getX() * size.getY());
        return new BlockPos(x, y, z);
    }
}
