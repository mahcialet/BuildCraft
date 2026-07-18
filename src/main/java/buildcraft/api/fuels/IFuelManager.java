package buildcraft.api.fuels;

import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/** Mutable fuel registry exposed for BuildCraft modules and integrations. */
public interface IFuelManager {
    <F extends IFuel> F addFuel(F fuel);

    IFuel addFuel(FluidStack fluid, long powerPerCycle, int totalBurningTime);

    IDirtyFuel addDirtyFuel(FluidStack fluid, long powerPerCycle, int totalBurningTime, FluidStack residue);

    Collection<IFuel> getFuels();

    @Nullable IFuel getFuel(FluidStack fluid);

    interface IDirtyFuel extends IFuel {
        FluidStack getResidue();
    }
}
