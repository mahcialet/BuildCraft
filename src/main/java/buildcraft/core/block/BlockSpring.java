package buildcraft.core.block;

import buildcraft.api.enums.EnumSpring;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Unbreakable source block that replenishes the fluid directly above it. */
public final class BlockSpring extends Block {
    public static final MapCodec<BlockSpring> CODEC = simpleCodec(BlockSpring::new);
    public static final EnumProperty<EnumSpring> SPRING_TYPE = EnumProperty.create("type", EnumSpring.class);

    public BlockSpring(BlockBehaviour.Properties properties) {
        super(properties.strength(-1.0F, 3_600_000.0F).sound(SoundType.STONE));
        registerDefaultState(stateDefinition.any().setValue(SPRING_TYPE, EnumSpring.WATER));
    }

    @Override
    protected MapCodec<? extends BlockSpring> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPRING_TYPE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) level.scheduleTick(pos, this, state.getValue(SPRING_TYPE).tickRate);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        tryGenerate(level, pos, state, random);
    }

    public boolean tryGenerate(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        EnumSpring spring = state.getValue(SPRING_TYPE);
        level.scheduleTick(pos, this, spring.tickRate);
        BlockState liquid = spring.liquidBlock();
        if (!spring.canGen || liquid == null || !level.isEmptyBlock(pos.above())) return false;
        if (spring.chance != -1 && random.nextInt(spring.chance) != 0) return false;
        return level.setBlock(pos.above(), liquid, Block.UPDATE_ALL);
    }
}
