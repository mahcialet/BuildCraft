package buildcraft.energy;

import buildcraft.energy.block.BuildCraftLiquidBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Always-present ambient oil and fuel fluids owned by the Energy module. */
public final class BCEnergyFluids {
    private static final DeferredRegister<FluidType> FLUID_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, BCEnergy.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS =
        DeferredRegister.create(Registries.FLUID, BCEnergy.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BCEnergy.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCEnergy.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> OIL_TYPE = FLUID_TYPES.register("oil",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("fluid.buildcraftenergy.oil")
            .density(900)
            .temperature(300)
            .viscosity(2_000)));
    public static final DeferredHolder<FluidType, FluidType> FUEL_LIGHT_TYPE = FLUID_TYPES.register("fuel_light",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("fluid.buildcraftenergy.fuel_light")
            .density(400)
            .temperature(300)
            .viscosity(600)));

    private static final BaseFlowingFluid.Properties OIL_PROPERTIES =
        new BaseFlowingFluid.Properties(OIL_TYPE, BCEnergyFluids::oil, BCEnergyFluids::flowingOil)
            .block(BCEnergyFluids::oilBlock)
            .bucket(BCEnergyFluids::oilBucket)
            .slopeFindDistance(6)
            .levelDecreasePerBlock(1)
            .tickRate(20)
            .explosionResistance(100);
    private static final BaseFlowingFluid.Properties FUEL_LIGHT_PROPERTIES =
        new BaseFlowingFluid.Properties(FUEL_LIGHT_TYPE, BCEnergyFluids::fuelLight,
            BCEnergyFluids::flowingFuelLight)
            .block(BCEnergyFluids::fuelLightBlock)
            .bucket(BCEnergyFluids::fuelLightBucket)
            .slopeFindDistance(8)
            .levelDecreasePerBlock(1)
            .tickRate(10)
            .explosionResistance(100);

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> OIL =
        FLUIDS.register("oil", () -> new BaseFlowingFluid.Source(OIL_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_OIL =
        FLUIDS.register("flowing_oil", () -> new BaseFlowingFluid.Flowing(OIL_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FUEL_LIGHT =
        FLUIDS.register("fuel_light", () -> new BaseFlowingFluid.Source(FUEL_LIGHT_PROPERTIES));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_FUEL_LIGHT =
        FLUIDS.register("flowing_fuel_light", () -> new BaseFlowingFluid.Flowing(FUEL_LIGHT_PROPERTIES));

    public static final DeferredBlock<BuildCraftLiquidBlock> OIL_BLOCK = BLOCKS.registerBlock("fluid_block_oil_heat_0",
        properties -> new BuildCraftLiquidBlock(BCEnergyFluids::oil, true,
            properties.mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK)
                .replaceable().noCollision().strength(100).pushReaction(PushReaction.DESTROY)
                .noLootTable().liquid()));
    public static final DeferredBlock<BuildCraftLiquidBlock> FUEL_LIGHT_BLOCK =
        BLOCKS.registerBlock("fluid_block_fuel_light_heat_0",
            properties -> new BuildCraftLiquidBlock(BCEnergyFluids::fuelLight, true,
                properties.mapColor(net.minecraft.world.level.material.MapColor.COLOR_YELLOW)
                    .replaceable().noCollision().strength(100).pushReaction(PushReaction.DESTROY)
                    .noLootTable().liquid()));

    public static final DeferredItem<BucketItem> OIL_BUCKET = ITEMS.registerItem("oil_bucket",
        properties -> new BucketItem(oil(), properties.stacksTo(1).craftRemainder(Items.BUCKET)));
    public static final DeferredItem<BucketItem> FUEL_LIGHT_BUCKET = ITEMS.registerItem("fuel_light_bucket",
        properties -> new BucketItem(fuelLight(), properties.stacksTo(1).craftRemainder(Items.BUCKET)));

    private BCEnergyFluids() {
    }

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    public static BaseFlowingFluid.Source oil() { return OIL.get(); }
    public static BaseFlowingFluid.Flowing flowingOil() { return FLOWING_OIL.get(); }
    public static BaseFlowingFluid.Source fuelLight() { return FUEL_LIGHT.get(); }
    public static BaseFlowingFluid.Flowing flowingFuelLight() { return FLOWING_FUEL_LIGHT.get(); }
    public static BuildCraftLiquidBlock oilBlock() { return OIL_BLOCK.get(); }
    public static BuildCraftLiquidBlock fuelLightBlock() { return FUEL_LIGHT_BLOCK.get(); }
    public static BucketItem oilBucket() { return OIL_BUCKET.get(); }
    public static BucketItem fuelLightBucket() { return FUEL_LIGHT_BUCKET.get(); }
}
