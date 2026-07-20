package buildcraft.core.item;

import buildcraft.api.tools.IToolWrench;
import buildcraft.api.tools.IWrenchable;
import buildcraft.core.AdvancementUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/** BuildCraft's non-consumable block rotation tool. */
public final class ItemWrench extends Item implements IToolWrench {
    public ItemWrench(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!canWrench(context)) {
            return InteractionResult.PASS;
        }

        BlockState currentState = context.getLevel().getBlockState(context.getClickedPos());
        if (currentState.getBlock() instanceof IWrenchable wrenchable) {
            InteractionResult customResult = wrenchable.onWrenched(context);
            if (customResult.consumesAction()) {
                if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
                    AdvancementUtil.award(player, "buildcraftcore:wrenched");
                }
                wrenchUsed(context);
                return customResult;
            }
            if (customResult == InteractionResult.FAIL) {
                return customResult;
            }
        }

        BlockState rotatedState = currentState.rotate(
            context.getLevel(), context.getClickedPos(), Rotation.CLOCKWISE_90
        );
        if (rotatedState == currentState) {
            return InteractionResult.PASS;
        }

        if (!context.getLevel().isClientSide()) {
            context.getLevel().setBlock(context.getClickedPos(), rotatedState, Block.UPDATE_ALL);
            context.getLevel().playSound(
                context.getPlayer(),
                context.getClickedPos(),
                SoundEvents.PISTON_CONTRACT,
                SoundSource.BLOCKS,
                0.5F,
                0.8F
            );
            if (context.getPlayer() instanceof ServerPlayer player) {
                AdvancementUtil.award(player, "buildcraftcore:wrenched");
            }
        }
        wrenchUsed(context);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canWrench(UseOnContext context) {
        return context.getPlayer() == null || context.getPlayer().mayBuild();
    }

    @Override
    public void wrenchUsed(UseOnContext context) {
        // Reserved for BuildCraft action criteria and other server-side hooks.
        // InteractionResult.SUCCESS requests the hand animation from the client.
    }
}
