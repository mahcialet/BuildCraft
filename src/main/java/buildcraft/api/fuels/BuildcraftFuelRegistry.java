package buildcraft.api.fuels;

import buildcraft.lib.fluid.FuelRegistry;
import buildcraft.lib.fluid.CoolantRegistry;

/** Public access point retained from the historical BuildCraft fuel API. */
public final class BuildcraftFuelRegistry {
    public static final IFuelManager fuel = FuelRegistry.INSTANCE;
    public static final ICoolantManager coolant = CoolantRegistry.INSTANCE;

    private BuildcraftFuelRegistry() {
    }
}
