package buildcraft.api.items;

import net.minecraft.world.item.ItemStack;

/** Public contract for an item-backed filter list. */
public interface IList {
    String getLabel(ItemStack stack);

    boolean setLabel(ItemStack stack, String name);

    boolean matches(ItemStack listStack, ItemStack target);
}
