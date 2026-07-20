package buildcraft.energy.block;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

/** Public LiquidBlock wrapper with a codec bound to its registered fluid. */
public final class BuildCraftLiquidBlock extends LiquidBlock {
    private final BooleanSupplier flammable;
    private final BooleanSupplier sticky;

    public BuildCraftLiquidBlock(Supplier<? extends FlowingFluid> fluid, boolean flammable,
        BlockBehaviour.Properties properties) {
        this(fluid, () -> flammable, () -> false, properties);
    }

    public BuildCraftLiquidBlock(Supplier<? extends FlowingFluid> fluid, BooleanSupplier flammable,
        BlockBehaviour.Properties properties) {
        this(fluid, flammable, () -> false, properties);
    }

    public BuildCraftLiquidBlock(Supplier<? extends FlowingFluid> fluid, BooleanSupplier flammable,
        BooleanSupplier sticky, BlockBehaviour.Properties properties) {
        super(fluid.get(), properties);
        this.flammable = flammable;
        this.sticky = sticky;
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

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
        InsideBlockEffectApplier effects, boolean intersects) {
        if (sticky.getAsBoolean()) entity.makeStuckInBlock(state, new Vec3(0.25, 0.05, 0.25));
    }

    public boolean isSticky() {
        return sticky.getAsBoolean();
    }
}
