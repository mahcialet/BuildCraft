package buildcraft.api.fuels;

import buildcraft.lib.fluid.FuelRegistry;

/** Public access point retained from the historical BuildCraft fuel API. */
public final class BuildcraftFuelRegistry {
    public static final IFuelManager fuel = FuelRegistry.INSTANCE;

    private BuildcraftFuelRegistry() {
    }
}
