package buildcraft.api.lists;

import net.minecraft.world.item.ItemStack;

/** Extensible matching rule used by material, type, and class list modes. */
public interface ListMatchHandler {
    boolean matches(ListMatchMode mode, ItemStack source, ItemStack target, boolean precise);
}
