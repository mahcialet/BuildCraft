package buildcraft.energy;

import buildcraft.api.enums.EnumSpring;
import buildcraft.lib.fluid.FuelRegistry;
import buildcraft.core.BCCreativeTabs;
import buildcraft.core.item.ItemBlockEngine;
import buildcraft.energy.client.BCEnergyClient;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Energy module bootstrap, beginning with its always-present fluid foundation. */
@Mod(BCEnergy.MOD_ID)
public final class BCEnergy {
    public static final String MOD_ID = "buildcraftenergy";

    public BCEnergy(IEventBus modBus) {
        BCEnergyFluids.register(modBus);
        BCEnergyBlockEntities.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
        EnumSpring.OIL.setLiquidBlock(() -> BCEnergyFluids.OIL_BLOCK.get().defaultBlockState());
        FuelRegistry.INSTANCE.addFuel(BCEnergyFluids.OIL, 3_000_000L, 10_000);
        FuelRegistry.INSTANCE.addFuel(BCEnergyFluids.FUEL_LIGHT, 6_000_000L, 15_000);
        buildcraft.lib.fluid.CoolantRegistry.INSTANCE.addCoolant(() -> Fluids.WATER, 0.0023F);
        if (FMLEnvironment.getDist().isClient()) BCEnergyClient.register(modBus);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) {
            event.accept(ItemBlockEngine.stirlingEngine());
            event.accept(ItemBlockEngine.combustionEngine());
            event.accept(ItemBlockEngine.rfEngine());
            event.accept(BCEnergyFluids.OIL_BUCKET.get());
            event.accept(BCEnergyFluids.FUEL_LIGHT_BUCKET.get());
        }
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(BCEnergyFluids.OIL_BUCKET.get());
            event.accept(BCEnergyFluids.FUEL_LIGHT_BUCKET.get());
        }
    }
}
