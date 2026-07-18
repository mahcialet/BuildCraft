package buildcraft.api.fuels;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/** Mutable coolant registry exposed for BuildCraft integrations. */
public interface ICoolantManager {
    <C extends ICoolant> C addCoolant(C coolant);

    <C extends ISolidCoolant> C addSolidCoolant(C coolant);

    ICoolant addCoolant(FluidStack fluid, float degreesCoolingPerMB);

    ISolidCoolant addSolidCoolant(ItemStack solid, FluidStack fluid, float multiplier);

    Collection<ICoolant> getCoolants();

    Collection<ISolidCoolant> getSolidCoolants();

    @Nullable ICoolant getCoolant(FluidStack fluid);

    float getDegreesPerMb(FluidStack fluid, float heat);

    @Nullable ISolidCoolant getSolidCoolant(ItemStack solid);
}
