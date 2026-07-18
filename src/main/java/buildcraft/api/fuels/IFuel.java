package buildcraft.api.fuels;

import net.neoforged.neoforge.fluids.FluidStack;

/** A liquid fuel definition consumed by BuildCraft combustion engines. */
public interface IFuel {
    FluidStack getFluid();

    long getPowerPerCycle();

    int getTotalBurningTime();
}
