package buildcraft.silicon.block;

import buildcraft.silicon.block.entity.ChargingTableBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ChargingTableBlock extends BaseEntityBlock {
    public static final MapCodec<ChargingTableBlock> CODEC = simpleCodec(ChargingTableBlock::new);
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 9 / 16.0, 1);
    public ChargingTableBlock(BlockBehaviour.Properties properties) { super(properties.strength(10).sound(SoundType.METAL).noOcclusion()); }
    public ChargingTableBlock() { this(BlockBehaviour.Properties.of()); }
    @Override protected MapCodec<? extends ChargingTableBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ChargingTableBlockEntity(pos, state); }
}
