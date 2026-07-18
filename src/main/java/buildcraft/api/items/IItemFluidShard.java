package buildcraft.api.items;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Creates disposable item drops for fluid that cannot remain in a broken tank. */
public interface IItemFluidShard {
    void addFluidDrops(List<ItemStack> drops, FluidStack fluid);
}
