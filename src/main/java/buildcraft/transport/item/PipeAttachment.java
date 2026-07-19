package buildcraft.transport.item;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public interface PipeAttachment {
    default void tickAttachment(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {}
    default InteractionResult useAttachment(PipeHolderBlockEntity pipe, Direction side, ItemStack stack,
                                            Player player) {
        return InteractionResult.PASS;
    }
}
