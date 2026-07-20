package buildcraft.builders.block.entity;

import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.BCBuildersItems;
import buildcraft.builders.block.ArchitectTableBlock;
import buildcraft.builders.snapshot.SnapshotData;
import buildcraft.builders.snapshot.SnapshotKind;
import buildcraft.core.marker.VolumeBox;
import buildcraft.core.marker.VolumeBoxSavedData;
import buildcraft.core.marker.VolumeConnection;
import buildcraft.core.marker.VolumeSavedData;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public final class ArchitectTableBlockEntity extends buildcraft.core.block.entity.OwnedBlockEntity {
    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(2);
    private BlockPos areaMin;
    private BlockPos areaMax;
    private SnapshotKind scanningKind;
    private int cursor;
    private List<BlockState> palette = new ArrayList<>();
    private List<Integer> blocks = new ArrayList<>();
    private List<BlockPos> linkedMarkers = new ArrayList<>();
    private String blueprintName = "Blueprint";
    private boolean rotate = true;
    private boolean excavate = true;
    private boolean allowCreative;
    private boolean scanningCreativeOnly;
    private long lastNetworkSync = -100;
    private int lastClientCursor = Integer.MIN_VALUE;

    public ArchitectTableBlockEntity(BlockPos pos, BlockState state) {
        super(BCBuildersBlockEntities.ARCHITECT_TABLE.get(), pos, state);
    }

    public ItemStacksResourceHandler inventory() { return inventory; }
    public BlockPos areaMin() { return areaMin; }
    public BlockPos areaMax() { return areaMax; }
    public int cursor() { return cursor; }
    public SnapshotKind scanningKind() { return scanningKind; }
    public int volumeSize() { return areaMin == null || areaMax == null ? 0 : volume(); }
    public List<BlockPos> linkedMarkers() { return List.copyOf(linkedMarkers); }
    public String blueprintName() { return blueprintName; }
    public boolean rotate() { return rotate; }
    public boolean excavate() { return excavate; }
    public boolean allowCreative() { return allowCreative; }
    public void setBlueprintName(String value) {
        String sanitized = value == null ? "" : value.strip();
        if (sanitized.length() > 32) sanitized = sanitized.substring(0, 32);
        if (!blueprintName.equals(sanitized)) { blueprintName = sanitized; sync(); }
    }
    public void toggleRotate() { rotate = !rotate; resetScan(); sync(); }
    public void toggleExcavate() { excavate = !excavate; resetScan(); sync(); }
    public void toggleAllowCreative() { allowCreative = !allowCreative; resetScan(); sync(); }
    public boolean addLinkedMarker(BlockPos markerPos) {
        if (!(level != null && level.getBlockEntity(markerPos) instanceof ConstructionMarkerBlockEntity marker)
                || marker.snapshot() == null || linkedMarkers.contains(markerPos)) return false;
        BlockPos min = areaMin == null ? marker.snapshotMin() : new BlockPos(
                Math.min(areaMin.getX(), marker.snapshotMin().getX()), Math.min(areaMin.getY(), marker.snapshotMin().getY()),
                Math.min(areaMin.getZ(), marker.snapshotMin().getZ()));
        BlockPos max = areaMax == null ? marker.snapshotMax() : new BlockPos(
                Math.max(areaMax.getX(), marker.snapshotMax().getX()), Math.max(areaMax.getY(), marker.snapshotMax().getY()),
                Math.max(areaMax.getZ(), marker.snapshotMax().getZ()));
        BlockPos size = max.subtract(min).offset(1, 1, 1);
        long volume = (long) size.getX() * size.getY() * size.getZ();
        if (size.getX() > 64 || size.getY() > 64 || size.getZ() > 64 || volume > SnapshotData.MAX_VOLUME) return false;
        linkedMarkers.add(markerPos.immutable());
        sync();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ArchitectTableBlockEntity table) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        table.refreshArea(serverLevel);
        table.scan(state);
        table.syncScanProgress(serverLevel);
    }

    private void syncScanProgress(ServerLevel level) {
        if (cursor == lastClientCursor) return;
        if (!buildcraft.core.BCCoreConfig.networkUpdateDue(level.getGameTime(), lastNetworkSync,
                buildcraft.core.BCCoreConfig.NETWORK_UPDATE_RATE.get(), false)) return;
        lastClientCursor = cursor;
        lastNetworkSync = level.getGameTime();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public boolean configureArea(BlockPos min, BlockPos max) {
        long sx = (long) max.getX() - min.getX() + 1;
        long sy = (long) max.getY() - min.getY() + 1;
        long sz = (long) max.getZ() - min.getZ() + 1;
        if (sx <= 0 || sy <= 0 || sz <= 0 || sx > 64 || sy > 64 || sz > 64
                || sx * sy * sz > SnapshotData.MAX_VOLUME) return false;
        if (min.equals(areaMin) && max.equals(areaMax)) return true;
        areaMin = min.immutable();
        areaMax = max.immutable();
        resetScan();
        sync();
        return true;
    }

    private void refreshArea(ServerLevel level) {
        if (areaMin != null) return;
        VolumeSavedData markers = VolumeSavedData.get(level);
        VolumeBoxSavedData boxes = VolumeBoxSavedData.get(level);
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = worldPosition.relative(direction);
            VolumeConnection connection = markers.connectionAt(adjacent).orElse(null);
            if (connection != null && configureArea(connection.min(), connection.max())) return;
            VolumeBox box = boxes.boxAt(adjacent).orElse(null);
            if (box != null && configureArea(box.min(), box.max())) return;
        }
    }

    private void scan(BlockState machineState) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)
                || areaMin == null || areaMax == null || inventory.getAmountAsLong(1) > 0) return;
        ItemResource input = inventory.getResource(0);
        if (input.isEmpty() || inventory.getAmountAsLong(0) <= 0) {
            resetScan();
            return;
        }
        SnapshotKind kind;
        if (input.getItem() == BCBuildersItems.BLUEPRINT.get()) kind = SnapshotKind.BLUEPRINT;
        else if (input.getItem() == BCBuildersItems.TEMPLATE.get()) kind = SnapshotKind.TEMPLATE;
        else { resetScan(); return; }
        if (buildcraft.builders.item.SnapshotItem.hasSnapshot(input.toStack(1))) return;
        if (scanningKind != kind) {
            resetScan();
            scanningKind = kind;
            palette.add(Blocks.AIR.defaultBlockState());
        }

        int volume = volume();
        int perTick = kind == SnapshotKind.BLUEPRINT ? 10 : 100;
        for (int scanned = 0; scanned < perTick && cursor < volume; scanned++, cursor++) {
            BlockPos target = position(cursor);
            BlockState captured = level.getBlockState(target);
            if (kind == SnapshotKind.BLUEPRINT && isCreativeOnly(captured)) {
                if (allowCreative) scanningCreativeOnly = true;
                else captured = Blocks.AIR.defaultBlockState();
            }
            if (kind == SnapshotKind.TEMPLATE) captured = captured.isAir()
                    ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
            int paletteIndex = palette.indexOf(captured);
            if (paletteIndex < 0) {
                paletteIndex = palette.size();
                palette.add(captured);
            }
            blocks.add(paletteIndex);
        }
        if (cursor < volume) { setChanged(); return; }

        Direction facing = machineState.hasProperty(ArchitectTableBlock.FACING)
                ? machineState.getValue(ArchitectTableBlock.FACING) : Direction.NORTH;
        String name = blueprintName.isBlank() ? (kind == SnapshotKind.BLUEPRINT ? "Blueprint" : "Template") : blueprintName;
        SnapshotData snapshot = new SnapshotData(kind, size(), facing, areaMin.subtract(worldPosition),
                palette, blocks, name, rotate, excavate, allowCreative, scanningCreativeOnly);
        snapshot = composeLinked(snapshot);
        if (rotate) snapshot = snapshot.normalized(captureRotation(facing));
        ItemStack output = BCBuildersItems.snapshotStack(server, snapshot);
        inventory.set(1, ItemResource.of(output), 1);
        long remaining = inventory.getAmountAsLong(0) - 1;
        inventory.set(0, remaining > 0 ? input : ItemResource.EMPTY, (int) Math.max(0, remaining));
        ownerPlayer().ifPresent(player -> buildcraft.core.AdvancementUtil.award(
            player, "buildcraftbuilders:architect"));
        resetScan();
        sync();
    }

    private SnapshotData composeLinked(SnapshotData primary) {
        if (linkedMarkers.isEmpty() || level == null) return primary;
        BlockPos unionMin = areaMin;
        BlockPos unionMax = areaMax;
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (int index = 0; index < primary.blocks().size(); index++) {
            int x = index % primary.size().getX();
            int y = index / primary.size().getX() % primary.size().getY();
            int z = index / (primary.size().getX() * primary.size().getY());
            states.put(areaMin.offset(x, y, z), primary.palette().get(primary.blocks().get(index)));
        }
        for (BlockPos markerPos : linkedMarkers) {
            if (!(level.getBlockEntity(markerPos) instanceof ConstructionMarkerBlockEntity marker)) continue;
            SnapshotData linked = marker.snapshot();
            if (linked == null || !linked.valid()) continue;
            BlockPos linkedMin = marker.snapshotMin();
            BlockPos linkedMax = marker.snapshotMax();
            unionMin = new BlockPos(Math.min(unionMin.getX(), linkedMin.getX()), Math.min(unionMin.getY(), linkedMin.getY()),
                    Math.min(unionMin.getZ(), linkedMin.getZ()));
            unionMax = new BlockPos(Math.max(unionMax.getX(), linkedMax.getX()), Math.max(unionMax.getY(), linkedMax.getY()),
                    Math.max(unionMax.getZ(), linkedMax.getZ()));
            for (int index = 0; index < linked.blocks().size(); index++) {
                int x = index % linked.size().getX();
                int y = index / linked.size().getX() % linked.size().getY();
                int z = index / (linked.size().getX() * linked.size().getY());
                states.put(linkedMin.offset(x, y, z), linked.palette().get(linked.blocks().get(index)));
            }
        }
        BlockPos compositeSize = unionMax.subtract(unionMin).offset(1, 1, 1);
        long compositeVolume = (long) compositeSize.getX() * compositeSize.getY() * compositeSize.getZ();
        if (compositeSize.getX() > 64 || compositeSize.getY() > 64 || compositeSize.getZ() > 64
                || compositeVolume > SnapshotData.MAX_VOLUME) return primary;
        List<BlockState> compositePalette = new ArrayList<>();
        compositePalette.add(Blocks.AIR.defaultBlockState());
        List<Integer> compositeBlocks = new ArrayList<>();
        for (int z = 0; z < compositeSize.getZ(); z++) for (int y = 0; y < compositeSize.getY(); y++)
            for (int x = 0; x < compositeSize.getX(); x++) {
                BlockState state = states.getOrDefault(unionMin.offset(x, y, z), Blocks.AIR.defaultBlockState());
                if (primary.kind() == SnapshotKind.TEMPLATE) state = state.isAir()
                        ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
                int paletteIndex = compositePalette.indexOf(state);
                if (paletteIndex < 0) { paletteIndex = compositePalette.size(); compositePalette.add(state); }
                compositeBlocks.add(paletteIndex);
            }
        return new SnapshotData(primary.kind(), compositeSize, primary.facing(), unionMin.subtract(worldPosition),
                compositePalette, compositeBlocks, primary.name(), primary.rotate(), primary.excavate(),
                primary.allowCreative(), primary.creativeOnly());
    }
    private int volume() { BlockPos size = size(); return size.getX() * size.getY() * size.getZ(); }
    private BlockPos size() { return areaMax.subtract(areaMin).offset(1, 1, 1); }
    private BlockPos position(int index) {
        BlockPos size = size();
        int x = index % size.getX();
        int y = (index / size.getX()) % size.getY();
        int z = index / (size.getX() * size.getY());
        return areaMin.offset(x, y, z);
    }
    private void resetScan() {
        scanningKind = null;
        cursor = 0;
        palette = new ArrayList<>();
        blocks = new ArrayList<>();
        scanningCreativeOnly = false;
    }
    private static boolean isCreativeOnly(BlockState state) {
        return state.is(Blocks.BEDROCK) || state.is(Blocks.COMMAND_BLOCK)
                || state.is(Blocks.CHAIN_COMMAND_BLOCK) || state.is(Blocks.REPEATING_COMMAND_BLOCK)
                || state.is(Blocks.SPAWNER);
    }
    private static Rotation captureRotation(Direction facing) {
        return switch (facing) {
            case WEST -> Rotation.NONE;
            case NORTH -> Rotation.CLOCKWISE_90;
            case EAST -> Rotation.CLOCKWISE_180;
            case SOUTH -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input.childOrEmpty("inventory"));
        areaMin = input.getLong("area_min").stream().map(BlockPos::of).findFirst().orElse(null);
        areaMax = input.getLong("area_max").stream().map(BlockPos::of).findFirst().orElse(null);
        scanningKind = input.read("scanning_kind", SnapshotKind.CODEC).orElse(null);
        cursor = Math.max(0, input.getIntOr("cursor", 0));
        palette = new ArrayList<>(input.read("palette", BlockState.CODEC.listOf()).orElse(List.of()));
        blocks = new ArrayList<>(input.read("blocks", Codec.INT.listOf()).orElse(List.of()));
        scanningCreativeOnly = input.getBooleanOr("scanning_creative_only", false);
        linkedMarkers = new ArrayList<>(input.read("linked_markers", BlockPos.CODEC.listOf()).orElse(List.of()));
        blueprintName = input.getStringOr("blueprint_name", "Blueprint");
        rotate = input.getBooleanOr("rotate", true);
        excavate = input.getBooleanOr("excavate", true);
        allowCreative = input.getBooleanOr("allow_creative", false);
        if (scanningKind == null || cursor != blocks.size()) resetScan();
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
        if (areaMin != null && areaMax != null) {
            output.putLong("area_min", areaMin.asLong());
            output.putLong("area_max", areaMax.asLong());
        }
        if (scanningKind != null) {
            output.store("scanning_kind", SnapshotKind.CODEC, scanningKind);
            output.putInt("cursor", cursor);
            output.store("palette", BlockState.CODEC.listOf(), palette);
            output.store("blocks", Codec.INT.listOf(), blocks);
            if (scanningCreativeOnly) output.putBoolean("scanning_creative_only", true);
        }
        if (!linkedMarkers.isEmpty()) output.store("linked_markers", BlockPos.CODEC.listOf(), linkedMarkers);
        if (!blueprintName.equals("Blueprint")) output.putString("blueprint_name", blueprintName);
        if (!rotate) output.putBoolean("rotate", false);
        if (!excavate) output.putBoolean("excavate", false);
        if (allowCreative) output.putBoolean("allow_creative", true);
    }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
