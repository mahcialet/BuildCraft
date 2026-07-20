package buildcraft.silicon.item;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import buildcraft.transport.item.PipeAttachment;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import buildcraft.core.AdvancementUtil;
import net.minecraft.server.level.ServerPlayer;

public class PipePlugItem extends Item implements PipeAttachment {
    public PipePlugItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PipeHolderBlockEntity holder)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!holder.installAttachment(context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        if (this instanceof GateItem && context.getPlayer() instanceof ServerPlayer player) {
            AdvancementUtil.award(player, "buildcrafttransport:pipe_logic");
            if (GateItem.slots(context.getItemInHand()) > 1) {
                AdvancementUtil.award(player, "buildcrafttransport:extended_logic");
            }
        }
        if (context.getPlayer() == null || !context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
