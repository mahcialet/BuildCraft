package buildcraft.silicon.item;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class PipePlugItem extends Item {
    public PipePlugItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PipeHolderBlockEntity holder)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!holder.installAttachment(context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        if (context.getPlayer() == null || !context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
