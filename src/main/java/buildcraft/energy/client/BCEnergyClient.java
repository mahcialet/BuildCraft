package buildcraft.energy.client;

import buildcraft.energy.BCEnergy;
import buildcraft.energy.BCEnergyFluids;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;

/** Client-only model registration for Energy fluids. */
public final class BCEnergyClient {
    private BCEnergyClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BCEnergyClient::registerFluidModels);
    }

    private static void registerFluidModels(RegisterFluidModelsEvent event) {
        event.register(model("oil_heat_0"), BCEnergyFluids.OIL, BCEnergyFluids.FLOWING_OIL);
        event.register(model("fuel"), BCEnergyFluids.FUEL_LIGHT, BCEnergyFluids.FLOWING_FUEL_LIGHT);
    }

    private static FluidModel.Unbaked model(String name) {
        return new FluidModel.Unbaked(
            new Material(Identifier.fromNamespaceAndPath(BCEnergy.MOD_ID, "block/fluids/" + name + "_still")),
            new Material(Identifier.fromNamespaceAndPath(BCEnergy.MOD_ID, "block/fluids/" + name + "_flow")),
            null,
            null
        );
    }
}
