package buildcraft.transport.item;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public interface LensAttachment extends PipeAttachment {
    DyeColor lensColor(ItemStack stack);
    boolean isFilter(ItemStack stack);
}
