package buildcraft.energy.block;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

/** Public LiquidBlock wrapper with a codec bound to its registered fluid. */
public final class BuildCraftLiquidBlock extends LiquidBlock {
    private final BooleanSupplier flammable;

    public BuildCraftLiquidBlock(Supplier<? extends FlowingFluid> fluid, boolean flammable,
        BlockBehaviour.Properties properties) {
        this(fluid, () -> flammable, properties);
    }

    public BuildCraftLiquidBlock(Supplier<? extends FlowingFluid> fluid, BooleanSupplier flammable,
        BlockBehaviour.Properties properties) {
        super(fluid.get(), properties);
        this.flammable = flammable;
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammable.getAsBoolean();
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammable.getAsBoolean() ? 100 : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammable.getAsBoolean() ? 100 : 0;
    }
}
