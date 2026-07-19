package buildcraft.factory.block.entity;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.factory.BCFactoryBlockEntities;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.lib.mj.MjBatteryReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;

public final class MiningWellBlockEntity extends BlockEntity {
    public static final long BATTERY_CAPACITY = 500 * MjAPI.MJ;
    public static final long MAX_POWER_PER_TICK = 10 * MjAPI.MJ;
    private static final int MAX_DEPTH = 512;

    private final MjBattery battery = new MjBattery(BATTERY_CAPACITY);
    private final MjBatteryReceiver receiver = new MjBatteryReceiver(battery);
    private final ItemStacksResourceHandler drops = new ItemStacksResourceHandler(18);
    private final OutputHandler output = new OutputHandler();
    private BlockPos target;
    private long progress;

    public MiningWellBlockEntity(BlockPos pos, BlockState state) {
        super(BCFactoryBlockEntities.MINING_WELL.get(), pos, state);
    }

    public MjBatteryReceiver mjReceiver() { return receiver; }
    public ResourceHandler<ItemResource> outputHandler() { return output; }
    public ItemStacksResourceHandler internalDrops() { return drops; }
    public BlockPos target() { return target; }
    public long progress() { return progress; }
    public long storedMj() { return battery.getStored(); }

    public static void tick(Level level, BlockPos pos, BlockState state, MiningWellBlockEntity well) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        well.battery.tick(level, pos);
        well.pushDrops(serverLevel);
        well.mine(serverLevel);
    }

    private void mine(ServerLevel level) {
        if (target == null || !isTargetValid(level, target)) {
            target = findTarget(level);
            if (target != null) buildTubesTo(level, target);
            progress = 0;
            setChanged();
        }
        if (target == null) return;
        BlockState state = level.getBlockState(target);
        float hardness = state.getDestroySpeed(level, target);
        if (hardness < 0) {
            target = null;
            progress = 0;
            setChanged();
            return;
        }
        long required = state.getFluidState().isEmpty()
                ? Math.max(MjAPI.MJ, (long) Math.floor(32 * MjAPI.MJ * (hardness + 1)))
                : MjAPI.MJ;
        long used = battery.extractPower(0, Math.min(MAX_POWER_PER_TICK, required - progress), false);
        if (used <= 0) return;
        progress += used;
        setChanged();
        if (progress < required) return;

        var fakePlayer = FakePlayerFactory.getMinecraft(level);
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var event = CommonHooks.fireBlockBreak(level, GameType.SURVIVAL, fakePlayer, target, state);
        if (event.isCanceled()) {
            progress = 0;
            return;
        }
        List<ItemStack> mined = Block.getDrops(state, level, target, level.getBlockEntity(target), fakePlayer, tool);
        BlockPos minedPos = target;
        level.removeBlock(minedPos, false);
        for (ItemStack stack : mined) storeOrDrop(level, stack);
        level.setBlock(minedPos, BCFactoryBlocks.TUBE.get().defaultBlockState(), Block.UPDATE_ALL);
        target = null;
        progress = 0;
        setChanged();
    }

    private boolean isTargetValid(ServerLevel level, BlockPos pos) {
        if (pos.getX() != worldPosition.getX() || pos.getZ() != worldPosition.getZ()) return false;
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.is(BCFactoryBlocks.TUBE.get());
    }

    private BlockPos findTarget(ServerLevel level) {
        int minimum = Math.max(level.getMinY(), worldPosition.getY() - MAX_DEPTH);
        for (int y = worldPosition.getY() - 1; y >= minimum; y--) {
            BlockPos pos = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(BCFactoryBlocks.TUBE.get())) return pos;
        }
        return null;
    }

    private void buildTubesTo(ServerLevel level, BlockPos targetPos) {
        for (int y = worldPosition.getY() - 1; y > targetPos.getY(); y--) {
            BlockPos pos = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, BCFactoryBlocks.TUBE.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private void storeOrDrop(ServerLevel level, ItemStack stack) {
        int remaining = stack.getCount();
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = drops.insert(ItemResource.of(stack), remaining, transaction);
            if (inserted > 0) {
                transaction.commit();
                remaining -= inserted;
            }
        }
        if (remaining > 0) {
            Direction facing = getBlockState().getValue(buildcraft.factory.block.MiningWellBlock.FACING);
            double x = worldPosition.getX() + 0.5 + facing.getStepX() * 0.6;
            double y = worldPosition.getY() + 0.5;
            double z = worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.6;
            level.addFreshEntity(new ItemEntity(level, x, y, z, stack.copyWithCount(remaining)));
        }
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

    public void clearTubes() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        for (int y = worldPosition.getY() - 1;
                y >= Math.max(serverLevel.getMinY(), worldPosition.getY() - MAX_DEPTH); y--) {
            BlockPos pos = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
            if (!serverLevel.getBlockState(pos).is(BCFactoryBlocks.TUBE.get())) break;
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        battery.extractAll();
        long stored = Math.max(0, input.getLongOr("stored_mj", 0));
        if (stored > 0) battery.addPower(stored, false);
        drops.deserialize(input.childOrEmpty("drops"));
        target = input.getLong("target").stream().map(BlockPos::of).findFirst().orElse(null);
        progress = Math.max(0, input.getLongOr("progress", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (battery.getStored() > 0) output.putLong("stored_mj", battery.getStored());
        drops.serialize(output.child("drops"));
        if (target != null) output.putLong("target", target.asLong());
        if (progress > 0) output.putLong("progress", progress);
    }

    private final class OutputHandler implements ResourceHandler<ItemResource> {
        @Override public int size() { return drops.size(); }
        @Override public ItemResource getResource(int index) { return drops.getResource(index); }
        @Override public long getAmountAsLong(int index) { return drops.getAmountAsLong(index); }
        @Override public boolean isValid(int index, ItemResource resource) { return false; }
        @Override public long getCapacityAsLong(int index, ItemResource resource) {
            return drops.getCapacityAsLong(index, resource);
        }
        @Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
        @Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return drops.extract(index, resource, amount, transaction);
        }
    }
}
