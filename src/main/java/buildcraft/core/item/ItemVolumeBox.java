package buildcraft.core.item;

import buildcraft.core.marker.VolumeBoxSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Places a new one-block freely editable volume adjacent to the clicked face. */
public final class ItemVolumeBox extends Item {
    public ItemVolumeBox(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        boolean added = VolumeBoxSavedData.get((ServerLevel) context.getLevel()).add(
            context.getClickedPos().relative(context.getClickedFace())
        );
        return added ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
}
