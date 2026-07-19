package buildcraft.factory.block.entity;

import buildcraft.factory.BCFactoryBlockEntities;
import buildcraft.factory.block.FloodGateBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class FloodGateBlockEntity extends BlockEntity {
    public static final int CAPACITY = 2_000;
    private static final Direction[] LIQUID_SEARCH = {
        Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private static final Direction[] GAS_SEARCH = {
        Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };
    private final FluidBuffer fluidBuffer = new FluidBuffer();
    private int ticks;

    public FloodGateBlockEntity(BlockPos pos, BlockState state) {
        super(BCFactoryBlockEntities.FLOOD_GATE.get(), pos, state);
    }

    public FluidStacksResourceHandler fluidBuffer() {
        return fluidBuffer;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FloodGateBlockEntity gate) {
        if (level.isClientSide() || gate.fluidBuffer.getAmountAsInt(0) < 1_000) return;
        if (++gate.ticks < 16) return;
        gate.ticks = 0;
        FluidResource resource = gate.fluidBuffer.getResource(0);
        BlockPos target = gate.findTarget(resource);
        if (target != null && net.neoforged.neoforge.transfer.fluid.FluidUtil.tryPlaceFluid(
                gate.fluidBuffer, null, level, InteractionHand.MAIN_HAND, target
        ).getAmount() > 0) {
            gate.changed();
        }
    }

    private BlockPos findTarget(FluidResource resource) {
        if (level == null || resource.isEmpty()) return null;
        Direction[] search = resource.getFluidType().isLighterThanAir() ? GAS_SEARCH : LIQUID_SEARCH;
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> checked = new HashSet<>();
        checked.add(worldPosition);
        for (Direction direction : Direction.values()) {
            var property = FloodGateBlock.property(direction);
            if (property != null && getBlockState().getValue(property)) {
                queue.add(worldPosition.relative(direction));
            }
        }
        while (!queue.isEmpty() && checked.size() < 4_096) {
            BlockPos current = queue.removeFirst();
            if (!checked.add(current) || current.distSqr(worldPosition) > 64 * 64) continue;
            BlockState state = level.getBlockState(current);
            if (state.isAir()) return current;
            if (state.getFluidState().isEmpty()
                    || state.getFluidState().getType() != resource.getFluid()) continue;
            if (!state.getFluidState().isSource()) return current;
            for (Direction direction : search) queue.addLast(current.relative(direction));
        }
        return null;
    }

    public BlockPos nextTarget() {
        return findTarget(fluidBuffer.getResource(0));
    }

    public boolean toggleSide(Direction direction) {
        if (level == null) return false;
        var property = FloodGateBlock.property(direction);
        if (property == null) return false;
        level.setBlock(worldPosition, getBlockState().cycle(property), Block.UPDATE_ALL);
        ticks = 0;
        setChanged();
        return true;
    }

    private void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluidBuffer.deserialize(input.childOrEmpty("tank"));
        ticks = Math.clamp(input.getIntOr("ticks", 0), 0, 15);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fluidBuffer.serialize(output.child("tank"));
        if (ticks > 0) output.putInt("ticks", ticks);
    }

    private final class FluidBuffer extends FluidStacksResourceHandler {
        private FluidBuffer() { super(1, CAPACITY); }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int inserted = super.insert(index, resource, amount, transaction);
            if (inserted > 0) changed();
            return inserted;
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int extracted = super.extract(index, resource, amount, transaction);
            if (extracted > 0) changed();
            return extracted;
        }
    }
}
