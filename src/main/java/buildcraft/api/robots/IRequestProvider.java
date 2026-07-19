package buildcraft.api.robots;

import net.minecraft.world.item.ItemStack;

public interface IRequestProvider {
    int getRequestsCount();
    ItemStack getRequest(int index);
    ItemStack offerItem(int index, ItemStack stack);
}
