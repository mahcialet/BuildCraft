package buildcraft.transport.item;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public interface PipeAttachment {
    default void tickAttachment(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {}
}
