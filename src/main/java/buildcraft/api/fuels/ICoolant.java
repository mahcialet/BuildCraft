package buildcraft.api.fuels;

import net.neoforged.neoforge.fluids.FluidStack;

/** A fluid capable of removing heat from a combustion engine. */
public interface ICoolant {
    boolean matchesFluid(FluidStack stack);

    /** Returns degrees of heat removed by one millibucket at the supplied temperature. */
    float getDegreesCoolingPerMB(FluidStack stack, float heat);
}
