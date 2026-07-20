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
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Energy module bootstrap, beginning with its always-present fluid foundation. */
@Mod(BCEnergy.MOD_ID)
public final class BCEnergy {
    public static final String MOD_ID = "buildcraftenergy";

    public BCEnergy(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, BCEnergyConfig.SPEC);
        BCEnergyFluids.register(modBus);
        BCEnergyFeatures.register(modBus);
        BCEnergyBlocks.register(modBus);
        BCEnergyBlockEntities.register(modBus);
        BCEnergyMenus.register(modBus);
        BCEnergyItems.register(modBus);
        NeoForge.EVENT_BUS.addListener(buildcraft.energy.gen.OilBiomeReplacement::serverStarting);
        NeoForge.EVENT_BUS.addListener(buildcraft.energy.gen.OilBiomeReplacement::serverStopped);
        modBus.addListener(this::addCreativeTabContents);
        modBus.addListener(this::commonSetup);
        EnumSpring.OIL.setLiquidBlock(() -> BCEnergyFluids.OIL_BLOCK.get().defaultBlockState());
        buildcraft.lib.fluid.CoolantRegistry.INSTANCE.addCoolant(() -> Fluids.WATER, 0.0023F);
        if (FMLEnvironment.getDist().isClient()) BCEnergyClient.register(modBus);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BCEnergyRefineryRecipes.bootstrap();
            registerRefineryFuels();
        });
    }

    private static void registerRefineryFuels() {
        registerFuel("fuel_gaseous", 8, 3_750);
        registerFuel("fuel_light", 6, 10_000);
        registerFuel("fuel_dense", 4, 90_000);
        registerFuel("fuel_mixed_light", 3, 10_000);
        registerFuel("fuel_mixed_heavy", 5, 19_200);
        registerFuel("oil_distilled", 1, 37_500);
        registerDirtyFuel("oil_dense", 4, 30_000, 500);
        registerDirtyFuel("oil_heavy", 2, 40_000, 333);
        registerDirtyFuel("oil", 3, 5_000, 62);
    }

    private static void registerFuel(String name, int mjPerCycle, int burningTime) {
        for (var variant : BCEnergyFluids.refineryFluid(name).variants()) {
            FuelRegistry.INSTANCE.addFuel(variant.source(), mjPerCycle * buildcraft.api.mj.MjAPI.MJ, burningTime);
        }
    }

    private static void registerDirtyFuel(String name, int mjPerCycle, int burningTime, int residueAmount) {
        var residue = BCEnergyFluids.refineryFluid("oil_residue");
        for (int heat = 0; heat < 3; heat++) {
            FuelRegistry.INSTANCE.addDirtyFuel(BCEnergyFluids.refineryFluid(name).heat(heat).source(),
                mjPerCycle * buildcraft.api.mj.MjAPI.MJ, burningTime,
                residue.heat(heat).source(), residueAmount);
        }
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) {
            event.accept(ItemBlockEngine.stirlingEngine());
            event.accept(ItemBlockEngine.combustionEngine());
            if (BCEnergyConfig.ENABLE_RF_ENGINE.get()) event.accept(ItemBlockEngine.rfEngine());
            event.accept(BCEnergyFluids.OIL_BUCKET.get());
            event.accept(BCEnergyFluids.FUEL_LIGHT_BUCKET.get());
            for (var family : BCEnergyFluids.REFINERY_FLUIDS.values()) {
                var bucket = family.heat(0).bucket().get();
                if (bucket != BCEnergyFluids.OIL_BUCKET.get() && bucket != BCEnergyFluids.FUEL_LIGHT_BUCKET.get()) {
                    event.accept(bucket);
                }
            }
            if (BCEnergyConfig.ENABLE_MJ_DYNAMO.get()) event.accept(BCEnergyItems.MJ_DYNAMO.get());
        event.accept(BCEnergyItems.GLOB_OF_OIL.get());
        }
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(BCEnergyFluids.OIL_BUCKET.get());
            event.accept(BCEnergyFluids.FUEL_LIGHT_BUCKET.get());
            for (var family : BCEnergyFluids.REFINERY_FLUIDS.values()) {
                var bucket = family.heat(0).bucket().get();
                if (bucket != BCEnergyFluids.OIL_BUCKET.get() && bucket != BCEnergyFluids.FUEL_LIGHT_BUCKET.get()) {
                    event.accept(bucket);
                }
            }
        }
    }
}
