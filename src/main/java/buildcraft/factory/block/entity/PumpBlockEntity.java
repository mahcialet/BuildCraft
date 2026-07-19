package buildcraft.factory.block.entity;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.factory.BCFactoryBlockEntities;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.lib.mj.MjBatteryReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class PumpBlockEntity extends BlockEntity implements buildcraft.api.core.IHasWork {
    public static final int CAPACITY = 16_000;
    public static final long POWER_PER_SOURCE = 10 * MjAPI.MJ;
    private static final int MAX_DEPTH = 512;
    private static final int MAX_DISTANCE = 64;
    private static final Direction[] LIQUID_SEARCH = {
        Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final Direction[] GAS_SEARCH = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private final PumpTank tank = new PumpTank();
    private final OutputFluidHandler output = new OutputFluidHandler();
    private final MjBattery battery = new MjBattery(50 * MjAPI.MJ);
    private final MjBatteryReceiver receiver = new MjBatteryReceiver(battery);
    private final ArrayDeque<BlockPos> sources = new ArrayDeque<>();
    private BlockPos intake;
    private int rebuildTicks;

    @Override public boolean hasWork() { return intake != null || !sources.isEmpty(); }

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(BCFactoryBlockEntities.PUMP.get(), pos, state);
    }

    public MjBatteryReceiver mjReceiver() { return receiver; }
    public ResourceHandler<FluidResource> outputFluidHandler() { return output; }
    public FluidStacksResourceHandler internalTank() { return tank; }
    public BlockPos intake() { return intake; }
    public long storedMj() { return battery.getStored(); }

    public static void tick(Level level, BlockPos pos, BlockState state, PumpBlockEntity pump) {
        if (level.isClientSide()) return;
        pump.battery.tick(level, pos);
        pump.pushFluid();
        if (pump.tank.getAmountAsInt(0) > CAPACITY / 2) return;
        if (pump.sources.isEmpty() && (++pump.rebuildTicks >= 30 || pump.intake == null)) {
            pump.rebuildTicks = 0;
            pump.rebuildQueue();
        }
        pump.pumpSource();
    }

    private void rebuildQueue() {
        sources.clear();
        clearTubes();
        intake = null;
        if (level == null) return;
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            BlockPos candidate = worldPosition.below(depth);
            BlockState state = level.getBlockState(candidate);
            if (!state.getFluidState().isEmpty()) {
                intake = candidate;
                break;
            }
            if (!state.isAir()) break;
        }
        if (intake == null) {
            changed();
            return;
        }
        for (int y = worldPosition.getY() - 1; y > intake.getY(); y--) {
            BlockPos tubePos = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
            if (level.getBlockState(tubePos).isAir()) {
                level.setBlock(tubePos, BCFactoryBlocks.TUBE.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        Fluid fluid = level.getFluidState(intake).getType();
        Direction[] search = fluid.getFluidType().isLighterThanAir() ? GAS_SEARCH : LIQUID_SEARCH;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> checked = new HashSet<>();
        queue.add(intake);
        while (!queue.isEmpty() && checked.size() < 4_096) {
            BlockPos current = queue.removeFirst();
            if (!checked.add(current) || current.distSqr(intake) > MAX_DISTANCE * MAX_DISTANCE) continue;
            var fluidState = level.getFluidState(current);
            if (fluidState.isEmpty() || fluidState.getType() != fluid) continue;
            if (fluidState.isSource()) sources.addLast(current);
            for (Direction direction : search) queue.addLast(current.relative(direction));
        }
        changed();
    }

    private void clearTubes() {
        if (level == null) return;
        for (int depth = 1; depth <= MAX_DEPTH; depth++) {
            BlockPos candidate = worldPosition.below(depth);
            if (!level.getBlockState(candidate).is(BCFactoryBlocks.TUBE.get())) break;
            level.setBlock(candidate, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private void pumpSource() {
        if (level == null || sources.isEmpty() || tank.getAmountAsInt(0) > CAPACITY / 2) return;
        BlockPos sourcePos = sources.peekLast();
        var fluidState = level.getFluidState(sourcePos);
        if (!fluidState.isSource()) {
            sources.removeLast();
            return;
        }
        FluidResource resource = FluidResource.of(fluidState.getType());
        try (Transaction transaction = Transaction.openRoot()) {
            if (tank.insert(0, resource, 1_000, transaction) != 1_000) return;
            if (battery.extractPower(POWER_PER_SOURCE, POWER_PER_SOURCE, false) != POWER_PER_SOURCE) return;
            transaction.commit();
        }
        if (!isInfiniteWaterSource(sourcePos, fluidState.getType())) {
            level.setBlock(sourcePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        sources.removeLast();
        changed();
    }

    private boolean isInfiniteWaterSource(BlockPos pos, Fluid fluid) {
        if (level == null || fluid != Fluids.WATER) return false;
        int adjacent = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(direction)).isSource()
                    && level.getFluidState(pos.relative(direction)).getType() == Fluids.WATER) adjacent++;
        }
        BlockState below = level.getBlockState(pos.below());
        return adjacent >= 2 && (!below.isAir() || level.getFluidState(pos.below()).isSource());
    }

    private void pushFluid() {
        if (level == null || tank.getAmountAsInt(0) <= 0) return;
        FluidResource resource = tank.getResource(0);
        for (Direction direction : Direction.values()) {
            BlockPos targetPos = worldPosition.relative(direction);
            var handler = level.getCapability(Capabilities.Fluid.BLOCK, targetPos, direction.getOpposite());
            if (handler == null || handler == output) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = tank.extract(0, resource, tank.getAmountAsInt(0), transaction);
                int inserted = handler.insert(resource, extracted, transaction);
                if (inserted <= 0) continue;
                if (inserted < extracted) tank.insert(0, resource, extracted - inserted, transaction);
                transaction.commit();
                changed();
                return;
            }
        }
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(
            worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS
        );
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.deserialize(input.childOrEmpty("tank"));
        battery.extractAll();
        long stored = Math.max(0, input.getLongOr("stored_mj", 0));
        if (stored > 0) battery.addPower(stored, false);
        rebuildTicks = Math.clamp(input.getIntOr("rebuild_ticks", 0), 0, 29);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("tank"));
        if (battery.getStored() > 0) output.putLong("stored_mj", battery.getStored());
        if (rebuildTicks > 0) output.putInt("rebuild_ticks", rebuildTicks);
    }

    private final class PumpTank extends FluidStacksResourceHandler {
        private PumpTank() { super(1, CAPACITY); }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int inserted = super.insert(index, resource, amount, transaction);
            if (inserted > 0) changed();
            return inserted;
        }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int extracted = super.extract(index, resource, amount, transaction);
            if (extracted > 0) changed();
            return extracted;
        }
    }

    private final class OutputFluidHandler implements ResourceHandler<FluidResource> {
        @Override public int size() { return tank.size(); }
        @Override public FluidResource getResource(int index) { return tank.getResource(index); }
        @Override public long getAmountAsLong(int index) { return tank.getAmountAsLong(index); }
        @Override public boolean isValid(int index, FluidResource resource) { return false; }
        @Override public long getCapacityAsLong(int index, FluidResource resource) {
            return tank.getCapacityAsLong(index, resource);
        }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return 0;
        }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            return tank.extract(index, resource, amount, transaction);
        }
    }
}
