package buildcraft.builders.block.entity;

import buildcraft.api.core.IControllable;
import buildcraft.api.core.IHasWork;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.core.marker.VolumeBox;
import buildcraft.core.marker.VolumeBoxSavedData;
import buildcraft.core.marker.VolumeConnection;
import buildcraft.core.marker.VolumeSavedData;
import buildcraft.lib.mj.MjBatteryReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

public final class FillerBlockEntity extends BlockEntity implements IHasWork, IControllable {
    public static final long BATTERY_CAPACITY = 16_000 * MjAPI.MJ;
    public static final long POWER_PER_BLOCK = 4 * MjAPI.MJ;
    private static final int MAX_AREA_VOLUME = 64 * 64 * 64;
    private static final int MAX_SCAN_PER_TICK = 256;

    private final MjBattery battery = new MjBattery(BATTERY_CAPACITY);
    private final MjBatteryReceiver receiver = new MjBatteryReceiver(battery);
    private final ItemStacksResourceHandler resources = new ItemStacksResourceHandler(27) {
        @Override public boolean isValid(int index, ItemResource resource) {
            return resource.getItem() instanceof BlockItem;
        }
        @Override protected void onContentsChanged(int index, ItemStack previousStack) {
            sync();
        }
    };
    private @Nullable BlockPos areaMin;
    private @Nullable BlockPos areaMax;
    private int cursor;
    private boolean finished;
    private ControlMode mode = ControlMode.ON;

    public FillerBlockEntity(BlockPos pos, BlockState state) {
        super(BCBuildersBlockEntities.FILLER.get(), pos, state);
    }

    public ItemStacksResourceHandler resources() { return resources; }
    public MjBatteryReceiver mjReceiver() { return receiver; }
    public long storedMj() { return battery.getStored(); }
    public @Nullable BlockPos areaMin() { return areaMin; }
    public @Nullable BlockPos areaMax() { return areaMax; }
    public int cursor() { return cursor; }
    public boolean finished() { return finished; }

    public static void tick(Level level, BlockPos pos, BlockState state, FillerBlockEntity filler) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        filler.battery.tick(level, pos);
        filler.refreshArea(serverLevel);
        filler.fillNext(serverLevel);
    }

    private void refreshArea(ServerLevel level) {
        BlockPos min = null;
        BlockPos max = null;
        VolumeSavedData markers = VolumeSavedData.get(level);
        VolumeBoxSavedData boxes = VolumeBoxSavedData.get(level);
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = worldPosition.relative(direction);
            VolumeConnection connection = markers.connectionAt(adjacent).orElse(null);
            if (connection != null) {
                min = connection.min();
                max = connection.max();
                break;
            }
            VolumeBox box = boxes.boxAt(adjacent).orElse(null);
            if (box != null) {
                min = box.min();
                max = box.max();
                break;
            }
        }
        if (min != null && volume(min, max) > MAX_AREA_VOLUME) {
            min = null;
            max = null;
        }
        if (java.util.Objects.equals(areaMin, min) && java.util.Objects.equals(areaMax, max)) return;
        areaMin = min == null ? null : min.immutable();
        areaMax = max == null ? null : max.immutable();
        cursor = 0;
        finished = false;
        sync();
    }

    private void fillNext(ServerLevel level) {
        if (mode == ControlMode.OFF || areaMin == null || areaMax == null) return;
        int volume = volume(areaMin, areaMax);
        if (volume <= 0) return;
        if (cursor >= volume) {
            finished = true;
            if (mode != ControlMode.LOOP) return;
            cursor = 0;
        }
        for (int checked = 0; checked < Math.min(volume, MAX_SCAN_PER_TICK) && cursor < volume; checked++) {
            BlockPos target = targetAt(cursor);
            if (target.equals(worldPosition)) {
                cursor++;
                continue;
            }
            BlockState existing = level.getBlockState(target);
            if (!existing.isAir() && !existing.canBeReplaced()) {
                cursor++;
                continue;
            }
            int slot = resourceSlot();
            if (slot < 0) return;
            long used = battery.extractPower(POWER_PER_BLOCK, POWER_PER_BLOCK, false);
            if (used < POWER_PER_BLOCK) return;
            ItemResource resource = resources.getResource(slot);
            try (Transaction transaction = Transaction.openRoot()) {
                if (resources.extract(slot, resource, 1, transaction) != 1) return;
                transaction.commit();
            }
            BlockItem blockItem = (BlockItem) resource.getItem();
            level.setBlock(target, blockItem.getBlock().defaultBlockState(), Block.UPDATE_ALL);
            cursor++;
            finished = false;
            sync();
            return;
        }
        finished = cursor >= volume;
        sync();
    }

    private int resourceSlot() {
        for (int slot = 0; slot < resources.size(); slot++) {
            if (resources.getAmountAsLong(slot) > 0 && resources.getResource(slot).getItem() instanceof BlockItem) {
                return slot;
            }
        }
        return -1;
    }

    private BlockPos targetAt(int index) {
        int sizeX = areaMax.getX() - areaMin.getX() + 1;
        int sizeZ = areaMax.getZ() - areaMin.getZ() + 1;
        int layer = sizeX * sizeZ;
        int y = index / layer;
        int inLayer = index % layer;
        int z = inLayer / sizeX;
        int x = inLayer % sizeX;
        return areaMin.offset(x, y, z);
    }

    private static int volume(BlockPos min, BlockPos max) {
        long size = (long) (max.getX() - min.getX() + 1)
                * (max.getY() - min.getY() + 1)
                * (max.getZ() - min.getZ() + 1);
        return size <= 0 || size > Integer.MAX_VALUE ? 0 : (int) size;
    }

    @Override public boolean hasWork() {
        return mode != ControlMode.OFF && areaMin != null && (mode == ControlMode.LOOP || !finished);
    }
    @Override public ControlMode controlMode() { return mode; }
    @Override public void setControlMode(ControlMode mode) {
        if (this.mode == mode) return;
        if (this.mode == ControlMode.OFF && mode != ControlMode.OFF) finished = false;
        this.mode = mode;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        battery.extractAll();
        long stored = Math.max(0, input.getLongOr("stored_mj", 0));
        if (stored > 0) battery.addPower(stored, false);
        resources.deserialize(input.childOrEmpty("resources"));
        areaMin = input.getLong("area_min").stream().map(BlockPos::of).findFirst().orElse(null);
        areaMax = input.getLong("area_max").stream().map(BlockPos::of).findFirst().orElse(null);
        cursor = Math.max(0, input.getIntOr("cursor", 0));
        finished = input.getBooleanOr("finished", false);
        mode = input.read("mode", ControlMode.CODEC).orElse(ControlMode.ON);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (battery.getStored() > 0) output.putLong("stored_mj", battery.getStored());
        resources.serialize(output.child("resources"));
        if (areaMin != null) output.putLong("area_min", areaMin.asLong());
        if (areaMax != null) output.putLong("area_max", areaMax.asLong());
        if (cursor > 0) output.putInt("cursor", cursor);
        if (finished) output.putBoolean("finished", true);
        output.store("mode", ControlMode.CODEC, mode);
    }

    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
