package buildcraft.silicon.block;

import buildcraft.silicon.BCSiliconBlockEntities;
import buildcraft.silicon.block.entity.AssemblyTableBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class AssemblyTableBlock extends BaseEntityBlock {
    public static final MapCodec<AssemblyTableBlock> CODEC = simpleCodec(AssemblyTableBlock::new);
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 9 / 16.0, 1);
    public AssemblyTableBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(10.0F).sound(SoundType.METAL).noOcclusion());
    }
    public AssemblyTableBlock() { this(BlockBehaviour.Properties.of()); }
    @Override protected MapCodec<? extends AssemblyTableBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                             BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AssemblyTableBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BCSiliconBlockEntities.ASSEMBLY_TABLE.get(), AssemblyTableBlockEntity::tick);
    }
}
