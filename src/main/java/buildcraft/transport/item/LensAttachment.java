package buildcraft.transport.item;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public interface LensAttachment extends PipeAttachment {
    Optional<DyeColor> lensColor(ItemStack stack);
    boolean isFilter(ItemStack stack);
}
