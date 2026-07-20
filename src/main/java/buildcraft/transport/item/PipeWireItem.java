package buildcraft.transport.item;

import buildcraft.transport.PipeWireColor;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import buildcraft.core.AdvancementUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Direction;

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
        if (context.getPlayer() instanceof ServerPlayer player) {
            for (Direction direction : Direction.values()) {
                if (!holder.getBlockState().getValue(buildcraft.transport.block.PipeHolderBlock.property(direction))) continue;
                if (context.getLevel().getBlockEntity(holder.getBlockPos().relative(direction))
                        instanceof PipeHolderBlockEntity other && other.hasWire(color)) {
                    AdvancementUtil.award(player, "buildcrafttransport:logic_transportation");
                    break;
                }
            }
        }
        if (context.getPlayer() == null || !context.getPlayer().isCreative()) context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
