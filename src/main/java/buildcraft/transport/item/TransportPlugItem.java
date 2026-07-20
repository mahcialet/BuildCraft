package buildcraft.transport.item;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import buildcraft.core.AdvancementUtil;
import net.minecraft.server.level.ServerPlayer;

public class TransportPlugItem extends Item implements PipeAttachment {
    public TransportPlugItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PipeHolderBlockEntity holder)) {
            return InteractionResult.PASS;
        }
        boolean connected = holder.getBlockState().getValue(
            buildcraft.transport.block.PipeHolderBlock.property(context.getClickedFace()));
        if (!context.getLevel().isClientSide()
                && holder.installAttachment(context.getClickedFace(), context.getItemInHand())) {
            context.getItemInHand().shrink(1);
            if (connected && this instanceof BlockerPlugItem && context.getPlayer() instanceof ServerPlayer player) {
                AdvancementUtil.award(player, "buildcrafttransport:plugging_the_gap");
            }
        }
        return InteractionResult.SUCCESS;
    }
}
