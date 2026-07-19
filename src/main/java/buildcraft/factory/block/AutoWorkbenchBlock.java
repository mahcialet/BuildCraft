package buildcraft.factory.block;

import buildcraft.factory.BCFactoryBlockEntities;
import buildcraft.factory.block.entity.AutoWorkbenchBlockEntity;
import buildcraft.factory.menu.AutoWorkbenchMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class AutoWorkbenchBlock extends BaseEntityBlock {
    public static final MapCodec<AutoWorkbenchBlock> CODEC = simpleCodec(AutoWorkbenchBlock::new);

    public AutoWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(2.0F).sound(SoundType.WOOD));
    }

    public AutoWorkbenchBlock() { this(BlockBehaviour.Properties.of()); }

    @Override protected MapCodec<? extends AutoWorkbenchBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoWorkbenchBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) AutoWorkbenchMenu.open(serverPlayer, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type
    ) {
        return createTickerHelper(type, BCFactoryBlockEntities.AUTO_WORKBENCH.get(), AutoWorkbenchBlockEntity::tick);
    }
}
