package buildcraft.transport.item;

import buildcraft.transport.PipeWireColor;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class PipeWireItem extends Item {
    private final PipeWireColor color;

    public PipeWireItem(PipeWireColor color, Properties properties) {
        super(properties);
        this.color = color;
    }

    public PipeWireColor color() {
        return color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PipeHolderBlockEntity holder)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            if (!holder.removeWire(color)) return InteractionResult.FAIL;
            if (!context.getPlayer().isCreative()) {
                var returned = new net.minecraft.world.item.ItemStack(this);
                if (!context.getPlayer().addItem(returned)) context.getPlayer().drop(returned, false);
            }
            return InteractionResult.SUCCESS;
        }
        if (!holder.installWire(color)) return InteractionResult.FAIL;
        if (context.getPlayer() == null || !context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
