package buildcraft.api.fuels;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/** Converts a matching solid item into an equivalent coolant volume. */
public interface ISolidCoolant {
    @Nullable FluidStack getFluidFromSolidCoolant(ItemStack stack);
}
