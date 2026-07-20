package buildcraft.core.block;

import buildcraft.core.BCCoreBlockEntities;
import buildcraft.core.block.entity.PowerTesterBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class PowerTesterBlock extends BaseEntityBlock {
    public static final MapCodec<PowerTesterBlock> CODEC = simpleCodec(PowerTesterBlock::new);
    public PowerTesterBlock(BlockBehaviour.Properties properties) { super(properties.strength(5, 10).sound(SoundType.METAL)); }
    @Override protected MapCodec<? extends PowerTesterBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PowerTesterBlockEntity(pos, state); }
    @Override public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BCCoreBlockEntities.POWER_TESTER.get(), PowerTesterBlockEntity::tick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof PowerTesterBlockEntity tester) {
            server.sendOverlayMessage(Component.translatable("chat.buildcraftcore.power_tester",
                    format(tester.lastReceived()), format(tester.tickReceived()), format(tester.totalReceived())));
        }
        return InteractionResult.SUCCESS;
    }
    private static String format(long value) { return String.format(java.util.Locale.ROOT, "%.3f MJ", value / 1_000_000.0); }
}
