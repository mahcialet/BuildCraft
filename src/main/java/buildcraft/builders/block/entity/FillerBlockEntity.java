package buildcraft.builders.block.entity;

import buildcraft.api.core.IControllable;
import buildcraft.api.core.IHasWork;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.FillerPattern;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
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
    private FillerPattern pattern = FillerPattern.FILL;

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
    public FillerPattern pattern() { return pattern; }

    public static void tick(Level level, BlockPos pos, BlockState state, FillerBlockEntity filler) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        filler.battery.tick(level, pos);
        filler.refreshArea(serverLevel);
        filler.fillNext(serverLevel);
    }

    private void refreshArea(ServerLevel level) {
        if (areaMin != null && areaMax != null) return;
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
        if (min == null) return;
        configureArea(min, max);
    }

    /** Accepts bounds copied from a marker graph, volume box, map, or another area provider. */
    public boolean configureArea(BlockPos min, BlockPos max) {
        int areaVolume = volume(min, max);
        if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()
                || areaVolume <= 0 || areaVolume > MAX_AREA_VOLUME) return false;
        if (java.util.Objects.equals(areaMin, min) && java.util.Objects.equals(areaMax, max)) return true;
        areaMin = min.immutable();
        areaMax = max.immutable();
        cursor = 0;
        finished = false;
        sync();
        return true;
    }

    private void fillNext(ServerLevel level) {
        if (mode == ControlMode.OFF || pattern == FillerPattern.NONE
                || areaMin == null || areaMax == null) return;
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
            if (!pattern.includes(target, areaMin, areaMax)) {
                cursor++;
                continue;
            }
            BlockState existing = level.getBlockState(target);
            if (pattern.clears()) {
                if (existing.isAir()) {
                    cursor++;
                    continue;
                }
                if (!clear(level, target, existing)) return;
                cursor++;
                finished = false;
                sync();
                return;
            }
            if (!existing.isAir() && !existing.canBeReplaced()) {
                cursor++;
                continue;
            }
            int slot = resourceSlot();
            if (slot < 0) return;
            ItemResource resource = resources.getResource(slot);
            if (battery.extractPower(POWER_PER_BLOCK, POWER_PER_BLOCK, true) < POWER_PER_BLOCK) return;
            if (!place(level, target, resource)) {
                cursor++;
                continue;
            }
            battery.extractPower(POWER_PER_BLOCK, POWER_PER_BLOCK, false);
            try (Transaction transaction = Transaction.openRoot()) {
                if (resources.extract(slot, resource, 1, transaction) != 1) return;
                transaction.commit();
            }
            cursor++;
            finished = false;
            sync();
            return;
        }
        finished = cursor >= volume;
        sync();
    }

    private boolean place(ServerLevel level, BlockPos target, ItemResource resource) {
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        ItemStack held = resource.toStack(1);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, held);
        var hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        return ((BlockItem) resource.getItem()).place(new BlockPlaceContext(
                new UseOnContext(level, fakePlayer, InteractionHand.MAIN_HAND, held, hit))).consumesAction();
    }

    private boolean clear(ServerLevel level, BlockPos target, BlockState state) {
        if (state.getDestroySpeed(level, target) < 0) {
            return true;
        }
        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var event = CommonHooks.fireBlockBreak(level, GameType.SURVIVAL, fakePlayer, target, state);
        if (event.isCanceled()) {
            return true;
        }
        long used = battery.extractPower(POWER_PER_BLOCK, POWER_PER_BLOCK, false);
        if (used < POWER_PER_BLOCK) return false;
        var drops = Block.getDrops(state, level, target, level.getBlockEntity(target), fakePlayer, tool);
        level.removeBlock(target, false);
        for (ItemStack stack : drops) storeOrDrop(level, stack);
        return true;
    }

    private void storeOrDrop(ServerLevel level, ItemStack stack) {
        int remaining = stack.getCount();
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = resources.insert(ItemResource.of(stack), remaining, transaction);
            if (inserted > 0) {
                transaction.commit();
                remaining -= inserted;
            }
        }
        if (remaining > 0) {
            level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + .5,
                    worldPosition.getY() + .75, worldPosition.getZ() + .5,
                    stack.copyWithCount(remaining)));
        }
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
        return mode != ControlMode.OFF && pattern != FillerPattern.NONE && areaMin != null
                && (mode == ControlMode.LOOP || !finished);
    }
    @Override public ControlMode controlMode() { return mode; }
    @Override public void setControlMode(ControlMode mode) {
        if (this.mode == mode) return;
        if (this.mode == ControlMode.OFF && mode != ControlMode.OFF) finished = false;
        this.mode = mode;
        sync();
    }

    public void setPattern(FillerPattern pattern) {
        if (this.pattern == pattern) return;
        this.pattern = pattern;
        cursor = 0;
        finished = pattern == FillerPattern.NONE;
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
        pattern = input.read("pattern", FillerPattern.CODEC).orElse(FillerPattern.FILL);
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
        output.store("pattern", FillerPattern.CODEC, pattern);
    }

    @Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
