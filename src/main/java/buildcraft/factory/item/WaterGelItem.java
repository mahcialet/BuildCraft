package buildcraft.factory.item;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.WaterGelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class WaterGelItem extends Item {
    public WaterGelItem(Properties properties) { super(properties.stacksTo(16)); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.FAIL;
        BlockPos pos = hit.getBlockPos();
        if (!level.getFluidState(pos).is(Fluids.WATER) || !level.getFluidState(pos).isSource()) {
            return InteractionResult.FAIL;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
            SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide()) {
            level.setBlock(pos, BCFactoryBlocks.WATER_GEL.get().defaultBlockState()
                .setValue(WaterGelBlock.STAGE, WaterGelBlock.Stage.SPREAD_0), Block.UPDATE_ALL);
            level.scheduleTick(pos, BCFactoryBlocks.WATER_GEL.get(), 200);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
