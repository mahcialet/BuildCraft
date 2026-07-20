package buildcraft.energy;

import buildcraft.energy.gen.OilDepositFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCEnergyFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, BCEnergy.MOD_ID);
    public static final DeferredHolder<Feature<?>, OilDepositFeature> OIL_DEPOSIT =
            FEATURES.register("oil_deposit", OilDepositFeature::new);

    private BCEnergyFeatures() {
    }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
