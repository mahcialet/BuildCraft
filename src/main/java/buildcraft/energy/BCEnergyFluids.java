package buildcraft.energy;

import buildcraft.energy.block.BuildCraftLiquidBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
        properties -> new BuildCraftLiquidBlock(BCEnergyFluids::oil,
            () -> BCEnergyConfig.OIL_CAN_BURN.get(),
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

    public record RefineryFluidVariant(
        String name,
        int heat,
        String textureName,
        int tint,
        DeferredHolder<FluidType, FluidType> type,
        DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
        DeferredBlock<BuildCraftLiquidBlock> block,
        DeferredItem<BucketItem> bucket
    ) {}

    public record RefineryFluidFamily(String name, List<RefineryFluidVariant> variants) {
        public RefineryFluidVariant heat(int heat) { return variants.get(heat); }
    }

    /** All ten historical refinery fluids, each retaining its three heat states. */
    public static final Map<String, RefineryFluidFamily> REFINERY_FLUIDS = createRefineryFluids();

    private static Map<String, RefineryFluidFamily> createRefineryFluids() {
        Map<String, RefineryFluidFamily> families = new LinkedHashMap<>();
        families.put("oil", familyWithExistingCold("oil", 900, 2_000, 3, 6, true,
            MapColor.COLOR_BLACK, 0xFF505050, OIL_TYPE, OIL, FLOWING_OIL, OIL_BLOCK, OIL_BUCKET, "oil_heat_0"));
        families.put("oil_residue", family("oil_residue", 1_200, 4_000, 3, 4, false,
            MapColor.COLOR_PURPLE, 0xFF100F10));
        families.put("oil_heavy", family("oil_heavy", 850, 1_800, 3, 6, true,
            MapColor.COLOR_BROWN, 0xFFA08F1F));
        families.put("oil_dense", family("oil_dense", 950, 1_600, 3, 5, true,
            MapColor.COLOR_GRAY, 0xFF876E77));
        families.put("oil_distilled", family("oil_distilled", 750, 1_400, 2, 8, true,
            MapColor.COLOR_ORANGE, 0xFFE4AF78));
        families.put("fuel_dense", family("fuel_dense", 600, 800, 2, 7, true,
            MapColor.COLOR_ORANGE, 0xFFFFAF3F));
        families.put("fuel_mixed_heavy", family("fuel_mixed_heavy", 700, 1_000, 2, 7, true,
            MapColor.COLOR_YELLOW, 0xFFF2A700));
        families.put("fuel_light", familyWithExistingCold("fuel_light", 400, 600, 1, 8, true,
            MapColor.COLOR_YELLOW, 0xFFFFFF30, FUEL_LIGHT_TYPE, FUEL_LIGHT, FLOWING_FUEL_LIGHT, FUEL_LIGHT_BLOCK,
            FUEL_LIGHT_BUCKET, "fuel"));
        families.put("fuel_mixed_light", family("fuel_mixed_light", 650, 900, 1, 9, true,
            MapColor.COLOR_YELLOW, 0xFFF6D700));
        families.put("fuel_gaseous", family("fuel_gaseous", 300, 500, 0, 10, true,
            MapColor.COLOR_YELLOW, 0xFFFAF630));
        return Map.copyOf(families);
    }

    private static RefineryFluidFamily familyWithExistingCold(String name, int density, int viscosity,
        int boilPoint, int spread, boolean flammable, MapColor color, int tint,
        DeferredHolder<FluidType, FluidType> type,
        DeferredHolder<Fluid, BaseFlowingFluid.Source> source,
        DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing,
        DeferredBlock<BuildCraftLiquidBlock> block,
        DeferredItem<BucketItem> bucket,
        String coldTexture
    ) {
        return new RefineryFluidFamily(name, List.of(
            new RefineryFluidVariant(name, 0, coldTexture, 0xFFFFFFFF, type, source, flowing, block, bucket),
            registerVariant(name, 1, density, viscosity, boilPoint, spread, flammable, color, tint),
            registerVariant(name, 2, density, viscosity, boilPoint, spread, flammable, color, tint)
        ));
    }

    private static RefineryFluidFamily family(String name, int density, int viscosity, int boilPoint,
        int spread, boolean flammable, MapColor color, int tint) {
        return new RefineryFluidFamily(name, List.of(
            registerVariant(name, 0, density, viscosity, boilPoint, spread, flammable, color, tint),
            registerVariant(name, 1, density, viscosity, boilPoint, spread, flammable, color, tint),
            registerVariant(name, 2, density, viscosity, boilPoint, spread, flammable, color, tint)
        ));
    }

    private static RefineryFluidVariant registerVariant(String baseName, int heat, int baseDensity,
        int baseViscosity, int boilPoint, int spread, boolean flammable, MapColor color, int tint) {
        String name = heat == 0 ? baseName : baseName + "_heat_" + heat;
        int density = baseDensity * (heat >= boilPoint ? -1 : 1);
        int viscosity = baseViscosity * (4 - heat) / 4;
        var type = FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
            .descriptionId("fluid.buildcraftenergy." + name)
            .density(density).temperature(300 + heat * 20).viscosity(viscosity)));
        var sourceRef = new AtomicReference<DeferredHolder<Fluid, BaseFlowingFluid.Source>>();
        var flowingRef = new AtomicReference<DeferredHolder<Fluid, BaseFlowingFluid.Flowing>>();
        var blockRef = new AtomicReference<DeferredBlock<BuildCraftLiquidBlock>>();
        var bucketRef = new AtomicReference<DeferredItem<BucketItem>>();
        BaseFlowingFluid.Properties fluidProperties = new BaseFlowingFluid.Properties(
            type, () -> sourceRef.get().get(), () -> flowingRef.get().get())
            .block(() -> blockRef.get().get())
            .bucket(() -> bucketRef.get().get())
            .slopeFindDistance(spread)
            .levelDecreasePerBlock(1)
            .tickRate(Math.max(5, viscosity / 100))
            .explosionResistance(100);
        var source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(fluidProperties));
        var flowing = FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(fluidProperties));
        sourceRef.set(source);
        flowingRef.set(flowing);
        var block = BLOCKS.registerBlock("fluid_block_" + baseName + "_heat_" + heat,
            properties -> new BuildCraftLiquidBlock(() -> source.get(), flammable,
                properties.mapColor(color).replaceable().noCollision().strength(100)
                    .pushReaction(PushReaction.DESTROY).noLootTable().liquid()));
        blockRef.set(block);
        var bucket = ITEMS.registerItem(name + "_bucket",
            properties -> new BucketItem(source.get(), properties.stacksTo(1).craftRemainder(Items.BUCKET)));
        bucketRef.set(bucket);
        return new RefineryFluidVariant(name, heat, "heat_" + heat, tint, type, source, flowing, block, bucket);
    }

    public static RefineryFluidFamily refineryFluid(String name) {
        RefineryFluidFamily family = REFINERY_FLUIDS.get(name);
        if (family == null) throw new IllegalArgumentException("Unknown refinery fluid " + name);
        return family;
    }

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
