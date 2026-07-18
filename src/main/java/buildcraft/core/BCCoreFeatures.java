package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.core.gen.WaterSpringFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** World-generation features retained by the core module. */
public final class BCCoreFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(Registries.FEATURE, BuildCraft.MOD_ID);

    public static final DeferredHolder<Feature<?>, WaterSpringFeature> WATER_SPRING =
        FEATURES.register("water_spring", WaterSpringFeature::new);

    private BCCoreFeatures() {
    }

    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
    }
}
