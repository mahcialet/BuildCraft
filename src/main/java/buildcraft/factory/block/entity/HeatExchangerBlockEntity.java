package buildcraft.factory.block.entity;

import buildcraft.energy.BCEnergyRefineryRecipes;
import buildcraft.factory.BCFactoryBlockEntities;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.HeatExchangerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class HeatExchangerBlockEntity extends BlockEntity {
    public static final int TANK_CAPACITY = 2_000;
    public static final int PREPARE_TICKS = 120;
    private static final int[] THROUGHPUT = {5, 10, 20};

    private final ExchangeTank input = new ExchangeTank(true);
    private final ExchangeTank output = new ExchangeTank(false);
    private final InputHandler inputHandler = new InputHandler();
    private final OutputHandler outputHandler = new OutputHandler();
    private int preparation;

    public HeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(BCFactoryBlockEntities.HEAT_EXCHANGER.get(), pos, state);
    }

    public FluidStacksResourceHandler inputTank() { return input; }
    public FluidStacksResourceHandler outputTank() { return output; }
    public int preparation() { return preparation; }

    public @Nullable ResourceHandler<FluidResource> fluidHandler(@Nullable Direction side) {
        HeatExchangerBlock.Part part = getBlockState().getValue(HeatExchangerBlock.PART);
        Direction facing = getBlockState().getValue(HeatExchangerBlock.FACING);
        if (part == HeatExchangerBlock.Part.START) {
            if (side == Direction.DOWN) return inputHandler;
            if (side == facing.getClockWise()) return outputHandler;
        } else if (part == HeatExchangerBlock.Part.END) {
            if (side == Direction.UP || side == facing.getCounterClockWise()) return inputHandler;
        }
        return null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HeatExchangerBlockEntity exchanger) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        List<BlockPos> structure = exchanger.findStructure(serverLevel);
        exchanger.applyParts(serverLevel, structure);
        if (!structure.isEmpty() && structure.getFirst().equals(pos)) exchanger.process(serverLevel, structure);
    }

    private List<BlockPos> findStructure(ServerLevel level) {
        Direction facing = getBlockState().getValue(HeatExchangerBlock.FACING);
        Direction toStart = facing.getClockWise();
        Direction toEnd = facing.getCounterClockWise();
        BlockPos start = worldPosition;
        for (int offset = 0; offset < 6 && matches(level, start.relative(toStart), facing); offset++) {
            start = start.relative(toStart);
        }
        List<BlockPos> positions = new ArrayList<>();
        BlockPos cursor = start;
        while (matches(level, cursor, facing) && positions.size() <= 5) {
            positions.add(cursor);
            cursor = cursor.relative(toEnd);
        }
        if (positions.size() < 3 || positions.size() > 5 || matches(level, cursor, facing)) return List.of();
        return positions;
    }

    private static boolean matches(ServerLevel level, BlockPos pos, Direction facing) {
        return level.getBlockState(pos).is(BCFactoryBlocks.HEAT_EXCHANGER.get())
                && level.getBlockState(pos).getValue(HeatExchangerBlock.FACING) == facing;
    }

    private void applyParts(ServerLevel level, List<BlockPos> structure) {
        if (structure.isEmpty()) {
            if (getBlockState().getValue(HeatExchangerBlock.PART) != HeatExchangerBlock.Part.NONE) {
                level.setBlock(worldPosition, getBlockState().setValue(
                        HeatExchangerBlock.PART, HeatExchangerBlock.Part.NONE), Block.UPDATE_CLIENTS);
            }
            return;
        }
        for (int index = 0; index < structure.size(); index++) {
            HeatExchangerBlock.Part part = index == 0 ? HeatExchangerBlock.Part.START
                    : index == structure.size() - 1 ? HeatExchangerBlock.Part.END
                    : HeatExchangerBlock.Part.MIDDLE;
            BlockPos pos = structure.get(index);
            BlockState state = level.getBlockState(pos);
            if (state.getValue(HeatExchangerBlock.PART) != part) {
                level.setBlock(pos, state.setValue(HeatExchangerBlock.PART, part), Block.UPDATE_ALL);
                level.invalidateCapabilities(pos);
            }
        }
    }

    private void process(ServerLevel level, List<BlockPos> structure) {
        HeatExchangerBlockEntity end = level.getBlockEntity(structure.getLast()) instanceof HeatExchangerBlockEntity e
                ? e : null;
        if (end == null || input.getAmountAsInt(0) <= 0 || end.input.getAmountAsInt(0) <= 0) {
            stopPreparing();
            pushOutputs(level, end);
            return;
        }
        var heating = BCEnergyRefineryRecipes.heating(input.getResource(0).value());
        var cooling = BCEnergyRefineryRecipes.cooling(end.input.getResource(0).value());
        if (heating == null || cooling == null
                || BCEnergyRefineryRecipes.heatLevel(cooling.input())
                <= BCEnergyRefineryRecipes.heatLevel(heating.input())) {
            stopPreparing();
            pushOutputs(level, end);
            return;
        }
        if (preparation < PREPARE_TICKS) {
            preparation++;
            setChanged();
        }
        if (preparation >= PREPARE_TICKS) {
            int maximum = THROUGHPUT[structure.size() - 3];
            exchange(end, heating, cooling, maximum);
        }
        pushOutputs(level, end);
    }

    private void exchange(HeatExchangerBlockEntity end,
            BCEnergyRefineryRecipes.HeatExchangeRecipe heating,
            BCEnergyRefineryRecipes.HeatExchangeRecipe cooling, int maximum) {
        int amount = Math.min(maximum, Math.min(input.getAmountAsInt(0), end.input.getAmountAsInt(0)));
        amount = Math.min(amount, TANK_CAPACITY - output.getAmountAsInt(0));
        amount = Math.min(amount, TANK_CAPACITY - end.output.getAmountAsInt(0));
        if (amount <= 0) { stopPreparing(); return; }
        try (Transaction transaction = Transaction.openRoot()) {
            FluidResource heatIn = FluidResource.of(heating.input());
            FluidResource heatOut = FluidResource.of(heating.output());
            FluidResource coolIn = FluidResource.of(cooling.input());
            FluidResource coolOut = FluidResource.of(cooling.output());
            if (input.extract(0, heatIn, amount, transaction) != amount
                    || output.insert(0, heatOut, amount, transaction) != amount
                    || end.input.extract(0, coolIn, amount, transaction) != amount
                    || end.output.insert(0, coolOut, amount, transaction) != amount) return;
            transaction.commit();
        }
        setChanged();
        end.setChanged();
    }

    private void stopPreparing() {
        if (preparation > 0) {
            preparation--;
            setChanged();
        }
    }

    private void pushOutputs(ServerLevel level, @Nullable HeatExchangerBlockEntity end) {
        Direction facing = getBlockState().getValue(HeatExchangerBlock.FACING);
        push(level, output, worldPosition.relative(facing.getClockWise()), facing.getCounterClockWise());
        if (end != null) push(level, end.output, end.worldPosition.above(), Direction.DOWN);
    }

    private static void push(ServerLevel level, ExchangeTank tank, BlockPos targetPos, Direction targetSide) {
        if (tank.getAmountAsInt(0) <= 0) return;
        var target = level.getCapability(Capabilities.Fluid.BLOCK, targetPos, targetSide);
        if (target == null) return;
        FluidResource resource = tank.getResource(0);
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = target.insert(resource, Math.min(1_000, tank.getAmountAsInt(0)), transaction);
            int extracted = tank.extract(0, resource, inserted, transaction);
            if (extracted > 0) transaction.commit();
        }
    }

    @Override protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        input.deserialize(valueInput.childOrEmpty("input"));
        output.deserialize(valueInput.childOrEmpty("output"));
        preparation = Math.clamp(valueInput.getIntOr("preparation", 0), 0, PREPARE_TICKS);
    }

    @Override protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        input.serialize(valueOutput.child("input"));
        output.serialize(valueOutput.child("output"));
        if (preparation > 0) valueOutput.putInt("preparation", preparation);
    }

    private final class ExchangeTank extends FluidStacksResourceHandler {
        private final boolean inputTank;
        private ExchangeTank(boolean inputTank) { super(1, TANK_CAPACITY); this.inputTank = inputTank; }
        @Override public boolean isValid(int index, FluidResource resource) {
            if (!inputTank) return true;
            HeatExchangerBlock.Part part = getBlockState().getValue(HeatExchangerBlock.PART);
            return part == HeatExchangerBlock.Part.START
                    ? BCEnergyRefineryRecipes.heating(resource.value()) != null
                    : part == HeatExchangerBlock.Part.END
                    && BCEnergyRefineryRecipes.cooling(resource.value()) != null;
        }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int inserted = super.insert(index, resource, amount, transaction);
            if (inserted > 0) setChanged();
            return inserted;
        }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            int extracted = super.extract(index, resource, amount, transaction);
            if (extracted > 0) setChanged();
            return extracted;
        }
    }

    private final class InputHandler implements ResourceHandler<FluidResource> {
        @Override public int size() { return input.size(); }
        @Override public FluidResource getResource(int index) { return input.getResource(index); }
        @Override public long getAmountAsLong(int index) { return input.getAmountAsLong(index); }
        @Override public boolean isValid(int index, FluidResource resource) { return input.isValid(index, resource); }
        @Override public long getCapacityAsLong(int index, FluidResource resource) { return input.getCapacityAsLong(index, resource); }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) { return input.insert(index, resource, amount, transaction); }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) { return 0; }
    }

    private final class OutputHandler implements ResourceHandler<FluidResource> {
        @Override public int size() { return output.size(); }
        @Override public FluidResource getResource(int index) { return output.getResource(index); }
        @Override public long getAmountAsLong(int index) { return output.getAmountAsLong(index); }
        @Override public boolean isValid(int index, FluidResource resource) { return false; }
        @Override public long getCapacityAsLong(int index, FluidResource resource) { return output.getCapacityAsLong(index, resource); }
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) { return 0; }
        @Override public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) { return output.extract(index, resource, amount, transaction); }
    }
}
