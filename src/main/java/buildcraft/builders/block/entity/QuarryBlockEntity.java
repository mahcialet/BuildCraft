package buildcraft.builders.block.entity;

import buildcraft.api.core.IControllable;
import buildcraft.api.core.IHasWork;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.BCBuildersConfig;
import buildcraft.core.BCCoreConfig;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuilders;
import buildcraft.builders.block.QuarryBlock;
import buildcraft.core.marker.VolumeBox;
import buildcraft.core.marker.VolumeBoxSavedData;
import buildcraft.core.marker.VolumeConnection;
import buildcraft.core.marker.VolumeSavedData;
import buildcraft.lib.mj.MjBatteryReceiver;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class QuarryBlockEntity extends buildcraft.core.block.entity.OwnedBlockEntity implements IHasWork, IControllable {
    public static final long BATTERY_CAPACITY = 2_000 * MjAPI.MJ;
    public static final long MAX_POWER_PER_TICK = 10 * MjAPI.MJ;
    public static final long FRAME_COST = 4 * MjAPI.MJ;
    private static final int MAX_HORIZONTAL_SIZE = 64;

    public enum Stage { BUILDING, MINING, DONE }

    private final MjBattery battery = new MjBattery(BATTERY_CAPACITY);
    private final MjBatteryReceiver receiver = new MjBatteryReceiver(battery);
    private final ItemStacksResourceHandler drops = new ItemStacksResourceHandler(18);
    private final OutputHandler output = new OutputHandler();
    private final Set<Integer> blockedColumns = new HashSet<>();
    private final Set<Long> forcedChunks = new HashSet<>();
    private BlockPos areaMin;
    private BlockPos areaMax;
    private Stage stage = Stage.BUILDING;
    private ControlMode mode = ControlMode.ON;
    private int frameCursor;
    private int miningCursor;
    private BlockPos target;
    private long progress;
    private long nextMineTick;
    private Vec3 head;
    private long tickPowerBudget;
    private int tickTaskIndex;

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(BCBuildersBlockEntities.QUARRY.get(), pos, state);
    }

    public MjBatteryReceiver mjReceiver() { return receiver; }
    public ResourceHandler<ItemResource> outputHandler() { return output; }
    public ItemStacksResourceHandler internalDrops() { return drops; }
    public long storedMj() { return battery.getStored(); }
    public Stage stage() { return stage; }
    public BlockPos areaMin() { return areaMin; }
    public BlockPos areaMax() { return areaMax; }
    public BlockPos target() { return target; }
    public long progress() { return progress; }
    public Vec3 head() { return head; }
    public int forcedChunkCount() { return forcedChunks.size(); }
    public int frameCursor() { return frameCursor; }
    public int miningCursor() { return miningCursor; }

    public static void tick(Level level, BlockPos pos, BlockState state, QuarryBlockEntity quarry) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        quarry.battery.tick(level, pos);
        quarry.pushDrops(serverLevel);
        if (quarry.mode == ControlMode.OFF) return;
        if (quarry.areaMin == null && !quarry.acquireArea(serverLevel)) quarry.configureDefaultArea();
        quarry.ensureTickets(serverLevel);
        if (quarry.stage == Stage.DONE && quarry.mode == ControlMode.LOOP) {
            quarry.stage = Stage.MINING;
            quarry.miningCursor = 0;
            quarry.blockedColumns.clear();
        }
        if (quarry.stage == Stage.BUILDING) quarry.buildFrame(serverLevel);
        else if (quarry.stage == Stage.MINING) {
            quarry.tickPowerBudget = MAX_POWER_PER_TICK;
            int maxTasks = configuredMaxTasksPerTick();
            for (quarry.tickTaskIndex = 0; quarry.tickTaskIndex < maxTasks
                    && quarry.stage == Stage.MINING && quarry.tickPowerBudget > 0; quarry.tickTaskIndex++) {
                quarry.mine(serverLevel);
                if (quarry.target != null) break;
            }
        }
    }

    public boolean configureArea(BlockPos min, BlockPos max) {
        int sizeX = max.getX() - min.getX() + 1;
        int sizeY = max.getY() - min.getY() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;
        if (sizeX < 3 || sizeZ < 3 || sizeY < 1 || sizeX > MAX_HORIZONTAL_SIZE
                || sizeZ > MAX_HORIZONTAL_SIZE || max.getY() < worldPosition.getY()) return false;
        BlockPos adjustedMax = new BlockPos(max.getX(),
            Math.max(max.getY(), min.getY() + BCBuildersConfig.QUARRY_FRAME_MIN_HEIGHT.get()), max.getZ());
        if (min.equals(areaMin) && adjustedMax.equals(areaMax)) return true;
        releaseTickets();
        clearFrames();
        areaMin = min.immutable();
        areaMax = adjustedMax.immutable();
        head = new Vec3((areaMin.getX() + areaMax.getX() + 1) * 0.5, areaMax.getY() - 0.5,
                (areaMin.getZ() + areaMax.getZ() + 1) * 0.5);
        reset();
        sync();
        return true;
    }

    private boolean acquireArea(ServerLevel level) {
        VolumeSavedData markers = VolumeSavedData.get(level);
        VolumeBoxSavedData boxes = VolumeBoxSavedData.get(level);
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = worldPosition.relative(direction);
            VolumeConnection connection = markers.connectionAt(adjacent).orElse(null);
            if (connection != null && configureArea(connection.min(), connection.max())) return true;
            VolumeBox box = boxes.boxAt(adjacent).orElse(null);
            if (box != null && configureArea(box.min(), box.max())) return true;
        }
        return false;
    }

    private void configureDefaultArea() {
        Direction facing = getBlockState().hasProperty(QuarryBlock.FACING)
                ? getBlockState().getValue(QuarryBlock.FACING) : Direction.NORTH;
        int xMin;
        int zMin;
        switch (facing) {
            case EAST -> { xMin = worldPosition.getX() + 1; zMin = worldPosition.getZ() - 5; }
            case WEST -> { xMin = worldPosition.getX() - 11; zMin = worldPosition.getZ() - 5; }
            case SOUTH -> { xMin = worldPosition.getX() - 5; zMin = worldPosition.getZ() + 1; }
            default -> { xMin = worldPosition.getX() - 5; zMin = worldPosition.getZ() - 11; }
        }
        configureArea(new BlockPos(xMin, worldPosition.getY(), zMin),
                new BlockPos(xMin + 10, worldPosition.getY() + 4, zMin + 10));
    }

    private void buildFrame(ServerLevel level) {
        int volume = frameVolume();
        int scanned = 0;
        while (frameCursor < volume && scanned++ < 1024) {
            BlockPos pos = framePosition(frameCursor++);
            if (!isFrameEdge(pos)) continue;
            BlockState current = level.getBlockState(pos);
            if (current.is(BCBuildersBlocks.FRAME.get())) continue;
            if (!current.canBeReplaced()) continue;
            if (battery.extractPower(FRAME_COST, FRAME_COST, true) < FRAME_COST) {
                frameCursor--;
                return;
            }
            battery.extractPower(FRAME_COST, FRAME_COST, false);
            level.setBlock(pos, BCBuildersBlocks.FRAME.get().defaultBlockState(), Block.UPDATE_ALL);
            sync();
            return;
        }
        if (frameCursor >= volume) {
            stage = Stage.MINING;
            target = null;
            progress = 0;
            sync();
        }
    }

    private void mine(ServerLevel level) {
        if (target == null) {
            target = findTarget(level);
            progress = 0;
            if (target == null) {
                stage = Stage.DONE;
                if (areaMin != null && areaMax != null
                    && areaMax.getX() - areaMin.getX() == 63
                    && areaMax.getZ() - areaMin.getZ() == 63) {
                    ownerPlayer().ifPresent(player -> buildcraft.core.AdvancementUtil.award(
                        player, "buildcraftbuilders:diggy_diggy_hole"));
                }
                sync();
                return;
            }
        }
        if (!moveHead()) return;
        BlockState state = level.getBlockState(target);
        if (!mineable(state, level, target)) {
            target = null;
            return;
        }
        float hardness = state.getDestroySpeed(level, target);
        long required = Math.max(MjAPI.MJ, (long) Math.floor(
            32 * MjAPI.MJ * (hardness + 1) * BCCoreConfig.MINING_MULTIPLIER.get()));
        long needed = Math.min(MAX_POWER_PER_TICK, required - progress);
        int divisor = BCBuildersConfig.QUARRY_TASK_POWER_DIVISOR.get();
        long requested = needed;
        if (divisor > 0 && tickTaskIndex > 0) {
            requested = (needed * (divisor + tickTaskIndex) + divisor - 1) / divisor;
        }
        long extracted = battery.extractPower(0, Math.min(tickPowerBudget, requested), false);
        tickPowerBudget -= extracted;
        long accepted = effectiveTaskPower(extracted, tickTaskIndex, divisor);
        progress += accepted;
        if (progress < required) { setChanged(); return; }
        long mineDelay = configuredMineDelayTicks();
        if (mineDelay > 0 && level.getGameTime() < nextMineTick) return;

        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var event = CommonHooks.fireBlockBreak(level, GameType.SURVIVAL, fakePlayer, target, state);
        if (event.isCanceled()) {
            blockedColumns.add(columnIndex(target));
            target = null;
            progress = 0;
            sync();
            return;
        }
        List<ItemStack> mined = Block.getDrops(state, level, target, level.getBlockEntity(target), fakePlayer, tool);
        level.removeBlock(target, false);
        if (mineDelay > 0) nextMineTick = level.getGameTime() + mineDelay;
        for (ItemStack stack : mined) storeOrDrop(level, stack);
        target = null;
        progress = 0;
        sync();
    }

    public static int configuredMaxTasksPerTick() {
        return Math.max(1, BCBuildersConfig.QUARRY_MAX_TASKS_PER_TICK.get());
    }

    public static long effectiveTaskPower(long extracted, int taskIndex, int divisor) {
        if (extracted <= 0 || divisor <= 0 || taskIndex <= 0) return extracted;
        return extracted * divisor / (divisor + taskIndex);
    }

    private boolean moveHead() {
        Vec3 destination = Vec3.atCenterOf(target).add(0, 0.75, 0);
        if (head == null) head = destination;
        Vec3 delta = destination.subtract(head);
        if (!BCBuildersConfig.QUARRY_FRAME_MOVE_BOTH.get()) {
            if (Math.abs(delta.x) > 0.05) delta = new Vec3(delta.x, 0, 0);
            else if (Math.abs(delta.z) > 0.05) delta = new Vec3(0, 0, delta.z);
            else delta = new Vec3(0, delta.y, 0);
        }
        double distance = delta.length();
        if (distance <= 0.05) { head = destination; return true; }
        double configured = BCBuildersConfig.QUARRY_MAX_FRAME_SPEED.get();
        double maxPerTick = configured < 0.1 ? 0.5 : configured / 20.0;
        head = head.add(delta.scale(Math.min(maxPerTick, distance) / distance));
        sync();
        return false;
    }

    private BlockPos findTarget(ServerLevel level) {
        int width = areaMax.getX() - areaMin.getX() - 1;
        int depth = areaMax.getZ() - areaMin.getZ() - 1;
        int layerSize = width * depth;
        int top = areaMax.getY() - 1;
        int minimum = Math.max(level.getMinY(), top - BCCoreConfig.MINING_MAX_DEPTH.get() + 1);
        int layers = top - minimum + 1;
        int total = layerSize * layers;
        while (miningCursor < total) {
            int index = miningCursor++;
            int layer = index / layerSize;
            int within = index % layerSize;
            int x = within % width;
            int zBase = within / width;
            int z = ((x + layer) & 1) == 0 ? zBase : depth - zBase - 1;
            if (blockedColumns.contains(x + z * width)) continue;
            BlockPos pos = new BlockPos(areaMin.getX() + x + 1, top - layer, areaMin.getZ() + z + 1);
            BlockState state = level.getBlockState(pos);
            if (state.getDestroySpeed(level, pos) < 0) {
                blockedColumns.add(x + z * width);
                continue;
            }
            if (mineable(state, level, pos)) return pos;
        }
        return null;
    }

    public static long configuredMineDelayTicks() {
        double maxRate = BCBuildersConfig.QUARRY_MAX_BLOCK_MINE_RATE.get();
        return maxRate < 0.01 ? 0 : Math.max(1, (long) Math.ceil(20.0 / maxRate));
    }

    private boolean mineable(BlockState state, ServerLevel level, BlockPos pos) {
        return !state.isAir() && !state.is(BCBuildersBlocks.FRAME.get())
                && state.getFluidState().isEmpty() && state.getDestroySpeed(level, pos) >= 0;
    }

    private int columnIndex(BlockPos pos) {
        int width = areaMax.getX() - areaMin.getX() - 1;
        return pos.getX() - areaMin.getX() - 1 + (pos.getZ() - areaMin.getZ() - 1) * width;
    }

    private int frameVolume() {
        BlockPos size = areaMax.subtract(areaMin).offset(1, 1, 1);
        return size.getX() * size.getY() * size.getZ();
    }
    private BlockPos framePosition(int index) {
        BlockPos size = areaMax.subtract(areaMin).offset(1, 1, 1);
        int x = index % size.getX();
        int y = index / size.getX() % size.getY();
        int z = index / (size.getX() * size.getY());
        return areaMin.offset(x, y, z);
    }
    private boolean isFrameEdge(BlockPos pos) {
        int boundaries = 0;
        if (pos.getX() == areaMin.getX() || pos.getX() == areaMax.getX()) boundaries++;
        if (pos.getY() == areaMin.getY() || pos.getY() == areaMax.getY()) boundaries++;
        if (pos.getZ() == areaMin.getZ() || pos.getZ() == areaMax.getZ()) boundaries++;
        return boundaries >= 2;
    }

    public void clearFrames() {
        if (!(level instanceof ServerLevel serverLevel) || areaMin == null || areaMax == null) return;
        BlockPos.betweenClosedStream(areaMin, areaMax).filter(this::isFrameEdge).forEach(pos -> {
            if (serverLevel.getBlockState(pos).is(BCBuildersBlocks.FRAME.get())) serverLevel.removeBlock(pos, false);
        });
    }

    private Set<Long> desiredChunks() {
        Set<Long> chunks = new HashSet<>();
        chunks.add(ChunkPos.pack(worldPosition.getX() >> 4, worldPosition.getZ() >> 4));
        if (areaMin != null && areaMax != null) {
            for (int x = areaMin.getX() >> 4; x <= areaMax.getX() >> 4; x++) {
                for (int z = areaMin.getZ() >> 4; z <= areaMax.getZ() >> 4; z++) chunks.add(ChunkPos.pack(x, z));
            }
        }
        return chunks;
    }
    private void ensureTickets(ServerLevel level) {
        if (!buildcraft.core.BCCoreConfig.CHUNK_LOAD_LEVEL.get().allowsHardTileTickets()) {
            releaseTickets();
            return;
        }
        for (long packed : desiredChunks()) {
            if (forcedChunks.add(packed)) {
                ChunkPos chunk = ChunkPos.unpack(packed);
                BCBuilders.QUARRY_TICKETS.forceChunk(level, worldPosition, chunk.x(), chunk.z(), true, false);
            }
        }
    }
    public void releaseTickets() {
        if (!(level instanceof ServerLevel serverLevel)) { forcedChunks.clear(); return; }
        Set<Long> chunks = forcedChunks.isEmpty() ? desiredChunks() : Set.copyOf(forcedChunks);
        for (long packed : chunks) {
            ChunkPos chunk = ChunkPos.unpack(packed);
            BCBuilders.QUARRY_TICKETS.forceChunk(serverLevel, worldPosition, chunk.x(), chunk.z(), false, false);
        }
        forcedChunks.clear();
    }
    public void destroy() { releaseTickets(); clearFrames(); }

    private void storeOrDrop(ServerLevel level, ItemStack stack) {
        int remaining = stack.getCount();
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = drops.insert(ItemResource.of(stack), remaining, transaction);
            if (inserted > 0) { transaction.commit(); remaining -= inserted; }
        }
        if (remaining > 0) level.addFreshEntity(new ItemEntity(level, worldPosition.getX() + .5,
                worldPosition.getY() + .5, worldPosition.getZ() + .5, stack.copyWithCount(remaining)));
    }

    private void pushDrops(ServerLevel level) {
        for (Direction direction : Direction.values()) {
            var handler = level.getCapability(Capabilities.Item.BLOCK,
                    worldPosition.relative(direction), direction.getOpposite());
            if (handler == null) continue;
            for (int slot = 0; slot < drops.size(); slot++) {
                ItemResource resource = drops.getResource(slot);
                int amount = drops.getAmountAsInt(slot);
                if (resource.isEmpty() || amount <= 0) continue;
                try (Transaction transaction = Transaction.openRoot()) {
                    int inserted = handler.insert(resource, amount, transaction);
                    int extracted = drops.extract(slot, resource, inserted, transaction);
                    if (extracted > 0) transaction.commit();
                }
            }
        }
    }

    private void reset() {
        stage = Stage.BUILDING;
        frameCursor = 0;
        miningCursor = 0;
        target = null;
        progress = 0;
        blockedColumns.clear();
    }
    public void restart() { reset(); sync(); }
    @Override public boolean hasWork() { return mode != ControlMode.OFF && stage != Stage.DONE; }
    @Override public ControlMode controlMode() { return mode; }
    @Override public void setControlMode(ControlMode mode) {
        if (this.mode != mode) { this.mode = mode; if (mode == ControlMode.LOOP && stage == Stage.DONE) reset(); sync(); }
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
        drops.deserialize(input.childOrEmpty("drops"));
        areaMin = input.getLong("area_min").stream().map(BlockPos::of).findFirst().orElse(null);
        areaMax = input.getLong("area_max").stream().map(BlockPos::of).findFirst().orElse(null);
        stage = Stage.values()[Math.floorMod(input.getIntOr("stage", 0), Stage.values().length)];
        mode = input.read("mode", ControlMode.CODEC).orElse(ControlMode.ON);
        frameCursor = Math.max(0, input.getIntOr("frame_cursor", 0));
        miningCursor = Math.max(0, input.getIntOr("mining_cursor", 0));
        target = input.getLong("target").stream().map(BlockPos::of).findFirst().orElse(null);
        progress = Math.max(0, input.getLongOr("progress", 0));
        nextMineTick = Math.max(0, input.getLongOr("next_mine_tick", 0));
        blockedColumns.clear();
        forcedChunks.clear();
        blockedColumns.addAll(input.read("blocked_columns", Codec.INT.listOf()).orElse(List.of()));
        if (input.getBooleanOr("has_head", false)) head = new Vec3(input.getDoubleOr("head_x", 0),
                input.getDoubleOr("head_y", 0), input.getDoubleOr("head_z", 0));
        else head = null;
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (battery.getStored() > 0) output.putLong("stored_mj", battery.getStored());
        drops.serialize(output.child("drops"));
        if (areaMin != null) output.putLong("area_min", areaMin.asLong());
        if (areaMax != null) output.putLong("area_max", areaMax.asLong());
        output.putInt("stage", stage.ordinal());
        output.store("mode", ControlMode.CODEC, mode);
        if (frameCursor > 0) output.putInt("frame_cursor", frameCursor);
        if (miningCursor > 0) output.putInt("mining_cursor", miningCursor);
        if (target != null) output.putLong("target", target.asLong());
        if (progress > 0) output.putLong("progress", progress);
        if (nextMineTick > 0) output.putLong("next_mine_tick", nextMineTick);
        if (!blockedColumns.isEmpty()) output.store("blocked_columns", Codec.INT.listOf(), new ArrayList<>(blockedColumns));
        if (head != null) {
            output.putBoolean("has_head", true);
            output.putDouble("head_x", head.x);
            output.putDouble("head_y", head.y);
            output.putDouble("head_z", head.z);
        }
    }

    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    private final class OutputHandler implements ResourceHandler<ItemResource> {
        @Override public int size() { return drops.size(); }
        @Override public ItemResource getResource(int index) { return drops.getResource(index); }
        @Override public long getAmountAsLong(int index) { return drops.getAmountAsLong(index); }
        @Override public boolean isValid(int index, ItemResource resource) { return false; }
        @Override public long getCapacityAsLong(int index, ItemResource resource) { return drops.getCapacityAsLong(index, resource); }
        @Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) { return 0; }
        @Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return drops.extract(index, resource, amount, transaction);
        }
    }
}
