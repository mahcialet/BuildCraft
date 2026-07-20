package buildcraft.transport.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface FacadeAttachment extends PipeAttachment {
    BlockState facadeState(ItemStack stack);
    default boolean isHollow(ItemStack stack) { return false; }
}
