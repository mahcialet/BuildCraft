package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.api.enums.EnumDecoratedBlock;
import buildcraft.core.block.BlockDecoration;
import buildcraft.core.gametest.BuildCraftGameTestInstance;
import buildcraft.core.item.ItemBlockDecoration;
import buildcraft.core.item.ItemMarkerConnector;
import buildcraft.core.item.ItemMapLocation;
import buildcraft.api.items.MapLocationData;
import buildcraft.api.items.MapLocationType;
import buildcraft.api.items.PaintbrushData;
import buildcraft.core.item.ItemPaintbrush;
import buildcraft.api.items.ListData;
import buildcraft.api.items.ListLineData;
import buildcraft.api.lists.ListMatchMode;
import buildcraft.core.item.ItemList;
import buildcraft.core.menu.ListMenu;
import buildcraft.core.marker.VolumeBox;
import buildcraft.core.marker.VolumeBoxSavedData;
import buildcraft.api.items.FluidItemDrops;
import buildcraft.core.item.ItemFragileFluidContainer;
import buildcraft.api.enums.EnumSpring;
import buildcraft.core.block.BlockSpring;
import buildcraft.core.item.ItemBlockSpring;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.api.mj.MjEnergyAdapter;
import buildcraft.api.mj.MjRfConversion;
import buildcraft.api.mj.IMjToRfStatus;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.fuels.BuildcraftFuelRegistry;
import buildcraft.api.fuels.IFuel;
import buildcraft.core.block.BlockEngine;
import buildcraft.core.block.entity.RedstoneEngineBlockEntity;
import buildcraft.core.block.entity.CreativeEngineBlockEntity;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.energy.block.entity.StirlingEngineBlockEntity;
import buildcraft.energy.block.entity.CombustionEngineBlockEntity;
import buildcraft.energy.block.entity.RfEngineBlockEntity;
import buildcraft.energy.menu.EngineMenu;
import buildcraft.lib.mj.MjRedstoneBatteryReceiver;
import buildcraft.core.marker.PathConnection;
import buildcraft.core.marker.PathSavedData;
import buildcraft.core.marker.VolumeConnection;
import buildcraft.core.marker.VolumeSavedData;
import buildcraft.core.block.entity.PathMarkerBlockEntity;
import buildcraft.core.block.entity.VolumeMarkerBlockEntity;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Executable regression tests for migrated BuildCraft core behavior. */
public final class BCCoreGameTests {
    private static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_TYPES =
        DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, BuildCraft.MOD_ID);

    public static final DeferredHolder<
        MapCodec<? extends GameTestInstance>, MapCodec<BuildCraftGameTestInstance>
    > CODE_TEST = TEST_TYPES.register("code", () -> BuildCraftGameTestInstance.CODEC);

    private BCCoreGameTests() {
    }

    public static void register(IEventBus modBus) {
        TEST_TYPES.register(modBus);
        modBus.addListener(BCCoreGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("core"));
        Holder<TestEnvironmentDefinition<?>> diagnosticsEnvironment = event.registerEnvironment(id("core_diagnostics"));
        Holder<TestEnvironmentDefinition<?>> siliconPlaceholderEnvironment = event.registerEnvironment(id("silicon_placeholders"));
        Holder<TestEnvironmentDefinition<?>> gateParameterEnvironment = event.registerEnvironment(id("gate_parameters"));
        Holder<TestEnvironmentDefinition<?>> fillerGateEnvironment = event.registerEnvironment(id("filler_gate_patterns"));
        Holder<TestEnvironmentDefinition<?>> roboticsGateListEnvironment = event.registerEnvironment(id("robotics_gate_lists"));
        Holder<TestEnvironmentDefinition<?>> transportCreativeEnvironment = event.registerEnvironment(id("transport_creative"));
        Holder<TestEnvironmentDefinition<?>> siliconLensEnvironment = event.registerEnvironment(id("silicon_lens_variants"));
        Holder<TestEnvironmentDefinition<?>> siliconSensorTimerEnvironment =
                event.registerEnvironment(id("silicon_sensor_timer"));
        Holder<TestEnvironmentDefinition<?>> pickerEnvironment =
            event.registerEnvironment(id("robotics_picker"));
        Holder<TestEnvironmentDefinition<?>> fluidCarrierEnvironment =
            event.registerEnvironment(id("robotics_fluid_carrier"));
        Holder<TestEnvironmentDefinition<?>> lumberjackEnvironment =
            event.registerEnvironment(id("robotics_lumberjack"));
        Holder<TestEnvironmentDefinition<?>> harvesterEnvironment =
            event.registerEnvironment(id("robotics_harvester"));
        Holder<TestEnvironmentDefinition<?>> minerEnvironment =
            event.registerEnvironment(id("robotics_miner"));
        Holder<TestEnvironmentDefinition<?>> planterEnvironment =
            event.registerEnvironment(id("robotics_planter"));
        Holder<TestEnvironmentDefinition<?>> farmerEnvironment =
            event.registerEnvironment(id("robotics_farmer"));
        Holder<TestEnvironmentDefinition<?>> leafCutterEnvironment =
                event.registerEnvironment(id("robotics_leaf_cutter"));
        Holder<TestEnvironmentDefinition<?>> shovelmanEnvironment =
                event.registerEnvironment(id("robotics_shovelman"));
        Holder<TestEnvironmentDefinition<?>> butcherEnvironment =
                event.registerEnvironment(id("robotics_butcher"));
        Holder<TestEnvironmentDefinition<?>> pumpEnvironment =
                event.registerEnvironment(id("robotics_pump"));
        Holder<TestEnvironmentDefinition<?>> knightEnvironment =
                event.registerEnvironment(id("robotics_knight"));
        Holder<TestEnvironmentDefinition<?>> bomberEnvironment =
                event.registerEnvironment(id("robotics_bomber"));
        Holder<TestEnvironmentDefinition<?>> stripesEnvironment =
                event.registerEnvironment(id("robotics_stripes"));
        Holder<TestEnvironmentDefinition<?>> builderRobotEnvironment =
                event.registerEnvironment(id("robotics_builder_robot"));
        registerTest(event, pickerEnvironment, "robotics_picker_robot", BCCoreGameTests::roboticsPickerRobot);
        registerTest(event, lumberjackEnvironment, "robotics_lumberjack_robot",
            BCCoreGameTests::roboticsLumberjackRobot);
        registerTest(event, harvesterEnvironment, "robotics_harvester_robot",
            BCCoreGameTests::roboticsHarvesterRobot);
        registerTest(event, minerEnvironment, "robotics_miner_robot",
            BCCoreGameTests::roboticsMinerRobot);
        registerTest(event, planterEnvironment, "robotics_planter_robot",
            BCCoreGameTests::roboticsPlanterRobot);
        registerTest(event, farmerEnvironment, "robotics_farmer_robot",
            BCCoreGameTests::roboticsFarmerRobot);
        registerTest(event, leafCutterEnvironment, "robotics_leaf_cutter_robot",
                BCCoreGameTests::roboticsLeafCutterRobot);
        registerTest(event, shovelmanEnvironment, "robotics_shovelman_robot",
                BCCoreGameTests::roboticsShovelmanRobot);
        registerTest(event, butcherEnvironment, "robotics_butcher_robot",
                BCCoreGameTests::roboticsButcherRobot);
        registerTest(event, pumpEnvironment, "robotics_pump_robot",
                BCCoreGameTests::roboticsPumpRobot);
        registerTest(event, knightEnvironment, "robotics_knight_robot",
                BCCoreGameTests::roboticsKnightRobot);
        registerTest(event, bomberEnvironment, "robotics_bomber_robot",
                BCCoreGameTests::roboticsBomberRobot);
        registerTest(event, stripesEnvironment, "robotics_stripes_robot",
                BCCoreGameTests::roboticsStripesRobot);
        registerTest(event, builderRobotEnvironment, "robotics_builder_robot",
                BCCoreGameTests::roboticsBuilderRobot);
        registerTest(event, environment, "decoration_states", BCCoreGameTests::decorationStates);
        registerTest(event, environment, "wrench_rotation", BCCoreGameTests::wrenchRotation);
        registerTest(event, environment, "path_graph", BCCoreGameTests::pathGraph);
        registerTest(event, environment, "path_marker_sync", BCCoreGameTests::pathMarkerSync);
        registerTest(event, environment, "volume_graph", BCCoreGameTests::volumeGraph);
        registerTest(event, environment, "volume_marker_sync", BCCoreGameTests::volumeMarkerSync);
        registerTest(event, environment, "map_location", BCCoreGameTests::mapLocation);
        registerTest(event, environment, "paintbrush", BCCoreGameTests::paintbrush);
        registerTest(event, environment, "list", BCCoreGameTests::list);
        registerTest(event, environment, "volume_box", BCCoreGameTests::volumeBox);
        registerTest(event, environment, "builders_filler_planner", BCCoreGameTests::buildersFillerPlanner);
        registerTest(event, environment, "fragile_fluid_shard", BCCoreGameTests::fragileFluidShard);
        registerTest(event, diagnosticsEnvironment, "core_goggles_power_tester", BCCoreGameTests::coreGogglesPowerTester);
        registerTest(event, environment, "spring", BCCoreGameTests::spring);
        registerTest(event, environment, "mj_foundation", BCCoreGameTests::mjFoundation);
        registerTest(event, environment, "mj_energy_conversion", BCCoreGameTests::mjEnergyConversion);
        registerTest(event, environment, "redstone_engine", BCCoreGameTests::redstoneEngine);
        registerTest(event, environment, "creative_engine", BCCoreGameTests::creativeEngine);
        registerTest(event, environment, "energy_fluids", BCCoreGameTests::energyFluids);
        registerTest(event, environment, "stirling_engine", BCCoreGameTests::stirlingEngine);
        registerTest(event, environment, "combustion_engine", BCCoreGameTests::combustionEngine);
        registerTest(event, environment, "rf_engine", BCCoreGameTests::rfEngine);
        registerTest(event, environment, "engine_menus", BCCoreGameTests::engineMenus);
        registerTest(event, environment, "combustion_containers", BCCoreGameTests::combustionContainers);
        registerTest(event, environment, "energy_engine_recipes", BCCoreGameTests::energyEngineRecipes);
        registerTest(event, environment, "energy_engine_loot", BCCoreGameTests::energyEngineLoot);
        registerTest(event, environment, "energy_refinery_fluids", BCCoreGameTests::energyRefineryFluids);
        registerTest(event, environment, "mj_dynamo", BCCoreGameTests::mjDynamo);
        registerTest(event, environment, "transport_pipe_foundation", BCCoreGameTests::transportPipeFoundation);
        registerTest(event, environment, "transport_filtered_buffer", BCCoreGameTests::transportFilteredBuffer);
        registerTest(event, environment, "transport_pipe_plugs", BCCoreGameTests::transportPipePlugs);
        registerTest(event, environment, "transport_rf_pipes", BCCoreGameTests::transportRfPipes);
registerTest(event, environment, "transport_fluid_pipe_foundation", BCCoreGameTests::transportFluidPipeFoundation);
registerTest(event, environment, "transport_power_pipe_foundation", BCCoreGameTests::transportPowerPipeFoundation);
registerTest(event, environment, "transport_wood_power_pipe", BCCoreGameTests::transportWoodPowerPipe);
registerTest(event, environment, "transport_general_power_pipes", BCCoreGameTests::transportGeneralPowerPipes);
registerTest(event, environment, "transport_diamond_power_pipes", BCCoreGameTests::transportDiamondPowerPipes);
registerTest(event, environment, "transport_branched_power_network", BCCoreGameTests::transportBranchedPowerNetwork);
registerTest(event, environment, "factory_tank", BCCoreGameTests::factoryTank);
        registerTest(event, environment, "factory_flood_gate", BCCoreGameTests::factoryFloodGate);
        registerTest(event, environment, "factory_pump", BCCoreGameTests::factoryPump);
        registerTest(event, environment, "factory_mining_well", BCCoreGameTests::factoryMiningWell);
        registerTest(event, environment, "factory_chute", BCCoreGameTests::factoryChute);
        registerTest(event, environment, "factory_distiller", BCCoreGameTests::factoryDistiller);
        registerTest(event, environment, "factory_heat_exchanger", BCCoreGameTests::factoryHeatExchanger);
        registerTest(event, environment, "factory_water_gel", BCCoreGameTests::factoryWaterGel);
        registerTest(event, environment, "factory_auto_workbench", BCCoreGameTests::factoryAutoWorkbench);
        registerTest(event, environment, "builders_filler", BCCoreGameTests::buildersFiller);
        registerTest(event, environment, "builders_snapshot_data", BCCoreGameTests::buildersSnapshotData);
        registerTest(event, environment, "builders_architect_table", BCCoreGameTests::buildersArchitectTable);
        registerTest(event, environment, "builders_builder", BCCoreGameTests::buildersBuilder);
        registerTest(event, environment, "builders_replacer", BCCoreGameTests::buildersReplacer);
        registerTest(event, environment, "builders_quarry", BCCoreGameTests::buildersQuarry);
        registerTest(event, environment, "builders_blueprint_library", BCCoreGameTests::buildersBlueprintLibrary);
        registerTest(event, environment, "builders_construction_marker", BCCoreGameTests::buildersConstructionMarker);
        registerTest(event, environment, "robotics_redstone_board", BCCoreGameTests::roboticsRedstoneBoard);
        registerTest(event, environment, "robotics_requester", BCCoreGameTests::roboticsRequester);
        registerTest(event, environment, "robotics_zone_data", BCCoreGameTests::roboticsZoneData);
registerTest(event, environment, "robotics_zone_planner", BCCoreGameTests::roboticsZonePlanner);
registerTest(event, environment, "robotics_robot_goggles", BCCoreGameTests::roboticsRobotGoggles);
registerTest(event, environment, "robotics_gate_statements", BCCoreGameTests::roboticsGateStatements);
registerTest(event, environment, "robotics_robot_station", BCCoreGameTests::roboticsRobotStation);
        registerTest(event, environment, "robotics_delivery_robot", BCCoreGameTests::roboticsDeliveryRobot);
        registerTest(event, environment, "robotics_carrier_robot", BCCoreGameTests::roboticsCarrierRobot);
        registerTest(event, fluidCarrierEnvironment, "robotics_fluid_carrier_robot",
            BCCoreGameTests::roboticsFluidCarrierRobot);
        registerTest(event, environment, "builders_filler_patterns", BCCoreGameTests::buildersFillerPatterns);
        registerTest(event, environment, "builders_filler_advanced_patterns", BCCoreGameTests::buildersFillerAdvancedPatterns);
        registerTest(event, environment, "builders_filler_pyramid_centres", BCCoreGameTests::buildersFillerPyramidCentres);
        registerTest(event, environment, "builders_filler_sphere_patterns", BCCoreGameTests::buildersFillerSpherePatterns);
        registerTest(event, environment, "builders_filler_2d_patterns", BCCoreGameTests::buildersFiller2dPatterns);
        registerTest(event, environment, "silicon_chipsets", BCCoreGameTests::siliconChipsets);
        registerTest(event, environment, "silicon_gate_items", BCCoreGameTests::siliconGateItems);
        registerTest(event, environment, "silicon_laser_assembly", BCCoreGameTests::siliconLaserAssembly);
        registerTest(event, environment, "silicon_advanced_crafting_table", BCCoreGameTests::siliconAdvancedCraftingTable);
        registerTest(event, environment, "silicon_integration_table", BCCoreGameTests::siliconIntegrationTable);
        registerTest(event, siliconPlaceholderEnvironment, "silicon_placeholder_tables", BCCoreGameTests::siliconPlaceholderTables);
        registerTest(event, gateParameterEnvironment, "silicon_gate_parameter_triggers", BCCoreGameTests::siliconGateParameterTriggers);
        registerTest(event, fillerGateEnvironment, "builders_filler_gate_patterns", BCCoreGameTests::buildersFillerGatePatterns);
        registerTest(event, roboticsGateListEnvironment, "robotics_gate_list_parameters", BCCoreGameTests::roboticsGateListParameters);
        registerTest(event, transportCreativeEnvironment, "transport_creative_pipe_entries", BCCoreGameTests::transportCreativePipeEntries);
        registerTest(event, environment, "transport_pipe_shell_colors", BCCoreGameTests::transportPipeShellColors);
        registerTest(event, siliconLensEnvironment, "silicon_lens_variants", BCCoreGameTests::siliconLensVariants);
        registerTest(event, siliconSensorTimerEnvironment, "silicon_sensor_timer_plugs",
                BCCoreGameTests::siliconSensorTimerPlugs);
        registerTest(event, environment, "silicon_pipe_attachments", BCCoreGameTests::siliconPipeAttachments);
        registerTest(event, environment, "silicon_pulsar_gate", BCCoreGameTests::siliconPulsarGate);
        registerTest(event, environment, "transport_wood_fluid_pipe", BCCoreGameTests::transportWoodFluidPipe);
        registerTest(event, environment, "transport_fast_isolated_fluid_pipes", BCCoreGameTests::transportFastIsolatedFluidPipes);
        registerTest(event, environment, "transport_iron_fluid_pipe", BCCoreGameTests::transportIronFluidPipe);
        registerTest(event, environment, "transport_clay_void_fluid_pipes", BCCoreGameTests::transportClayVoidFluidPipes);
        registerTest(event, environment, "transport_diamond_fluid_pipe", BCCoreGameTests::transportDiamondFluidPipe);
        registerTest(event, environment, "transport_diamond_wood_fluid_pipe", BCCoreGameTests::transportDiamondWoodFluidPipe);
        registerTest(event, environment, "transport_item_flow", BCCoreGameTests::transportItemFlow);
        registerTest(event, environment, "transport_partial_item_bounce", BCCoreGameTests::transportPartialItemBounce);
        registerTest(event, environment, "transport_pipe_visual_state", BCCoreGameTests::transportPipeVisualState);
        registerTest(event, environment, "transport_special_item_pipes", BCCoreGameTests::transportSpecialItemPipes);
        registerTest(event, environment, "transport_routing_item_pipes", BCCoreGameTests::transportRoutingItemPipes);
        registerTest(event, environment, "transport_terminal_item_pipes", BCCoreGameTests::transportTerminalItemPipes);
        registerTest(event, environment, "transport_colored_item_pipes", BCCoreGameTests::transportColoredItemPipes);
        registerTest(event, environment, "silicon_lens_routing", BCCoreGameTests::siliconLensRouting);
        registerTest(event, environment, "silicon_facade", BCCoreGameTests::siliconFacade);
        registerTest(event, environment, "silicon_inventory_triggers", BCCoreGameTests::siliconInventoryTriggers);
        registerTest(event, environment, "silicon_fluid_triggers", BCCoreGameTests::siliconFluidTriggers);
        registerTest(event, environment, "silicon_power_triggers", BCCoreGameTests::siliconPowerTriggers);
        registerTest(event, environment, "silicon_machine_triggers", BCCoreGameTests::siliconMachineTriggers);
        registerTest(event, environment, "silicon_engine_stage_triggers", BCCoreGameTests::siliconEngineStageTriggers);
        registerTest(event, environment, "silicon_fluids_traversing_trigger", BCCoreGameTests::siliconFluidsTraversingTrigger);
        registerTest(event, environment, "silicon_power_requested_trigger", BCCoreGameTests::siliconPowerRequestedTrigger);
        registerTest(event, environment, "silicon_pipe_direction_action", BCCoreGameTests::siliconPipeDirectionAction);
        registerTest(event, environment, "silicon_power_limit_actions", BCCoreGameTests::siliconPowerLimitActions);
        registerTest(event, environment, "silicon_extraction_preset_actions", BCCoreGameTests::siliconExtractionPresetActions);
        registerTest(event, environment, "silicon_pipe_color_actions", BCCoreGameTests::siliconPipeColorActions);
        registerTest(event, environment, "silicon_pipe_wire_signals", BCCoreGameTests::siliconPipeWireSignals);
        registerTest(event, environment, "transport_daizuli_item_pipe", BCCoreGameTests::transportDaizuliItemPipe);
        registerTest(event, environment, "transport_diamond_wood_item_pipe", BCCoreGameTests::transportDiamondWoodItemPipe);
        registerTest(event, environment, "transport_emzuli_item_pipe", BCCoreGameTests::transportEmzuliItemPipe);
        registerTest(event, environment, "transport_diamond_item_pipe", BCCoreGameTests::transportDiamondItemPipe);
        registerTest(event, environment, "transport_stripes_item_pipe", BCCoreGameTests::transportStripesItemPipe);
    }

    private static void registerTest(
        RegisterGameTestsEvent event,
        Holder<TestEnvironmentDefinition<?>> environment,
        String name,
        java.util.function.Consumer<GameTestHelper> test
    ) {
        Identifier id = id(name);
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
            environment, Identifier.withDefaultNamespace("empty"), 20, 0, true
        );
        event.registerTest(id, BuildCraftGameTestInstance.create(id, data, test));
    }

    private static void decorationStates(GameTestHelper helper) {
        for (EnumDecoratedBlock type : EnumDecoratedBlock.values()) {
            ItemStack stack = ItemBlockDecoration.createStack(type);
            BlockItemStateProperties properties = stack.getOrDefault(
                DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
            );
            helper.assertValueEqual(properties.get(BlockDecoration.DECORATION_TYPE), type, "stored decoration type");

            BlockState state = properties.apply(BCCoreBlocks.DECORATED.get().defaultBlockState());
            helper.assertValueEqual(state.getValue(BlockDecoration.DECORATION_TYPE), type, "applied decoration type");
            helper.assertValueEqual(
                state.getLightEmission(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)),
                type.lightLevel(),
                "decoration light level"
            );
        }
        helper.succeed();
    }

    private static void wrenchRotation(GameTestHelper helper) {
        BlockPos relativePos = BlockPos.ZERO;
        BlockPos absolutePos = helper.absolutePos(relativePos);
        helper.getLevel().setBlock(
            absolutePos,
            Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH),
            net.minecraft.world.level.block.Block.UPDATE_ALL
        );

        ItemStack wrench = new ItemStack(BCCoreItems.WRENCH.get());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos), net.minecraft.core.Direction.UP, absolutePos, false);
        UseOnContext context = new UseOnContext(helper.getLevel(), null, InteractionHand.MAIN_HAND, wrench, hit);
        InteractionResult result = wrench.useOn(context);

        helper.assertTrue(result.consumesAction(), "Wrench did not consume a valid rotation action");
        helper.assertBlockProperty(relativePos, BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.EAST);
        helper.succeed();
    }

    private static void pathGraph(GameTestHelper helper) {
        PathSavedData paths = new PathSavedData();
        BlockPos a = new BlockPos(0, 1, 0);
        BlockPos b = new BlockPos(2, 1, 0);
        BlockPos c = new BlockPos(4, 1, 0);
        BlockPos d = new BlockPos(6, 1, 0);
        BlockPos e = new BlockPos(8, 1, 0);
        for (BlockPos marker : java.util.List.of(a, b, c, d, e)) paths.addMarker(marker);

        helper.assertTrue(paths.connect(a, b), "Could not create a path");
        helper.assertTrue(paths.connect(b, c), "Could not extend a path");
        helper.assertTrue(paths.connect(d, e), "Could not create the second path");
        helper.assertTrue(paths.connect(c, d), "Could not merge paths");
        PathConnection path = paths.connectionAt(a).orElseThrow();
        helper.assertValueEqual(path.positions(), java.util.List.of(a, b, c, d, e), "merged path order");

        helper.assertTrue(paths.reverse(c), "Could not reverse path");
        helper.assertValueEqual(path.positions(), java.util.List.of(e, d, c, b, a), "reversed path order");
        helper.assertTrue(paths.connect(a, e), "Could not close path loop");
        helper.assertTrue(path.loop(), "Path did not become a loop");

        paths.removeMarker(c);
        PathConnection opened = paths.connectionAt(a).orElseThrow();
        helper.assertTrue(!opened.loop(), "Removing a loop marker did not open the loop");
        helper.assertValueEqual(opened.positions(), java.util.List.of(b, a, e, d), "opened loop order");

        Object encoded = PathSavedData.CODEC.encodeStart(JsonOps.INSTANCE, paths).getOrThrow();
        PathSavedData decoded = PathSavedData.CODEC.parse(JsonOps.INSTANCE, (com.google.gson.JsonElement) encoded).getOrThrow();
        helper.assertValueEqual(decoded.markers(), paths.markers(), "persisted marker positions");
        helper.assertValueEqual(decoded.connections().getFirst().positions(), opened.positions(), "persisted path order");

        PathSavedData aimedPaths = new PathSavedData();
        BlockPos left = new BlockPos(-1, 0, 2);
        BlockPos right = new BlockPos(1, 0, 2);
        aimedPaths.addMarker(left);
        aimedPaths.addMarker(right);
        ItemMarkerConnector.Candidate aimed = ItemMarkerConnector.findCandidate(
            aimedPaths, new Vec3(0.5, 0.5, 0.0), new Vec3(0.0, 0.0, 1.0)
        );
        helper.assertTrue(aimed != null, "Connector did not select the aimed marker line");
        helper.assertTrue(
            (aimed.from().equals(left) && aimed.to().equals(right))
                || (aimed.from().equals(right) && aimed.to().equals(left)),
            "Connector selected the wrong marker line"
        );

        PathSavedData directional = new PathSavedData();
        BlockPos p0 = new BlockPos(0, 0, 0);
        BlockPos p1 = new BlockPos(1, 0, 0);
        BlockPos q0 = new BlockPos(0, 0, 2);
        BlockPos q1 = new BlockPos(1, 0, 2);
        for (BlockPos marker : java.util.List.of(p0, p1, q0, q1)) directional.addMarker(marker);
        helper.assertTrue(directional.connect(p0, p1), "Could not create directional path A");
        helper.assertTrue(directional.connect(q0, q1), "Could not create directional path B");
        helper.assertTrue(
            !directional.canConnect(p0, q0) && !directional.canConnect(q0, p0),
            "Connector allowed a direction-reversing first-to-first merge"
        );
        helper.succeed();
    }

    private static void pathMarkerSync(GameTestHelper helper) {
        BlockPos first = new BlockPos(0, 1, 0);
        BlockPos middle = new BlockPos(2, 1, 0);
        BlockPos last = new BlockPos(4, 1, 0);
        for (BlockPos marker : java.util.List.of(first, middle, last)) {
            helper.setBlock(marker.below(), Blocks.STONE);
            helper.setBlock(marker, BCCoreBlocks.MARKER_PATH.get().defaultBlockState());
        }

        PathSavedData paths = PathSavedData.get(helper.getLevel());
        BlockPos absoluteFirst = helper.absolutePos(first);
        BlockPos absoluteMiddle = helper.absolutePos(middle);
        BlockPos absoluteLast = helper.absolutePos(last);
        helper.assertTrue(paths.connect(absoluteFirst, absoluteMiddle), "Could not connect placed markers");
        helper.assertTrue(paths.connect(absoluteMiddle, absoluteLast), "Could not extend placed markers");

        PathMarkerBlockEntity firstEntity = (PathMarkerBlockEntity) helper.getLevel().getBlockEntity(absoluteFirst);
        helper.assertValueEqual(
            firstEntity.path(), java.util.List.of(absoluteFirst, absoluteMiddle, absoluteLast), "synced block entity path"
        );
        helper.assertTrue(!firstEntity.loop(), "Open path synced as a loop");

        helper.destroyBlock(middle);
        helper.assertTrue(paths.connectionAt(absoluteFirst).isEmpty(), "Destroyed middle marker did not split short path");
        helper.assertValueEqual(firstEntity.path(), java.util.List.of(), "surviving marker snapshot was not cleared");
        helper.succeed();
    }

    private static void volumeGraph(GameTestHelper helper) {
        VolumeSavedData volumes = new VolumeSavedData();
        BlockPos origin = new BlockPos(0, 0, 0);
        BlockPos x = new BlockPos(4, 0, 0);
        BlockPos xy = new BlockPos(4, 3, 0);
        BlockPos xyz = new BlockPos(4, 3, 2);
        BlockPos opposite = new BlockPos(0, 3, 2);
        for (BlockPos marker : java.util.List.of(origin, x, xy, xyz, opposite)) volumes.addMarker(marker);
        helper.assertTrue(volumes.connect(origin, x), "Could not create X volume edge");
        helper.assertTrue(volumes.connect(x, xy), "Could not add Y volume edge");
        helper.assertTrue(volumes.connect(xy, xyz), "Could not add Z volume edge");
        helper.assertTrue(volumes.connect(origin, opposite), "Could not add an existing-box corner");
        VolumeConnection box = volumes.connectionAt(origin).orElseThrow();
        helper.assertValueEqual(box.min(), origin, "volume minimum");
        helper.assertValueEqual(box.max(), xyz, "volume maximum");
        helper.assertValueEqual(
            box.connectedAxes(), java.util.EnumSet.allOf(net.minecraft.core.Direction.Axis.class), "volume axes"
        );

        Object encoded = VolumeSavedData.CODEC.encodeStart(JsonOps.INSTANCE, volumes).getOrThrow();
        VolumeSavedData decoded = VolumeSavedData.CODEC.parse(
            JsonOps.INSTANCE, (com.google.gson.JsonElement) encoded
        ).getOrThrow();
        helper.assertValueEqual(decoded.connectionAt(origin).orElseThrow().max(), xyz, "persisted volume maximum");

        VolumeSavedData blocked = new VolumeSavedData();
        BlockPos near = new BlockPos(2, 0, 0);
        BlockPos far = new BlockPos(4, 0, 0);
        for (BlockPos marker : java.util.List.of(origin, near, far)) blocked.addMarker(marker);
        helper.assertValueEqual(blocked.validConnections(origin), java.util.List.of(near), "nearest axial marker");
        helper.assertTrue(!blocked.canConnect(origin, far), "Connection skipped an intervening marker");
        ItemMarkerConnector.Candidate volumeAim = ItemMarkerConnector.findVolumeCandidate(
            blocked, new Vec3(1.5, 0.5, -2.0), new Vec3(0.0, 0.0, 1.0)
        );
        helper.assertTrue(volumeAim != null, "Connector did not select an aimed volume line");
        helper.assertTrue(
            (volumeAim.from().equals(origin) && volumeAim.to().equals(near))
                || (volumeAim.from().equals(near) && volumeAim.to().equals(origin)),
            "Connector selected the wrong volume line"
        );

        VolumeSavedData merging = new VolumeSavedData();
        BlockPos a0 = new BlockPos(0, 0, 0), a1 = new BlockPos(2, 0, 0);
        BlockPos b0 = new BlockPos(0, 0, 2), b1 = new BlockPos(0, 2, 2);
        for (BlockPos marker : java.util.List.of(a0, a1, b0, b1)) merging.addMarker(marker);
        helper.assertTrue(merging.connect(a0, a1), "Could not create merge X edge");
        helper.assertTrue(merging.connect(b0, b1), "Could not create merge Y edge");
        helper.assertTrue(merging.connect(a0, b0), "Could not merge edges across Z");
        helper.assertValueEqual(merging.connections().size(), 1, "merged volume count");
        merging.removeMarker(a1);
        helper.assertTrue(merging.connectionAt(a1).isEmpty(), "Removed volume marker remained connected");
        helper.succeed();
    }

    private static void volumeMarkerSync(GameTestHelper helper) {
        BlockPos first = new BlockPos(0, 1, 0);
        BlockPos second = new BlockPos(3, 1, 0);
        for (BlockPos marker : java.util.List.of(first, second)) {
            helper.setBlock(marker.below(), Blocks.STONE);
            helper.setBlock(marker, BCCoreBlocks.MARKER_VOLUME.get().defaultBlockState());
        }
        BlockPos absoluteFirst = helper.absolutePos(first);
        BlockPos absoluteSecond = helper.absolutePos(second);
        VolumeSavedData volumes = VolumeSavedData.get(helper.getLevel());
        helper.assertTrue(volumes.connectValid(absoluteFirst), "Manual volume connection attempt failed");
        VolumeMarkerBlockEntity firstEntity = (VolumeMarkerBlockEntity) helper.getLevel().getBlockEntity(absoluteFirst);
        helper.assertValueEqual(firstEntity.min(), absoluteFirst, "synced volume minimum");
        helper.assertValueEqual(firstEntity.max(), absoluteSecond, "synced volume maximum");
        helper.assertValueEqual(
            firstEntity.axes(), java.util.EnumSet.of(net.minecraft.core.Direction.Axis.X), "synced volume axes"
        );
        helper.assertTrue(firstEntity.renderOwner(), "First marker did not own volume rendering");
        helper.assertTrue(firstEntity.isValidFromLocation(absoluteFirst.west()), "Adjacent corner was not a valid area-provider location");
        helper.assertTrue(!firstEntity.isValidFromLocation(absoluteFirst), "Inside position was a valid area-provider location");

        helper.setBlock(first.west(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(firstEntity.showingSignals(), "Powered marker did not enable signal guides");
        helper.setBlock(first.west(), Blocks.AIR);
        helper.assertTrue(!firstEntity.showingSignals(), "Unpowered marker did not disable signal guides");

        helper.destroyBlock(second);
        helper.assertTrue(volumes.connectionAt(absoluteFirst).isEmpty(), "Destroyed marker left a short volume connection");
        helper.assertValueEqual(firstEntity.min(), absoluteFirst, "surviving marker minimum was not cleared");
        helper.assertValueEqual(firstEntity.max(), absoluteFirst, "surviving marker maximum was not cleared");
        helper.succeed();
    }

    private static void mapLocation(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        ItemMapLocation item = BCCoreItems.MAP_LOCATION.get();

        // Use a vertical edge so independently allocated tests cannot present a nearer
        // collinear marker in the shared GameTest dimension.
        BlockPos areaFirst = new BlockPos(1, 10, 1), areaSecond = new BlockPos(1, 13, 1);
        for (BlockPos marker : java.util.List.of(areaFirst, areaSecond)) {
            helper.setBlock(marker.below(), Blocks.STONE);
            helper.setBlock(marker, BCCoreBlocks.MARKER_VOLUME.get().defaultBlockState());
        }
        VolumeSavedData.get(helper.getLevel()).connectValid(helper.absolutePos(areaFirst));
        ItemStack areaStack = new ItemStack(item);
        InteractionResult areaResult = item.useOn(useContext(helper, player, areaStack, areaFirst));
        helper.assertTrue(areaResult.consumesAction(), "Map did not record area provider");
        helper.assertValueEqual(item.getType(areaStack), MapLocationType.AREA, "map area type");
        helper.assertValueEqual(areaStack.getMaxStackSize(), 1, "recorded map stack limit");
        BlockPos absoluteAreaFirst = helper.absolutePos(areaFirst), absoluteAreaSecond = helper.absolutePos(areaSecond);
        BlockPos expectedAreaMin = new BlockPos(
            Math.min(absoluteAreaFirst.getX(), absoluteAreaSecond.getX()),
            Math.min(absoluteAreaFirst.getY(), absoluteAreaSecond.getY()),
            Math.min(absoluteAreaFirst.getZ(), absoluteAreaSecond.getZ())
        );
        BlockPos expectedAreaMax = new BlockPos(
            Math.max(absoluteAreaFirst.getX(), absoluteAreaSecond.getX()),
            Math.max(absoluteAreaFirst.getY(), absoluteAreaSecond.getY()),
            Math.max(absoluteAreaFirst.getZ(), absoluteAreaSecond.getZ())
        );
        helper.assertValueEqual(item.getAreaMin(areaStack).orElseThrow(), expectedAreaMin, "map area minimum");
        helper.assertValueEqual(item.getAreaMax(areaStack).orElseThrow(), expectedAreaMax, "map area maximum");

        BlockPos pathFirst = new BlockPos(0, 1, 3), pathSecond = new BlockPos(2, 1, 3);
        for (BlockPos marker : java.util.List.of(pathFirst, pathSecond)) {
            helper.setBlock(marker.below(), Blocks.STONE);
            helper.setBlock(marker, BCCoreBlocks.MARKER_PATH.get().defaultBlockState());
        }
        PathSavedData paths = PathSavedData.get(helper.getLevel());
        paths.connect(helper.absolutePos(pathFirst), helper.absolutePos(pathSecond));
        ItemStack pathStack = new ItemStack(item);
        helper.assertTrue(item.useOn(useContext(helper, player, pathStack, pathFirst)).consumesAction(), "Map did not record path");
        helper.assertValueEqual(item.getType(pathStack), MapLocationType.PATH, "map path type");
        helper.assertValueEqual(
            item.getPath(pathStack), java.util.List.of(helper.absolutePos(pathFirst), helper.absolutePos(pathSecond)), "map path"
        );

        BlockPos spot = new BlockPos(5, 1, 0);
        helper.setBlock(spot, Blocks.STONE);
        ItemStack spotStack = new ItemStack(item);
        helper.assertTrue(item.useOn(useContext(helper, player, spotStack, spot)).consumesAction(), "Map did not record spot");
        helper.assertValueEqual(item.getType(spotStack), MapLocationType.SPOT, "map spot type");
        helper.assertValueEqual(item.getPoint(spotStack).orElseThrow(), helper.absolutePos(spot), "map spot position");
        helper.assertValueEqual(item.getPointSide(spotStack).orElseThrow(), net.minecraft.core.Direction.UP, "map spot side");

        item.setStoredName(spotStack, "Quarry Site");
        Object encoded = MapLocationData.CODEC.encodeStart(JsonOps.INSTANCE, ItemMapLocation.getData(spotStack)).getOrThrow();
        MapLocationData decoded = MapLocationData.CODEC.parse(
            JsonOps.INSTANCE, (com.google.gson.JsonElement) encoded
        ).getOrThrow();
        helper.assertValueEqual(decoded.name(), "Quarry Site", "persisted map name");
        ItemMapLocation.clear(spotStack);
        helper.assertValueEqual(item.getType(spotStack), MapLocationType.CLEAN, "cleared map type");
        helper.assertValueEqual(spotStack.getMaxStackSize(), 16, "cleared map stack limit");
        helper.succeed();
    }

    private static void paintbrush(GameTestHelper helper) {
        ItemPaintbrush item = BCCoreItems.PAINTBRUSH.get();
        net.minecraft.world.entity.player.Player survival = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.player.Player creative = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        // GameTest's mock player does not apply the requested game mode's abilities.
        creative.getAbilities().instabuild = true;

        BlockPos glass = new BlockPos(0, 1, 0);
        helper.setBlock(glass, Blocks.GLASS);
        ItemStack red = ItemPaintbrush.colored(item, DyeColor.RED);
        helper.assertTrue(item.useOn(useContext(helper, survival, red, glass)).consumesAction(), "Brush did not paint glass");
        helper.assertBlockPresent(Blocks.RED_STAINED_GLASS, glass);
        helper.assertValueEqual(ItemPaintbrush.data(red).usesLeft(), 63, "paint use count");

        InteractionResult unchanged = item.useOn(useContext(helper, survival, red, glass));
        helper.assertTrue(!unchanged.consumesAction(), "Same-color paint consumed an action");
        helper.assertValueEqual(ItemPaintbrush.data(red).usesLeft(), 63, "same-color use count");

        BlockPos pane = new BlockPos(1, 1, 0);
        helper.setBlock(pane, Blocks.GLASS_PANE);
        ItemStack blue = ItemPaintbrush.colored(item, DyeColor.BLUE);
        helper.assertTrue(item.useOn(useContext(helper, creative, blue, pane)).consumesAction(), "Brush did not paint pane");
        helper.assertBlockPresent(Blocks.BLUE_STAINED_GLASS_PANE, pane);
        helper.assertValueEqual(ItemPaintbrush.data(blue).usesLeft(), 64, "creative paint use count");

        BlockPos terracotta = new BlockPos(2, 1, 0);
        helper.setBlock(terracotta, Blocks.TERRACOTTA);
        ItemStack lastUse = ItemPaintbrush.colored(item, DyeColor.LIME);
        ItemPaintbrush.load(lastUse, DyeColor.LIME, 1);
        helper.assertTrue(item.useOn(useContext(helper, survival, lastUse, terracotta)).consumesAction(),
            "Brush did not paint terracotta");
        helper.assertBlockPresent(Blocks.LIME_TERRACOTTA, terracotta);
        helper.assertTrue(ItemPaintbrush.data(lastUse) == null, "Exhausted brush retained use data");
        helper.assertTrue(lastUse.get(BCCoreDataComponents.PAINTBRUSH_COLOR.get()) == null,
            "Exhausted brush retained model color");

        ItemStack clean = new ItemStack(item);
        helper.assertTrue(item.useOn(useContext(helper, survival, clean, glass)).consumesAction(),
            "Clean brush did not remove glass color");
        helper.assertBlockPresent(Blocks.GLASS, glass);

        PaintbrushData expected = new PaintbrushData(DyeColor.MAGENTA, 37);
        Object encoded = PaintbrushData.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
        PaintbrushData decoded = PaintbrushData.CODEC.parse(
            JsonOps.INSTANCE, (com.google.gson.JsonElement) encoded
        ).getOrThrow();
        helper.assertValueEqual(decoded, expected, "persisted paintbrush data");
        helper.succeed();
    }

    private static void list(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack listStack = new ItemStack(BCCoreItems.LIST.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, listStack);
        ListMenu menu = new ListMenu(1, player.getInventory(), InteractionHand.MAIN_HAND);

        menu.setCarried(new ItemStack(Items.IRON_INGOT, 32));
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertValueEqual(ItemList.data(listStack).lines().getFirst().stacks().getFirst().getCount(), 1,
            "phantom slot stack count");
        helper.assertValueEqual(menu.getCarried().getCount(), 32, "phantom slot changed carried stack");

        helper.assertTrue(menu.clickMenuButton(player, 0), "Could not toggle precise matching");
        helper.assertTrue(menu.precise(0), "Precise matching did not enable");
        helper.assertTrue(menu.clickMenuButton(player, 1), "Could not toggle type matching");
        helper.assertValueEqual(menu.mode(0), ListMatchMode.TYPE, "type matching mode");
        helper.assertTrue(ItemList.data(listStack).matches(new ItemStack(Items.GOLD_INGOT)),
            "Type mode did not match another ingot");

        helper.assertTrue(menu.clickMenuButton(player, 1), "Could not disable type matching");
        helper.assertTrue(menu.clickMenuButton(player, 2), "Could not toggle material matching");
        helper.assertValueEqual(menu.mode(0), ListMatchMode.MATERIAL, "material matching mode");
        helper.assertTrue(ItemList.data(listStack).matches(new ItemStack(Items.IRON_NUGGET)),
            "Material mode did not match another iron form");

        menu.setLabel("Quarry supplies");
        helper.assertValueEqual(ItemList.data(listStack).label(), "Quarry supplies", "list label");
        helper.assertValueEqual(listStack.get(BCCoreDataComponents.LIST_USED.get()), true, "used-list model state");

        ItemStack namedIron = new ItemStack(Items.IRON_INGOT);
        namedIron.set(DataComponents.CUSTOM_NAME, Component.literal("Special"));
        ListLineData direct = new ListLineData(java.util.List.of(new ItemStack(Items.IRON_INGOT)), false,
            ListMatchMode.DIRECT);
        ListLineData precise = new ListLineData(java.util.List.of(new ItemStack(Items.IRON_INGOT)), true,
            ListMatchMode.DIRECT);
        helper.assertTrue(direct.matches(namedIron), "Non-precise direct mode rejected component difference");
        helper.assertTrue(!precise.matches(namedIron), "Precise direct mode ignored component difference");

        ListData expected = new ListData("Codec", java.util.List.of(direct, ListLineData.empty()));
        net.minecraft.resources.RegistryOps<com.google.gson.JsonElement> ops = net.minecraft.resources.RegistryOps.create(
            JsonOps.INSTANCE, helper.getLevel().registryAccess()
        );
        Object encoded = ListData.CODEC.encodeStart(ops, expected).getOrThrow();
        ListData decoded = ListData.CODEC.parse(ops, (com.google.gson.JsonElement) encoded).getOrThrow();
        helper.assertValueEqual(decoded.label(), expected.label(), "persisted list label");
        helper.assertTrue(decoded.lines().getFirst().matches(namedIron), "persisted list entries");
        helper.succeed();
    }

    private static void volumeBox(GameTestHelper helper) {
        BlockPos relative = new BlockPos(4, 8, 4);
        BlockPos absolute = helper.absolutePos(relative);
        VolumeBoxSavedData boxes = VolumeBoxSavedData.get(helper.getLevel());
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        helper.setBlock(relative.below(), Blocks.STONE);
        ItemStack volumeBoxItem = new ItemStack(BCCoreItems.VOLUME_BOX.get());
        helper.assertTrue(BCCoreItems.VOLUME_BOX.get().useOn(
            useContext(helper, player, volumeBoxItem, relative.below())
        ).consumesAction(), "Could not place volume box item");
        helper.assertTrue(!BCCoreItems.VOLUME_BOX.get().useOn(
            useContext(helper, player, volumeBoxItem, relative.below())
        ).consumesAction(), "Overlapping volume box was placed");
        helper.assertValueEqual(boxes.boxAt(absolute).orElseThrow().min(), absolute, "initial volume minimum");

        player.setPos(absolute.getX() + 0.5, absolute.getY() + 0.5 - player.getEyeHeight(), absolute.getZ() - 3.0);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
        ItemMarkerConnector connector = BCCoreItems.MARKER_CONNECTOR.get();
        helper.assertTrue(connector.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(),
            "Connector did not begin volume-box editing");
        VolumeBox editing = boxes.boxAt(absolute).orElseThrow();
        helper.assertTrue(editing.isEditingBy(player.getUUID()), "Volume box did not record its editor");

        VolumeBox expanded = editing.update(
            new Vec3(absolute.getX() + 0.5, absolute.getY() + 0.5, absolute.getZ() - 3.0),
            new Vec3(1.0, 0.0, 1.0).normalize()
        );
        helper.assertTrue(!expanded.min().equals(expanded.max()), "Corner edit did not resize volume box");
        helper.assertValueEqual(expanded.cancelEdit().min(), absolute, "cancelled volume minimum");
        helper.assertValueEqual(expanded.cancelEdit().max(), absolute, "cancelled volume maximum");
        helper.assertTrue(expanded.confirmEdit().edit().isEmpty(), "confirmed volume remained in editing state");

        player.setShiftKeyDown(true);
        helper.assertTrue(connector.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(),
            "Connector did not cancel volume-box editing");
        helper.assertTrue(boxes.boxAt(absolute).orElseThrow().edit().isEmpty(), "Cancelled saved volume remained editing");
        helper.assertTrue(connector.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(),
            "Connector did not remove volume box");
        helper.assertTrue(boxes.boxAt(absolute).isEmpty(), "Removed volume box remained saved");

        VolumeBox codecBox = VolumeBox.at(new BlockPos(1, 2, 3)).beginEdit(
            player.getUUID(), new BlockPos(1, 2, 3), 2.0
        );
        Object encoded = VolumeBox.CODEC.encodeStart(JsonOps.INSTANCE, codecBox).getOrThrow();
        VolumeBox decoded = VolumeBox.CODEC.parse(JsonOps.INSTANCE, (com.google.gson.JsonElement) encoded).getOrThrow();
        helper.assertValueEqual(decoded.id(), codecBox.id(), "persisted volume-box id");
        helper.assertValueEqual(decoded.edit(), codecBox.edit(), "persisted volume-box edit state");
        helper.succeed();
    }

    private static void buildersFillerPlanner(GameTestHelper helper) {
        BlockPos absolute = helper.absolutePos(new BlockPos(2, 8, 2));
        VolumeBoxSavedData boxes = VolumeBoxSavedData.get(helper.getLevel());
        helper.assertTrue(boxes.add(absolute), "Could not create planner volume box");
        VolumeBox box = boxes.boxAt(absolute).orElseThrow();
        var planners = buildcraft.builders.planner.FillerPlannerSavedData.get(helper.getLevel());
        helper.assertTrue(planners.attach(box.id()), "Could not attach Filler Planner");
        helper.assertTrue(!planners.attach(box.id()), "Duplicate Filler Planner attachment was accepted");
        var data = buildcraft.builders.planner.FillerPlannerData.defaults()
                .pattern(buildcraft.builders.FillerPattern.FRAME).inverted(true).hollow(true);
        planners.set(box.id(), data);
        helper.assertValueEqual(planners.get(box.id()).orElseThrow(), data, "Planner settings did not persist in memory");
        helper.assertTrue(buildcraft.builders.planner.FillerPlannerShape.includes(
                data.inverted(false), absolute, absolute, absolute.offset(2, 2, 2)), "Frame omitted its corner");
        helper.assertTrue(!buildcraft.builders.planner.FillerPlannerShape.includes(
                data.inverted(false), absolute.offset(1, 1, 1), absolute, absolute.offset(2, 2, 2)), "Frame included its interior");
        Object encoded = buildcraft.builders.planner.FillerPlannerData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        var decoded = buildcraft.builders.planner.FillerPlannerData.CODEC.parse(
                JsonOps.INSTANCE, (com.google.gson.JsonElement) encoded).getOrThrow();
        helper.assertValueEqual(decoded, data, "Filler Planner codec round-trip");
        helper.assertTrue(planners.remove(box.id()) && planners.get(box.id()).isEmpty(), "Planner did not detach");
        helper.succeed();
    }

    private static void coreGogglesPowerTester(GameTestHelper helper) {
        ItemStack goggles = new ItemStack(BCCoreItems.GOGGLES.get());
        helper.assertValueEqual(1, goggles.getMaxStackSize(), "Goggles stack size");
        helper.assertFalse(goggles.isDamageableItem(), "Goggles unexpectedly have durability");
        var equippable = goggles.get(DataComponents.EQUIPPABLE);
        helper.assertTrue(equippable != null, "Goggles are not equippable");
        helper.assertValueEqual(net.minecraft.world.entity.EquipmentSlot.HEAD, equippable.slot(), "Goggles equipment slot");
        helper.assertTrue(goggles.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().isEmpty(),
                "Goggles unexpectedly grant armour attributes");

        BlockPos relative = new BlockPos(2, 8, 2);
        helper.setBlock(relative, BCCoreBlocks.POWER_TESTER.get());
        BlockPos pos = helper.absolutePos(relative);
        var tester = (buildcraft.core.block.entity.PowerTesterBlockEntity) helper.getLevel().getBlockEntity(pos);
        var north = helper.getLevel().getCapability(MjAPI.CAP_RECEIVER, pos, Direction.NORTH);
        var down = helper.getLevel().getCapability(MjAPI.CAP_RECEIVER, pos, Direction.DOWN);
        helper.assertTrue(north == tester && down == tester, "Power Tester did not expose receiver on every side");
        long offered = buildcraft.core.block.entity.PowerTesterBlockEntity.MAX_RECEIVE + 7 * MjAPI.MJ;
        helper.assertValueEqual(7 * MjAPI.MJ, tester.receivePower(offered, true), "Power Tester simulated excess");
        helper.assertValueEqual(0L, tester.totalReceived(), "Power Tester simulation mutated totals");
        helper.assertValueEqual(7 * MjAPI.MJ, tester.receivePower(offered, false), "Power Tester committed excess");
        helper.assertValueEqual(buildcraft.core.block.entity.PowerTesterBlockEntity.MAX_RECEIVE,
                tester.pendingReceived(), "Power Tester pending tick measurement");
        buildcraft.core.block.entity.PowerTesterBlockEntity.tick(helper.getLevel(), pos, tester.getBlockState(), tester);
        helper.assertValueEqual(buildcraft.core.block.entity.PowerTesterBlockEntity.MAX_RECEIVE,
                tester.tickReceived(), "Power Tester current tick measurement");
        buildcraft.core.block.entity.PowerTesterBlockEntity.tick(helper.getLevel(), pos, tester.getBlockState(), tester);
        helper.assertValueEqual(buildcraft.core.block.entity.PowerTesterBlockEntity.MAX_RECEIVE,
                tester.lastReceived(), "Power Tester last tick measurement");
        var saved = tester.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertValueEqual(buildcraft.core.block.entity.PowerTesterBlockEntity.MAX_RECEIVE,
                saved.getLongOr("total", -1), "Power Tester persisted total");
        var drops = Block.getDrops(tester.getBlockState(), helper.getLevel(), pos, tester);
        helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(BCCoreItems.POWER_TESTER.get())),
                "Power Tester loot missing block item");
        helper.succeed();
    }

    private static void fragileFluidShard(GameTestHelper helper) {
        net.neoforged.neoforge.fluids.FluidStack water = new net.neoforged.neoforge.fluids.FluidStack(
            net.minecraft.world.level.material.Fluids.WATER, 1_250
        );
        java.util.ArrayList<ItemStack> drops = new java.util.ArrayList<>();
        FluidItemDrops.addFluidDrops(drops, water);
        helper.assertValueEqual(drops.size(), 3, "fluid shard drop count");
        helper.assertValueEqual(ItemFragileFluidContainer.getFluid(drops.get(0)).getAmount(), 500,
            "first fluid shard amount");
        helper.assertValueEqual(ItemFragileFluidContainer.getFluid(drops.get(1)).getAmount(), 500,
            "second fluid shard amount");
        helper.assertValueEqual(ItemFragileFluidContainer.getFluid(drops.get(2)).getAmount(), 250,
            "remainder fluid shard amount");

        ItemStack shard = drops.getFirst();
        net.neoforged.neoforge.transfer.access.ItemAccess access =
            net.neoforged.neoforge.transfer.access.ItemAccess.forStack(shard);
        net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> handler =
            access.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.ITEM);
        helper.assertTrue(handler != null, "Fluid shard did not expose its item fluid capability");
        net.neoforged.neoforge.transfer.fluid.FluidResource waterResource =
            net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
        helper.assertValueEqual(handler.getAmountAsInt(0), 500, "fluid handler initial amount");

        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(handler.extract(0, waterResource, 200, transaction), 200,
                "simulated fluid extraction");
        }
        helper.assertValueEqual(handler.getAmountAsInt(0), 500, "aborted extraction amount");

        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(handler.extract(0, waterResource, 200, transaction), 200,
                "committed fluid extraction");
            transaction.commit();
        }
        helper.assertValueEqual(handler.getAmountAsInt(0), 300, "remaining fluid amount");
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(0, waterResource, 100, transaction), 0,
                "extraction-only shard accepted fluid");
            helper.assertValueEqual(handler.extract(0, waterResource, 300, transaction), 300,
                "final fluid extraction");
            transaction.commit();
        }
        helper.assertTrue(shard.isEmpty(), "Drained fragile shard was not consumed");
        helper.succeed();
    }

    private static void spring(GameTestHelper helper) {
        BlockPos waterPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockState waterSpring = BCCoreBlocks.SPRING.get().defaultBlockState()
            .setValue(BlockSpring.SPRING_TYPE, EnumSpring.WATER);
        helper.getLevel().setBlock(waterPos, waterSpring, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().removeBlock(waterPos.above(), false);
        helper.assertTrue(BCCoreBlocks.SPRING.get().tryGenerate(
            helper.getLevel(), waterPos, waterSpring, net.minecraft.util.RandomSource.create(1L)
        ), "Water spring did not generate fluid");
        helper.assertTrue(helper.getLevel().getBlockState(waterPos.above()).is(Blocks.WATER),
            "Water spring generated the wrong block");

        BlockPos oilPos = helper.absolutePos(new BlockPos(2, 1, 0));
        BlockState oilSpring = waterSpring.setValue(BlockSpring.SPRING_TYPE, EnumSpring.OIL);
        helper.getLevel().setBlock(oilPos, oilSpring, net.minecraft.world.level.block.Block.UPDATE_ALL);
        net.minecraft.util.RandomSource oilRandom = net.minecraft.util.RandomSource.create(1L);
        boolean generatedOil = false;
        for (int attempt = 0; attempt < 32 && !generatedOil; attempt++) {
            helper.getLevel().removeBlock(oilPos.above(), false);
            generatedOil = BCCoreBlocks.SPRING.get().tryGenerate(
                helper.getLevel(), oilPos, oilSpring, oilRandom
            );
        }
        helper.assertTrue(generatedOil, "Configured oil spring did not pass its one-in-eight chance");
        helper.assertTrue(helper.getLevel().getBlockState(oilPos.above()).is(BCEnergyFluids.OIL_BLOCK.get()),
            "Oil spring generated the wrong block");

        for (EnumSpring type : EnumSpring.VALUES) {
            ItemStack stack = ItemBlockSpring.createStack(type);
            EnumSpring stored = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                .get(BlockSpring.SPRING_TYPE);
            helper.assertValueEqual(stored, type, "stored spring type");
            helper.assertValueEqual(stack.getHoverName(),
                Component.translatable("block.buildcraftcore.spring." + type.getSerializedName()),
                "spring variant name");
        }
        helper.assertValueEqual(waterSpring.getDestroySpeed(helper.getLevel(), waterPos), -1.0F,
            "spring destroy speed");
        helper.succeed();
    }

    private static void energyFluids(GameTestHelper helper) {
        helper.assertValueEqual("buildcraftenergy:glob_of_oil",
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                        buildcraft.energy.BCEnergyItems.GLOB_OF_OIL.get()).toString(),
                "Glob of Oil compatibility identifier");
        helper.assertValueEqual(BCEnergyFluids.OIL.get().getFluidType().getDensity(), 900,
            "oil density");
        helper.assertValueEqual(BCEnergyFluids.OIL.get().getFluidType().getViscosity(), 2_000,
            "oil viscosity");
        helper.assertValueEqual(BCEnergyFluids.FUEL_LIGHT.get().getFluidType().getDensity(), 400,
            "light fuel density");
        helper.assertValueEqual(BCEnergyFluids.FUEL_LIGHT.get().getFluidType().getViscosity(), 600,
            "light fuel viscosity");
        helper.assertTrue(BCEnergyFluids.OIL_BLOCK.get().defaultBlockState().getFluidState().isSource(),
            "oil block is not a source");
        helper.assertTrue(BCEnergyFluids.FUEL_LIGHT_BLOCK.get().defaultBlockState().getFluidState().isSource(),
            "light fuel block is not a source");
        helper.assertTrue(BCEnergyFluids.OIL_BLOCK.get().isFlammable(
            BCEnergyFluids.OIL_BLOCK.get().defaultBlockState(), helper.getLevel(),
            helper.absolutePos(BlockPos.ZERO), net.minecraft.core.Direction.UP
        ), "oil block is not flammable");
        helper.assertTrue(BCEnergyFluids.OIL_BUCKET.get().getContent() == BCEnergyFluids.OIL.get(),
            "oil bucket has the wrong fluid");
        helper.assertTrue(BCEnergyFluids.FUEL_LIGHT_BUCKET.get().getContent() == BCEnergyFluids.FUEL_LIGHT.get(),
            "light fuel bucket has the wrong fluid");

        IFuel oil = BuildcraftFuelRegistry.fuel.getFuel(
            new net.neoforged.neoforge.fluids.FluidStack(BCEnergyFluids.OIL.get(), 1)
        );
        IFuel fuel = BuildcraftFuelRegistry.fuel.getFuel(
            new net.neoforged.neoforge.fluids.FluidStack(BCEnergyFluids.FUEL_LIGHT.get(), 1)
        );
        helper.assertTrue(oil != null, "oil fuel definition missing");
        helper.assertValueEqual(oil.getPowerPerCycle(), 3 * MjAPI.MJ, "oil fuel power");
        helper.assertValueEqual(oil.getTotalBurningTime(), 5_000, "oil burn time");
        helper.assertTrue(fuel != null, "light fuel definition missing");
        helper.assertValueEqual(fuel.getPowerPerCycle(), 6 * MjAPI.MJ, "light fuel power");
        helper.assertValueEqual(fuel.getTotalBurningTime(), 10_000, "light fuel burn time");
        net.neoforged.neoforge.fluids.FluidStack water = new net.neoforged.neoforge.fluids.FluidStack(
            net.minecraft.world.level.material.Fluids.WATER, 1
        );
        helper.assertTrue(BuildcraftFuelRegistry.coolant.getCoolant(water) != null,
            "water coolant definition missing");
        helper.assertValueEqual(
            BuildcraftFuelRegistry.coolant.getDegreesPerMb(water, 500), 0.0023F,
            "water cooling coefficient"
        );
        helper.succeed();
    }

    private static void stirlingEngine(GameTestHelper helper) {
        BlockState state = BCCoreBlocks.ENGINE.get().defaultBlockState()
            .setValue(BlockEngine.ENGINE_TYPE, EnumEngineType.STONE)
            .setValue(BlockEngine.FACING, net.minecraft.core.Direction.UP);
        BlockPos enginePos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(enginePos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        StirlingEngineBlockEntity engine =
            (StirlingEngineBlockEntity) helper.getLevel().getBlockEntity(enginePos);
        helper.assertTrue(engine != null, "stirling engine block entity missing");
        helper.assertTrue(helper.getLevel().getCapability(
            MjAPI.CAP_CONNECTOR, enginePos, net.minecraft.core.Direction.UP
        ) != null, "stirling engine connector capability missing");
        net.neoforged.neoforge.transfer.ResourceHandler<
            net.neoforged.neoforge.transfer.item.ItemResource
        > fuelHandler = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            enginePos, net.minecraft.core.Direction.NORTH
        );
        helper.assertTrue(fuelHandler != null, "stirling engine fuel capability missing");
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(fuelHandler.insert(
                0, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.COBBLESTONE), 1, transaction
            ), 0, "stirling engine accepted a non-fuel item");
            helper.assertValueEqual(fuelHandler.insert(
                0, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.COAL), 1, transaction
            ), 1, "stirling engine rejected coal");
            transaction.commit();
        }
        TestMjReceiver receiver = new TestMjReceiver();
        for (int tick = 0; tick < 16; tick++) engine.tickCycle(true, receiver);
        helper.assertTrue(engine.burnTime() > 0, "stirling engine did not consume solid fuel");
        helper.assertValueEqual(engine.fuelInventory().getAmountAsInt(0), 0,
            "stirling engine retained consumed fuel");
        helper.assertTrue(receiver.received > 0, "stirling engine did not emit MJ");
        helper.assertTrue(engine.storedPower() > 0, "stirling engine did not buffer MJ");
        helper.succeed();
    }

    private static void combustionEngine(GameTestHelper helper) {
        BlockState state = BCCoreBlocks.ENGINE.get().defaultBlockState()
            .setValue(BlockEngine.ENGINE_TYPE, EnumEngineType.IRON)
            .setValue(BlockEngine.FACING, net.minecraft.core.Direction.UP);
        BlockPos enginePos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(enginePos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        CombustionEngineBlockEntity engine =
            (CombustionEngineBlockEntity) helper.getLevel().getBlockEntity(enginePos);
        helper.assertTrue(engine != null, "combustion engine block entity missing");
        net.neoforged.neoforge.transfer.ResourceHandler<
            net.neoforged.neoforge.transfer.fluid.FluidResource
        > fluidHandler = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
            enginePos, net.minecraft.core.Direction.NORTH
        );
        helper.assertTrue(fluidHandler != null, "combustion engine fluid capability missing");
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(fluidHandler.insert(
                CombustionEngineBlockEntity.FUEL_TANK,
                net.neoforged.neoforge.transfer.fluid.FluidResource.of(BCEnergyFluids.FUEL_LIGHT.get()),
                1_000, transaction
            ), 1_000, "combustion engine rejected fuel");
            helper.assertValueEqual(fluidHandler.insert(
                CombustionEngineBlockEntity.COOLANT_TANK,
                net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER),
                1_000, transaction
            ), 1_000, "combustion engine rejected water coolant");
            helper.assertValueEqual(fluidHandler.insert(
                CombustionEngineBlockEntity.RESIDUE_TANK,
                net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER),
                1, transaction
            ), 0, "combustion engine accepted residue input");
            transaction.commit();
        }
        TestMjReceiver receiver = new TestMjReceiver();
        for (int tick = 0; tick < 6_000; tick++) engine.tickCycle(true, receiver);
        helper.assertTrue(receiver.received > 0, "combustion engine did not emit MJ");
        helper.assertTrue(engine.tanks().getAmountAsInt(CombustionEngineBlockEntity.FUEL_TANK) < 1_000,
            "combustion engine did not consume fuel");
        helper.assertTrue(engine.tanks().getAmountAsInt(CombustionEngineBlockEntity.COOLANT_TANK) < 1_000,
            "combustion engine did not consume coolant above ideal heat");
        helper.assertTrue(engine.heat() < CombustionEngineBlockEntity.MAX_HEAT,
            "cooled combustion engine overheated");
        helper.succeed();
    }

    private static void rfEngine(GameTestHelper helper) {
        BlockState state = BCCoreBlocks.ENGINE.get().defaultBlockState()
            .setValue(BlockEngine.ENGINE_TYPE, EnumEngineType.RF)
            .setValue(BlockEngine.FACING, net.minecraft.core.Direction.UP);
        BlockPos enginePos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(enginePos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        RfEngineBlockEntity engine = (RfEngineBlockEntity) helper.getLevel().getBlockEntity(enginePos);
        helper.assertTrue(engine != null, "RF engine block entity missing");
        net.neoforged.neoforge.transfer.energy.EnergyHandler energy = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
            enginePos, net.minecraft.core.Direction.NORTH
        );
        helper.assertTrue(energy != null, "RF engine energy capability missing");
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(energy.insert(1_000, transaction), 1_000,
                "RF engine rejected external energy");
            helper.assertValueEqual(energy.extract(1, transaction), 0,
                "RF engine allowed external energy extraction");
            transaction.commit();
        }
        engine.upgrades().set(0,
            net.neoforged.neoforge.transfer.item.ItemResource.of(BCCoreItems.GEAR_IRON.get()), 1);
        engine.upgrades().set(1,
            net.neoforged.neoforge.transfer.item.ItemResource.of(BCCoreItems.GEAR_GOLD.get()), 1);
        helper.assertValueEqual(engine.mjPerTick(), 9 * MjAPI.MJ, "RF engine upgraded output");
        helper.assertValueEqual(engine.energyConsumptionRate(), 90, "RF engine upgraded consumption");
        TestMjReceiver receiver = new TestMjReceiver();
        for (int tick = 0; tick < 16; tick++) engine.tickCycle(true, receiver);
        helper.assertTrue(energy.getAmountAsInt() < 1_000, "RF engine did not consume external energy");
        helper.assertTrue(receiver.received > 0, "RF engine did not emit MJ");
        helper.assertTrue(engine.heat() > 20, "RF engine did not heat while converting energy");
        helper.succeed();
    }

    private static void engineMenus(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player player =
            helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);

        BlockPos stirlingPos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(stirlingPos, BCCoreBlocks.ENGINE.get().defaultBlockState()
            .setValue(BlockEngine.ENGINE_TYPE, EnumEngineType.STONE),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        player.getInventory().setItem(0, new ItemStack(Items.COAL));
        EngineMenu stirlingMenu = new EngineMenu(1, player.getInventory(), stirlingPos);
        helper.assertValueEqual(stirlingMenu.kind(), EngineMenu.EngineKind.STIRLING,
            "stirling menu kind");
        helper.assertTrue(!stirlingMenu.quickMoveStack(player, 28).isEmpty(),
            "stirling menu shift-click rejected coal");
        StirlingEngineBlockEntity stirling =
            (StirlingEngineBlockEntity) helper.getLevel().getBlockEntity(stirlingPos);
        helper.assertValueEqual(stirling.fuelInventory().getAmountAsInt(0), 1,
            "stirling menu did not update fuel handler");

        BlockPos rfPos = helper.absolutePos(new BlockPos(2, 1, 0));
        helper.getLevel().setBlock(rfPos, BCCoreBlocks.ENGINE.get().defaultBlockState()
            .setValue(BlockEngine.ENGINE_TYPE, EnumEngineType.RF),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        player.getInventory().setItem(0, new ItemStack(BCCoreItems.GEAR_IRON.get()));
        EngineMenu rfMenu = new EngineMenu(2, player.getInventory(), rfPos);
        helper.assertValueEqual(rfMenu.kind(), EngineMenu.EngineKind.RF, "RF menu kind");
        helper.assertTrue(!rfMenu.quickMoveStack(player, 31).isEmpty(),
            "RF menu shift-click rejected upgrade");
        RfEngineBlockEntity rf = (RfEngineBlockEntity) helper.getLevel().getBlockEntity(rfPos);
        helper.assertValueEqual(rf.upgrades().getAmountAsInt(0), 1,
            "RF menu did not update upgrade handler");
        helper.assertValueEqual(rfMenu.outputMjHundredths(), 600,
            "RF menu synchronized upgraded output");

        BlockPos combustionPos = helper.absolutePos(new BlockPos(4, 1, 0));
        helper.getLevel().setBlock(combustionPos, BCCoreBlocks.ENGINE.get().defaultBlockState()
            .setValue(BlockEngine.ENGINE_TYPE, EnumEngineType.IRON),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        CombustionEngineBlockEntity combustion =
            (CombustionEngineBlockEntity) helper.getLevel().getBlockEntity(combustionPos);
        combustion.tanks().set(CombustionEngineBlockEntity.FUEL_TANK,
            net.neoforged.neoforge.transfer.fluid.FluidResource.of(BCEnergyFluids.OIL.get()), 500);
        combustion.tickCycle(true, new TestMjReceiver());
        EngineMenu combustionMenu = new EngineMenu(3, player.getInventory(), combustionPos);
        helper.assertValueEqual(combustionMenu.kind(), EngineMenu.EngineKind.COMBUSTION,
            "combustion menu kind");
        helper.assertTrue(combustionMenu.fuelOrEnergy() < 500,
            "combustion menu fuel amount was not synchronized");
        helper.assertTrue(combustionMenu.heatHundredths() > 2_000,
            "combustion menu heat was not synchronized");
        helper.succeed();
    }

    private static void combustionContainers(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(0, 1, 0);
        BlockPos enginePos = helper.absolutePos(relativePos);
        BlockState state = BCCoreBlocks.ENGINE.get().defaultBlockState()
            .setValue(BlockEngine.ENGINE_TYPE, EnumEngineType.IRON);
        helper.getLevel().setBlock(enginePos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        CombustionEngineBlockEntity engine =
            (CombustionEngineBlockEntity) helper.getLevel().getBlockEntity(enginePos);
        net.minecraft.world.entity.player.Player player =
            helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(enginePos),
            net.minecraft.core.Direction.UP, enginePos, false);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BCEnergyFluids.OIL_BUCKET.get()));
        InteractionResult inserted = state.useItemOn(player.getMainHandItem(), helper.getLevel(), player,
            InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(inserted.consumesAction(), "combustion engine rejected oil bucket interaction");
        helper.assertValueEqual(engine.tanks().getAmountAsInt(CombustionEngineBlockEntity.FUEL_TANK), 1_000,
            "oil bucket did not fill combustion fuel tank");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET),
            "oil bucket did not return an empty bucket");

        engine.tanks().set(CombustionEngineBlockEntity.RESIDUE_TANK,
            net.neoforged.neoforge.transfer.fluid.FluidResource.of(BCEnergyFluids.FUEL_LIGHT.get()), 1_000);
        InteractionResult extracted = state.useItemOn(player.getMainHandItem(), helper.getLevel(), player,
            InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(extracted.consumesAction(), "combustion engine rejected residue bucket interaction");
        helper.assertTrue(player.getMainHandItem().is(BCEnergyFluids.FUEL_LIGHT_BUCKET.get()),
            "empty bucket did not collect combustion residue");
        helper.assertValueEqual(engine.tanks().getAmountAsInt(CombustionEngineBlockEntity.RESIDUE_TANK), 0,
            "residue tank was not drained into bucket");
        helper.succeed();
    }

    private static void energyEngineRecipes(GameTestHelper helper) {
        assertEngineRecipe(helper, EnumEngineType.STONE, java.util.List.of(
            new ItemStack(Items.COBBLESTONE), new ItemStack(Items.COBBLESTONE), new ItemStack(Items.COBBLESTONE),
            ItemStack.EMPTY, new ItemStack(Items.GLASS), ItemStack.EMPTY,
            new ItemStack(BCCoreItems.GEAR_STONE.get()), new ItemStack(Items.PISTON),
            new ItemStack(BCCoreItems.GEAR_STONE.get())
        ));
        assertEngineRecipe(helper, EnumEngineType.IRON, java.util.List.of(
            new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT),
            ItemStack.EMPTY, new ItemStack(Items.GLASS), ItemStack.EMPTY,
            new ItemStack(BCCoreItems.GEAR_IRON.get()), new ItemStack(Items.PISTON),
            new ItemStack(BCCoreItems.GEAR_IRON.get())
        ));
        assertEngineRecipe(helper, EnumEngineType.RF, java.util.List.of(
            new ItemStack(Items.REDSTONE), new ItemStack(Items.REDSTONE), new ItemStack(Items.REDSTONE),
            ItemStack.EMPTY, new ItemStack(Items.GLASS), ItemStack.EMPTY,
            new ItemStack(BCCoreItems.GEAR_IRON.get()), new ItemStack(Items.PISTON),
            new ItemStack(BCCoreItems.GEAR_IRON.get())
        ));
        helper.succeed();
    }

    private static void assertEngineRecipe(GameTestHelper helper, EnumEngineType expected,
        java.util.List<ItemStack> stacks) {
        net.minecraft.world.item.crafting.CraftingInput input =
            net.minecraft.world.item.crafting.CraftingInput.of(3, 3, stacks);
        net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> recipe =
            helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel()
            ).orElseThrow();
        ItemStack output = recipe.value().assemble(input);
        helper.assertTrue(output.is(BCCoreItems.ENGINE.get()), expected + " recipe returned wrong item");
        EnumEngineType actual = output.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
            .get(BlockEngine.ENGINE_TYPE);
        helper.assertValueEqual(actual, expected, expected + " recipe returned wrong engine state");
    }

    private static void energyEngineLoot(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        for (EnumEngineType expected : java.util.List.of(
            EnumEngineType.STONE, EnumEngineType.IRON, EnumEngineType.RF
        )) {
            BlockState state = BCCoreBlocks.ENGINE.get().defaultBlockState()
                .setValue(BlockEngine.ENGINE_TYPE, expected);
            helper.getLevel().setBlock(pos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
            java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                state, helper.getLevel(), pos, helper.getLevel().getBlockEntity(pos)
            );
            helper.assertValueEqual(drops.size(), 1, expected + " engine returned wrong drop count");
            ItemStack drop = drops.getFirst();
            helper.assertTrue(drop.is(BCCoreItems.ENGINE.get()), expected + " engine returned wrong drop item");
            EnumEngineType actual = drop.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
                .get(BlockEngine.ENGINE_TYPE);
            helper.assertValueEqual(actual, expected, expected + " engine drop lost its block state");
        }
        helper.succeed();
    }

    private static void mjDynamo(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockState state = buildcraft.energy.BCEnergyBlocks.MJ_DYNAMO.get().defaultBlockState()
            .setValue(buildcraft.energy.block.BlockDynamoMj.FACING, net.minecraft.core.Direction.UP);
        helper.getLevel().setBlock(pos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        buildcraft.energy.block.entity.DynamoMjBlockEntity dynamo =
            (buildcraft.energy.block.entity.DynamoMjBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(dynamo != null, "MJ Dynamo block entity missing");

        buildcraft.api.mj.IMjReceiver mjInput = helper.getLevel().getCapability(
            MjAPI.CAP_RECEIVER, pos, net.minecraft.core.Direction.NORTH
        );
        helper.assertTrue(mjInput != null, "MJ Dynamo input capability missing");
        helper.assertTrue(helper.getLevel().getCapability(
            MjAPI.CAP_RECEIVER, pos, net.minecraft.core.Direction.UP
        ) == null, "MJ Dynamo exposed MJ input on its output face");
        helper.assertTrue(helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
            pos, net.minecraft.core.Direction.UP
        ) != null, "MJ Dynamo energy output capability missing");
        helper.assertValueEqual(mjInput.receivePower(20 * MjAPI.MJ, false), 0L,
            "MJ Dynamo rejected MJ input");

        dynamo.upgrades().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(BCCoreItems.GEAR_IRON.get()), 1);
        dynamo.upgrades().set(1, net.neoforged.neoforge.transfer.item.ItemResource.of(BCCoreItems.GEAR_GOLD.get()), 1);
        helper.assertValueEqual(dynamo.mjPerTick(), 9 * MjAPI.MJ, "MJ Dynamo upgrade rate mismatch");
        net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler target =
            new net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler(10_000, 10_000, 0);
        dynamo.tickCycle(true, target);
        helper.assertValueEqual(target.getAmountAsInt(), 90, "MJ Dynamo did not emit converted energy");
        helper.assertValueEqual(dynamo.storedMj(), 11 * MjAPI.MJ, "MJ Dynamo consumed wrong MJ amount");
        net.minecraft.world.entity.player.Player menuPlayer =
            helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        EngineMenu menu = new EngineMenu(4, menuPlayer.getInventory(), pos);
        helper.assertValueEqual(menu.kind(), EngineMenu.EngineKind.DYNAMO, "MJ Dynamo menu kind");
        helper.assertValueEqual(menu.fuelOrEnergy(), 0, "MJ Dynamo menu energy status");
        helper.assertValueEqual(menu.storedMjHundredths(), 1_100, "MJ Dynamo menu MJ status");
        helper.assertValueEqual(menu.outputMjHundredths(), 900, "MJ Dynamo menu output status");

        net.minecraft.world.item.crafting.CraftingInput recipeInput =
            net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.REDSTONE), new ItemStack(Items.GLASS), new ItemStack(Items.REDSTONE),
                ItemStack.EMPTY, new ItemStack(Items.PISTON), ItemStack.EMPTY,
                new ItemStack(BCCoreItems.GEAR_IRON.get()), new ItemStack(Items.GLASS),
                new ItemStack(BCCoreItems.GEAR_IRON.get())
            ));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
            net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel()
        ).orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.energy.BCEnergyItems.MJ_DYNAMO.get()),
            "MJ Dynamo recipe returned wrong item");
        java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
            state, helper.getLevel(), pos, dynamo
        );
        helper.assertValueEqual(drops.size(), 1, "MJ Dynamo returned wrong drop count");
        helper.assertTrue(drops.getFirst().is(buildcraft.energy.BCEnergyItems.MJ_DYNAMO.get()),
            "MJ Dynamo returned wrong drop item");
        helper.succeed();
    }

    private static void transportFluidPipeFoundation(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos firstPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos secondPos = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockPos stonePos = helper.absolutePos(new BlockPos(2, 1, 0));
        helper.getLevel().setBlock(firstPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_FLUID), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(secondPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_FLUID), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(stonePos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.STONE_FLUID), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.assertTrue(helper.getLevel().getBlockState(firstPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST), "matching fluid pipes did not connect");
        helper.assertTrue(!helper.getLevel().getBlockState(secondPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST), "different fluid pipe materials connected");
        var handler = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                firstPos, net.minecraft.core.Direction.WEST);
        helper.assertTrue(handler != null, "fluid pipe did not expose a sided fluid capability");
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(water, 500, transaction), 500,
                    "fluid pipe rejected valid water insertion");
            transaction.commit();
        }
        helper.runAfterDelay(5, () -> {
            var second = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                    helper.getLevel().getBlockEntity(secondPos);
            helper.assertTrue(second.fluidBuffer().getAmountAsInt(0) > 0,
                    "fluid pipe did not transfer fluid to its neighbour");
            helper.assertTrue(second.fluidBuffer().getResource(0).equals(water),
                    "fluid pipe changed the transferred resource");
            var drops = net.minecraft.world.level.block.Block.getDrops(
                    helper.getLevel().getBlockState(secondPos), helper.getLevel(), secondPos, second);
            helper.assertValueEqual(drops.size(), 1, "fluid pipe returned wrong drop count");
            helper.assertTrue(drops.getFirst().is(buildcraft.transport.BCTransportItems.PIPE_COBBLE_FLUID.get()),
                    "fluid pipe returned wrong drop item");
            helper.succeed();
        });
    }

    private static void transportPowerPipeFoundation(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos firstPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos secondPos = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockPos stonePos = helper.absolutePos(new BlockPos(2, 1, 0));
        helper.getLevel().setBlock(firstPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_POWER), Block.UPDATE_ALL);
        helper.getLevel().setBlock(secondPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_POWER), Block.UPDATE_ALL);
        helper.getLevel().setBlock(stonePos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.STONE_POWER), Block.UPDATE_ALL);
        helper.assertTrue(helper.getLevel().getBlockState(firstPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST), "matching power pipes did not connect");
        helper.assertTrue(!helper.getLevel().getBlockState(secondPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST), "separate power materials connected");
        helper.assertTrue(helper.getLevel().getCapability(
                MjAPI.CAP_CONNECTOR, firstPos, net.minecraft.core.Direction.WEST) != null,
                "power pipe did not expose its MJ connector");
        helper.assertTrue(helper.getLevel().getCapability(
                MjAPI.CAP_RECEIVER, firstPos, net.minecraft.core.Direction.WEST) == null,
                "non-wooden power pipe incorrectly accepted direct MJ input");
        helper.assertValueEqual(buildcraft.transport.PipeType.COBBLESTONE_POWER.powerTransferPerTick(),
                4 * MjAPI.MJ, "cobblestone power transfer limit");
        helper.assertValueEqual(buildcraft.transport.PipeType.STONE_POWER.powerTransferPerTick(),
                8 * MjAPI.MJ, "stone power transfer limit");
        helper.assertValueEqual(buildcraft.transport.PipeType.QUARTZ_POWER.powerTransferPerTick(),
                32 * MjAPI.MJ, "quartz power transfer limit");
        helper.assertValueEqual(buildcraft.transport.PipeType.COBBLESTONE_POWER.powerResistancePerTick(),
                MjAPI.MJ / 16, "cobblestone power resistance");
        helper.assertValueEqual(buildcraft.transport.PipeType.STONE_POWER.powerResistancePerTick(),
                MjAPI.MJ / 32, "stone power resistance");
        var pipe = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(firstPos);
        var drops = Block.getDrops(helper.getLevel().getBlockState(firstPos), helper.getLevel(), firstPos, pipe);
        helper.assertValueEqual(drops.size(), 1, "power pipe returned wrong drop count");
        helper.assertTrue(drops.getFirst().is(buildcraft.transport.BCTransportItems.PIPE_COBBLE_POWER.get()),
                "power pipe returned wrong drop item");
        helper.succeed();
    }

    private static void transportWoodPowerPipe(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos woodPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos cobblePos = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockPos otherWoodPos = helper.absolutePos(new BlockPos(0, 1, 1));
        helper.getLevel().setBlock(woodPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.WOOD_POWER), Block.UPDATE_ALL);
        helper.getLevel().setBlock(cobblePos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_POWER), Block.UPDATE_ALL);
        helper.getLevel().setBlock(otherWoodPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.WOOD_POWER), Block.UPDATE_ALL);
        helper.assertTrue(helper.getLevel().getBlockState(woodPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST), "wooden power pipe rejected a normal power pipe");
        helper.assertTrue(!helper.getLevel().getBlockState(woodPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.SOUTH), "wooden power pipes connected to each other");
        IMjReceiver receiver = helper.getLevel().getCapability(
                MjAPI.CAP_RECEIVER, woodPos, net.minecraft.core.Direction.WEST);
        helper.assertTrue(receiver != null, "wooden power pipe receiver capability missing");
        helper.assertValueEqual(receiver.getPowerRequested(), 16 * MjAPI.MJ,
                "wooden power request did not use its transfer ceiling");
        helper.assertValueEqual(receiver.receivePower(20 * MjAPI.MJ, true), 4 * MjAPI.MJ,
                "wooden power simulation returned wrong excess");
        var wood = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(woodPos);
        helper.assertValueEqual(wood.powerStored(), 0L, "simulated power mutated the wooden buffer");
        helper.assertValueEqual(receiver.receivePower(20 * MjAPI.MJ, false), 4 * MjAPI.MJ,
                "wooden power overload was not returned");
        helper.assertValueEqual(buildcraft.transport.PipeType.WOOD_POWER.powerResistancePerTick(),
                MjAPI.MJ / 128, "wooden power resistance");
        helper.runAfterDelay(2, () -> {
            var cobble = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                    helper.getLevel().getBlockEntity(cobblePos);
            helper.assertValueEqual(4 * MjAPI.MJ, cobble.powerStored(),
                    "cobblestone pipe did not enforce its 4 MJ/t input ceiling");
            helper.assertValueEqual(12 * MjAPI.MJ, wood.powerStored(),
                    "wooden pipe lost rejected power");
            helper.succeed();
        });
    }

    private static void transportGeneralPowerPipes(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos sandstonePos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos ironPos = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockPos goldPos = helper.absolutePos(new BlockPos(2, 1, 0));
        helper.getLevel().setBlock(sandstonePos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.SANDSTONE_POWER), Block.UPDATE_ALL);
        helper.getLevel().setBlock(ironPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.IRON_POWER), Block.UPDATE_ALL);
        helper.getLevel().setBlock(goldPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.GOLD_POWER), Block.UPDATE_ALL);
        helper.assertTrue(helper.getLevel().getBlockState(sandstonePos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST),
                "sandstone power pipe rejected another power pipe");
        helper.assertTrue(helper.getLevel().getBlockState(ironPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST),
                "iron power pipe rejected golden power pipe");
        helper.assertTrue(!buildcraft.transport.PipeType.SANDSTONE_POWER.connectsPowerHandlers(),
                "sandstone power pipe exposed machine connections");
        helper.assertValueEqual(16 * MjAPI.MJ,
                buildcraft.transport.PipeType.SANDSTONE_POWER.powerTransferPerTick(),
                "sandstone power transfer limit");
        helper.assertValueEqual(32 * MjAPI.MJ,
                buildcraft.transport.PipeType.IRON_POWER.powerTransferPerTick(),
                "iron power transfer limit");
        helper.assertValueEqual(128 * MjAPI.MJ,
                buildcraft.transport.PipeType.GOLD_POWER.powerTransferPerTick(),
                "gold power transfer limit");
        var iron = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(ironPos);
        long[] limits = { 16, 8, 4, 2, 1, 0, 32 };
        for (int index = 0; index < limits.length; index++) {
            helper.assertTrue(iron.rotatePipeDirection(), "iron power limiter did not rotate");
            helper.assertValueEqual(limits[index] * MjAPI.MJ, iron.effectivePowerTransferPerTick(),
                    "iron power limiter step " + index);
        }
        helper.assertValueEqual(0, iron.powerLimitShift(), "iron power limiter did not wrap");
        var drops = Block.getDrops(helper.getLevel().getBlockState(goldPos), helper.getLevel(), goldPos,
                helper.getLevel().getBlockEntity(goldPos));
        helper.assertValueEqual(1, drops.size(), "gold power pipe returned wrong drop count");
        helper.assertTrue(drops.getFirst().is(buildcraft.transport.BCTransportItems.PIPE_GOLD_POWER.get()),
                "gold power pipe returned wrong drop");
        helper.succeed();
    }

    private static void transportDiamondPowerPipes(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos inputPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos diamondPos = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockPos woodPos = helper.absolutePos(new BlockPos(0, 1, 1));
        helper.getLevel().setBlock(inputPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.DIAMOND_WOOD_POWER), Block.UPDATE_ALL);
        helper.getLevel().setBlock(diamondPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.DIAMOND_POWER), Block.UPDATE_ALL);
        helper.getLevel().setBlock(woodPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.WOOD_POWER), Block.UPDATE_ALL);
        helper.assertTrue(helper.getLevel().getBlockState(inputPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST),
                "diamond wooden power pipe rejected diamond power pipe");
        helper.assertTrue(!helper.getLevel().getBlockState(inputPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.SOUTH),
                "two wooden power inputs connected");
        IMjReceiver receiver = helper.getLevel().getCapability(
                MjAPI.CAP_RECEIVER, inputPos, net.minecraft.core.Direction.WEST);
        helper.assertTrue(receiver != null, "diamond wooden power receiver missing");
        helper.assertValueEqual(256 * MjAPI.MJ, receiver.getPowerRequested(),
                "diamond wooden request ceiling");
        helper.assertValueEqual(44 * MjAPI.MJ, receiver.receivePower(300 * MjAPI.MJ, true),
                "diamond wooden simulation excess");
        var input = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(inputPos);
        helper.assertValueEqual(0L, input.powerStored(), "diamond wooden simulation mutated power");
        helper.assertValueEqual(44 * MjAPI.MJ, receiver.receivePower(300 * MjAPI.MJ, false),
                "diamond wooden committed excess");
        var diamond = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(diamondPos);
        long[] limits = { 128, 64, 32, 16, 8, 0, 256 };
        for (int index = 0; index < limits.length; index++) {
            helper.assertTrue(diamond.rotatePipeDirection(), "diamond power limiter did not rotate");
            helper.assertValueEqual(limits[index] * MjAPI.MJ, diamond.effectivePowerTransferPerTick(),
                    "diamond power limiter step " + index);
        }
        helper.assertValueEqual(MjAPI.MJ / 32,
                buildcraft.transport.PipeType.DIAMOND_WOOD_POWER.powerResistancePerTick(),
                "diamond wooden resistance");
        var drops = Block.getDrops(helper.getLevel().getBlockState(diamondPos), helper.getLevel(), diamondPos,
                diamond);
        helper.assertValueEqual(1, drops.size(), "diamond power pipe returned wrong drop count");
        helper.assertTrue(drops.getFirst().is(buildcraft.transport.BCTransportItems.PIPE_DIAMOND_POWER.get()),
                "diamond power pipe returned wrong drop");
        helper.succeed();
    }

    private static void transportBranchedPowerNetwork(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos inputPos = helper.absolutePos(new BlockPos(1, 3, 2));
        BlockPos junctionPos = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos eastPos = helper.absolutePos(new BlockPos(3, 3, 2));
        BlockPos southPos = helper.absolutePos(new BlockPos(2, 3, 3));
        helper.getLevel().setBlock(inputPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.WOOD_POWER), Block.UPDATE_ALL);
        helper.getLevel().setBlock(junctionPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.IRON_POWER), Block.UPDATE_ALL);
        helper.getLevel().setBlock(eastPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_POWER), Block.UPDATE_ALL);
        helper.getLevel().setBlock(southPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_POWER), Block.UPDATE_ALL);
        IMjReceiver input = helper.getLevel().getCapability(MjAPI.CAP_RECEIVER, inputPos, Direction.WEST);
        helper.assertTrue(input != null, "branched network wooden input receiver missing");
        helper.assertValueEqual(0L, input.receivePower(16 * MjAPI.MJ, false),
                "branched network input rejected nominal power");
        tickPipes(helper, 3, inputPos, junctionPos, eastPos, southPos);
        var wood = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(inputPos);
        var junction = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(junctionPos);
        var east = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(eastPos);
        var south = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(southPos);
        helper.assertValueEqual(4 * MjAPI.MJ, east.powerStored(),
                "branched network did not fill the east endpoint");
        helper.assertValueEqual(4 * MjAPI.MJ, south.powerStored(),
                "branched network did not rotate to the south endpoint");
        helper.assertValueEqual(16 * MjAPI.MJ,
                wood.powerStored() + junction.powerStored() + east.powerStored() + south.powerStored(),
                "branched network did not conserve buffered power");
        junction.activatePowerLimit(3);
        helper.assertValueEqual(4 * MjAPI.MJ, junction.effectivePowerTransferPerTick(),
                "branched junction limiter shift did not apply");
        var tag = junction.saveWithFullMetadata(helper.getLevel().registryAccess());
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                junctionPos, junction.getBlockState(), tag, helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof buildcraft.transport.block.entity.PipeHolderBlockEntity,
                "power pipe block entity did not reload from its saved tag");
        var restored = (buildcraft.transport.block.entity.PipeHolderBlockEntity) loaded;
        helper.assertValueEqual(junction.powerStored(), restored.powerStored(),
                "power buffer did not survive block-entity reload");
        helper.assertValueEqual(3, restored.powerLimitShift(),
                "power limiter shift did not survive block-entity reload");
        helper.assertValueEqual(32 * MjAPI.MJ, restored.powerCapacity(),
                "power meter capacity did not survive block-entity reload");
        helper.assertTrue(restored.isPowerLimiter(), "restored iron pipe lost limiter identity");
        helper.succeed();
    }

    private static void energyRefineryFluids(GameTestHelper helper) {
        helper.assertValueEqual(10, buildcraft.energy.BCEnergyFluids.REFINERY_FLUIDS.size(),
                "refinery fluid family count");
        java.util.Set<net.minecraft.world.level.material.Fluid> fluids =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (var family : buildcraft.energy.BCEnergyFluids.REFINERY_FLUIDS.values()) {
            helper.assertValueEqual(3, family.variants().size(), family.name() + " heat-state count");
            for (int heat = 0; heat < 3; heat++) {
                var variant = family.heat(heat);
                helper.assertTrue(fluids.add(variant.source().get()),
                        "duplicate refinery source fluid for " + variant.name());
                helper.assertTrue(variant.block().get().defaultBlockState().getFluidState().getType()
                                == variant.source().get(),
                        "refinery block/source mismatch for " + variant.name());
                helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(
                                variant.bucket().get()).getPath().equals(variant.name() + "_bucket"),
                        "refinery bucket registry mismatch for " + variant.name());
            }
        }
        helper.assertValueEqual(30, fluids.size(), "refinery source fluid identity count");
        helper.assertValueEqual(10,
                buildcraft.energy.BCEnergyRefineryRecipes.distillationRecipes().size(),
                "distillation recipe count");
        helper.assertValueEqual(20, buildcraft.energy.BCEnergyRefineryRecipes.heatingRecipes().size(),
                "heating recipe count");
        helper.assertValueEqual(20, buildcraft.energy.BCEnergyRefineryRecipes.coolingRecipes().size(),
                "cooling recipe count");
        var hotOil = buildcraft.energy.BCEnergyFluids.refineryFluid("oil").heat(2).source().get();
        var oilRecipe = buildcraft.energy.BCEnergyRefineryRecipes.distillation(hotOil);
        helper.assertTrue(oilRecipe != null, "hot oil distillation recipe missing");
        helper.assertValueEqual(16, oilRecipe.inputAmount(), "hot oil input ratio");
        helper.assertValueEqual(8, oilRecipe.gasAmount(), "hot oil gas ratio");
        helper.assertValueEqual(1, oilRecipe.liquidAmount(), "hot oil residue ratio");
        helper.assertValueEqual(12 * MjAPI.MJ, oilRecipe.powerRequired(), "hot oil distillation power");
        helper.assertTrue(oilRecipe.gasOutput() ==
                        buildcraft.energy.BCEnergyFluids.refineryFluid("oil_distilled").heat(2).source().get(),
                "hot oil gas output identity");
        helper.assertTrue(oilRecipe.liquidOutput() ==
                        buildcraft.energy.BCEnergyFluids.refineryFluid("oil_residue").heat(2).source().get(),
                "hot oil liquid output identity");
        var coldHeavy = buildcraft.energy.BCEnergyFluids.refineryFluid("oil_heavy").heat(0).source().get();
        var heating = buildcraft.energy.BCEnergyRefineryRecipes.heating(coldHeavy);
        helper.assertTrue(heating != null && heating.output() ==
                        buildcraft.energy.BCEnergyFluids.refineryFluid("oil_heavy").heat(1).source().get(),
                "heavy oil heating transition");
        helper.assertValueEqual(10, heating.amount(), "heat exchange amount");
        var hotCrude = buildcraft.energy.BCEnergyFluids.refineryFluid("oil").heat(1).source().get();
        var crudeFuel = buildcraft.lib.fluid.FuelRegistry.INSTANCE.getFuel(
                new net.neoforged.neoforge.fluids.FluidStack(hotCrude, 1_000));
        helper.assertTrue(crudeFuel instanceof buildcraft.api.fuels.IFuelManager.IDirtyFuel,
                "hot crude oil dirty-fuel registration missing");
        helper.assertValueEqual(3 * MjAPI.MJ, crudeFuel.getPowerPerCycle(), "crude oil MJ cycle");
        helper.assertValueEqual(5_000, crudeFuel.getTotalBurningTime(), "crude oil burn time");
        var dirtyCrude = (buildcraft.api.fuels.IFuelManager.IDirtyFuel) crudeFuel;
        helper.assertValueEqual(62, dirtyCrude.getResidue().getAmount(), "crude oil residue amount");
        helper.assertTrue(dirtyCrude.getResidue().getFluid() ==
                        buildcraft.energy.BCEnergyFluids.refineryFluid("oil_residue").heat(1).source().get(),
                "crude oil residue heat identity");
        helper.succeed();
    }

    private static void factoryTank(GameTestHelper helper) {
        BlockPos bottomPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos topPos = helper.absolutePos(new BlockPos(0, 2, 0));
        BlockState tankState = buildcraft.factory.BCFactoryBlocks.TANK.get().defaultBlockState();
        helper.getLevel().setBlock(bottomPos, tankState, Block.UPDATE_ALL);
        helper.getLevel().setBlock(topPos, tankState.setValue(
                buildcraft.factory.block.TankBlock.JOINED_BELOW, true), Block.UPDATE_ALL);
        helper.assertTrue(helper.getLevel().getBlockState(topPos).getValue(
                buildcraft.factory.block.TankBlock.JOINED_BELOW), "upper tank did not join its lower tank");
        var handler = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                topPos, net.minecraft.core.Direction.NORTH);
        helper.assertTrue(handler != null, "stacked tank fluid capability missing");
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(20_000, handler.insert(water, 20_000, transaction),
                    "stacked tanks rejected valid water");
            transaction.commit();
        }
        var bottom = (buildcraft.factory.block.entity.TankBlockEntity)
                helper.getLevel().getBlockEntity(bottomPos);
        var top = (buildcraft.factory.block.entity.TankBlockEntity)
                helper.getLevel().getBlockEntity(topPos);
        helper.assertValueEqual(16_000, bottom.localStorage().getAmountAsInt(0),
                "liquid did not fill the bottom tank first");
        helper.assertValueEqual(4_000, top.localStorage().getAmountAsInt(0),
                "liquid did not overflow into the upper tank");
        helper.assertValueEqual(32_000L, handler.getCapacityAsLong(0, water),
                "stacked tank capacity");
        var lava = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.LAVA);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(0, handler.insert(lava, 1_000, transaction),
                    "stacked tanks accepted a mixed fluid");
        }
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(5_000, handler.extract(water, 5_000, transaction),
                    "stacked tanks returned wrong drain amount");
            transaction.commit();
        }
        helper.assertValueEqual(0, top.localStorage().getAmountAsInt(0),
                "liquid did not drain from the upper tank first");
        helper.assertValueEqual(15_000, bottom.localStorage().getAmountAsInt(0),
                "stacked tank drain changed the wrong amount");
        helper.assertValueEqual(14, bottom.comparatorLevel(), "tank comparator level");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        var hit = new BlockHitResult(Vec3.atCenterOf(bottomPos), net.minecraft.core.Direction.NORTH,
                bottomPos, false);
        helper.assertTrue(helper.getLevel().getBlockState(bottomPos).useItemOn(
                player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit
        ).consumesAction(), "water bucket did not interact with tank");
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET),
                "tank did not return an empty bucket");
        helper.assertValueEqual(16_000, bottom.localStorage().getAmountAsInt(0),
                "bucket insertion changed wrong amount");
        helper.assertTrue(helper.getLevel().getBlockState(bottomPos).useItemOn(
                player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit
        ).consumesAction(), "empty bucket did not interact with tank");
        helper.assertTrue(player.getMainHandItem().is(Items.WATER_BUCKET),
                "tank did not fill an empty bucket");
        helper.assertValueEqual(15_000, bottom.localStorage().getAmountAsInt(0),
                "bucket extraction changed wrong amount");
        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.GLASS), new ItemStack(Items.GLASS), new ItemStack(Items.GLASS),
                new ItemStack(Items.GLASS), ItemStack.EMPTY, new ItemStack(Items.GLASS),
                new ItemStack(Items.GLASS), new ItemStack(Items.GLASS), new ItemStack(Items.GLASS)
        ));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel()
        ).orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.factory.BCFactoryItems.TANK.get()),
                "tank recipe returned wrong item");
        var drops = Block.getDrops(helper.getLevel().getBlockState(topPos), helper.getLevel(), topPos, top);
        helper.assertValueEqual(1, drops.size(), "tank returned wrong drop count");
        helper.assertTrue(drops.getFirst().is(buildcraft.factory.BCFactoryItems.TANK.get()),
                "tank returned wrong drop");
        helper.succeed();
    }

    private static void factoryFloodGate(GameTestHelper helper) {
        BlockPos gatePos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos outputPos = gatePos.below();
        helper.getLevel().setBlock(outputPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        BlockState state = buildcraft.factory.BCFactoryBlocks.FLOOD_GATE.get().defaultBlockState();
        helper.getLevel().setBlock(gatePos, state, Block.UPDATE_ALL);
        var gate = (buildcraft.factory.block.entity.FloodGateBlockEntity)
                helper.getLevel().getBlockEntity(gatePos);
        helper.assertTrue(gate != null, "flood gate block entity missing");
        var handler = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                gatePos, net.minecraft.core.Direction.UP);
        helper.assertTrue(handler != null, "flood gate fluid capability missing");
        helper.assertValueEqual(2_000L, handler.getCapacityAsLong(0,
                net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                    net.minecraft.world.level.material.Fluids.WATER)), "flood gate capacity");
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(2_000, handler.insert(water, 2_000, transaction),
                    "flood gate rejected water");
            transaction.commit();
        }
        helper.assertTrue(helper.getLevel().getBlockState(outputPos).isAir(),
                "flood gate output position was not air");
        helper.assertValueEqual(outputPos, gate.nextTarget(), "flood gate chose wrong initial target");
        helper.assertTrue(gate.toggleSide(net.minecraft.core.Direction.NORTH),
                "flood gate did not toggle a horizontal side");
        helper.assertTrue(!helper.getLevel().getBlockState(gatePos).getValue(
                buildcraft.factory.block.FloodGateBlock.NORTH), "flood gate side remained open");
        helper.assertTrue(!gate.toggleSide(net.minecraft.core.Direction.UP),
                "flood gate toggled its fixed top side");
        helper.runAfterDelay(17, () -> {
            helper.assertTrue(helper.getLevel().getFluidState(outputPos).isSource(),
                    "flood gate did not place source below itself; remaining=" +
                        gate.fluidBuffer().getAmountAsInt(0));
            helper.assertValueEqual(1_000, gate.fluidBuffer().getAmountAsInt(0),
                    "flood gate consumed wrong amount");
            var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                    new ItemStack(Items.IRON_INGOT), new ItemStack(BCCoreItems.GEAR_IRON.get()),
                    new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.IRON_BARS), new ItemStack(buildcraft.factory.BCFactoryItems.TANK.get()),
                    new ItemStack(Items.IRON_BARS),
                    new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_BARS),
                    new ItemStack(Items.IRON_INGOT)
            ));
            ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                    net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel()
            ).orElseThrow().value().assemble(recipeInput);
            helper.assertTrue(crafted.is(buildcraft.factory.BCFactoryItems.FLOOD_GATE.get()),
                    "flood gate recipe returned wrong item");
            var drops = Block.getDrops(helper.getLevel().getBlockState(gatePos), helper.getLevel(), gatePos, gate);
            helper.assertValueEqual(1, drops.size(), "flood gate returned wrong drop count");
            helper.assertTrue(drops.getFirst().is(buildcraft.factory.BCFactoryItems.FLOOD_GATE.get()),
                    "flood gate returned wrong drop");
            helper.succeed();
        });
    }

    private static void factoryPump(GameTestHelper helper) {
        BlockPos sourcePos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos lowerTubePos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos upperTubePos = helper.absolutePos(new BlockPos(1, 3, 1));
        BlockPos pumpPos = helper.absolutePos(new BlockPos(1, 4, 1));
        BlockPos tankPos = pumpPos.east();
        for (BlockPos pos : java.util.List.of(sourcePos.north(), sourcePos.south(), sourcePos.east(),
                sourcePos.west(), lowerTubePos, upperTubePos)) {
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        helper.getLevel().setBlock(sourcePos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(tankPos, buildcraft.factory.BCFactoryBlocks.TANK.get().defaultBlockState(),
                Block.UPDATE_ALL);
        helper.getLevel().setBlock(pumpPos, buildcraft.factory.BCFactoryBlocks.PUMP.get().defaultBlockState(),
                Block.UPDATE_ALL);
        var receiver = helper.getLevel().getCapability(MjAPI.CAP_RECEIVER, pumpPos, Direction.UP);
        helper.assertTrue(receiver != null, "pump MJ receiver missing");
        helper.assertValueEqual(0L, receiver.receivePower(10 * MjAPI.MJ, false), "pump rejected MJ");
        var fluid = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, pumpPos, Direction.WEST);
        helper.assertTrue(fluid != null, "pump fluid capability missing");
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(0, fluid.insert(water, 1_000, transaction),
                    "pump accepted fluid through output capability");
        }
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(sourcePos).isAir(),
                    "pump did not consume finite water source");
            helper.assertTrue(helper.getLevel().getBlockState(lowerTubePos).is(
                    buildcraft.factory.BCFactoryBlocks.TUBE.get()), "pump did not create lower tube");
            helper.assertTrue(helper.getLevel().getBlockState(upperTubePos).is(
                    buildcraft.factory.BCFactoryBlocks.TUBE.get()), "pump did not create upper tube");
            var tank = (buildcraft.factory.block.entity.TankBlockEntity)
                    helper.getLevel().getBlockEntity(tankPos);
            helper.assertValueEqual(1_000, tank.localStorage().getAmountAsInt(0),
                    "pump did not push water to adjacent tank");
            var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                    new ItemStack(Items.IRON_INGOT), new ItemStack(Items.REDSTONE),
                    new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.IRON_INGOT), new ItemStack(BCCoreItems.GEAR_IRON.get()),
                    new ItemStack(Items.IRON_INGOT),
                    new ItemStack(buildcraft.factory.BCFactoryItems.TANK.get()), new ItemStack(Items.BUCKET),
                    new ItemStack(buildcraft.factory.BCFactoryItems.TANK.get())
            ));
            ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                    net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                    .orElseThrow().value().assemble(recipeInput);
            helper.assertTrue(crafted.is(buildcraft.factory.BCFactoryItems.PUMP.get()),
                    "pump recipe returned wrong item");
            var drops = Block.getDrops(helper.getLevel().getBlockState(pumpPos), helper.getLevel(), pumpPos,
                    helper.getLevel().getBlockEntity(pumpPos));
            helper.assertValueEqual(1, drops.size(), "pump returned wrong drop count");
            helper.assertTrue(drops.getFirst().is(buildcraft.factory.BCFactoryItems.PUMP.get()),
                    "pump returned wrong drop");
            helper.succeed();
        });
    }

    private static void factoryMiningWell(GameTestHelper helper) {
        BlockPos targetPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos upperTubePos = helper.absolutePos(new BlockPos(1, 3, 1));
        BlockPos wellPos = helper.absolutePos(new BlockPos(1, 4, 1));
        helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(upperTubePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(wellPos,
                buildcraft.factory.BCFactoryBlocks.MINING_WELL.get().defaultBlockState(), Block.UPDATE_ALL);
        var receiver = helper.getLevel().getCapability(MjAPI.CAP_RECEIVER, wellPos, Direction.UP);
        helper.assertTrue(receiver != null, "mining well MJ receiver missing");
        helper.assertValueEqual(0L, receiver.receivePower(80 * MjAPI.MJ, false),
                "mining well rejected MJ");
        var output = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, wellPos, Direction.WEST);
        helper.assertTrue(output != null, "mining well item capability missing");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(0, output.insert(
                    net.neoforged.neoforge.transfer.item.ItemResource.of(new ItemStack(Items.COBBLESTONE)),
                    1, transaction), "mining well accepted an input item");
        }
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(targetPos).is(
                    buildcraft.factory.BCFactoryBlocks.TUBE.get()),
                    "mining well did not replace mined block with tube");
            helper.assertTrue(helper.getLevel().getBlockState(upperTubePos).is(
                    buildcraft.factory.BCFactoryBlocks.TUBE.get()),
                    "mining well did not extend its upper tube");
            var well = (buildcraft.factory.block.entity.MiningWellBlockEntity)
                    helper.getLevel().getBlockEntity(wellPos);
            int cobblestone = 0;
            for (int slot = 0; slot < well.internalDrops().size(); slot++) {
                if (well.internalDrops().getResource(slot).is(Items.COBBLESTONE)) {
                    cobblestone += well.internalDrops().getAmountAsInt(slot);
                }
            }
            helper.assertValueEqual(1, cobblestone, "mining well did not retain the mined drop");
            helper.assertValueEqual(0L, well.storedMj(), "mining well consumed wrong MJ amount");
            var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                    new ItemStack(Items.IRON_INGOT), new ItemStack(Items.REDSTONE),
                    new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.IRON_INGOT), new ItemStack(BCCoreItems.GEAR_IRON.get()),
                    new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_PICKAXE),
                    new ItemStack(Items.IRON_INGOT)
            ));
            ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                    net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                    .orElseThrow().value().assemble(recipeInput);
            helper.assertTrue(crafted.is(buildcraft.factory.BCFactoryItems.MINING_WELL.get()),
                    "mining well recipe returned wrong item");
            var drops = Block.getDrops(helper.getLevel().getBlockState(wellPos), helper.getLevel(), wellPos, well);
            helper.assertValueEqual(1, drops.size(), "mining well returned wrong drop count");
            helper.assertTrue(drops.getFirst().is(buildcraft.factory.BCFactoryItems.MINING_WELL.get()),
                    "mining well returned wrong drop");
            helper.getLevel().removeBlock(wellPos, false);
            helper.assertTrue(helper.getLevel().getBlockState(targetPos).isAir()
                            && helper.getLevel().getBlockState(upperTubePos).isAir(),
                    "mining well removal did not clear its tube column");
            helper.succeed();
        });
    }

    private static void factoryChute(GameTestHelper helper) {
        BlockPos chutePos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(chutePos, buildcraft.factory.BCFactoryBlocks.CHUTE.get().defaultBlockState()
                .setValue(buildcraft.factory.block.ChuteBlock.FACING, Direction.UP), Block.UPDATE_ALL);
        var input = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, chutePos, Direction.NORTH);
        helper.assertTrue(input != null, "chute item capability missing");
        var iron = net.neoforged.neoforge.transfer.item.ItemResource.of(new ItemStack(Items.IRON_INGOT));
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(1, input.insert(iron, 1, transaction), "chute rejected inserted item");
            transaction.commit();
        }
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(0, input.extract(iron, 1, transaction),
                    "chute exposed external item extraction");
        }
        var receiver = helper.getLevel().getCapability(MjAPI.CAP_RECEIVER, chutePos, Direction.DOWN);
        helper.assertTrue(receiver != null, "chute MJ receiver missing");
        helper.assertValueEqual(0L, receiver.receivePower(buildcraft.factory.block.entity.ChuteBlockEntity.PICKUP_COST,
                false), "chute rejected pickup power");
        var entity = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),
            chutePos.getX() + 0.5, chutePos.getY() + 1.05, chutePos.getZ() + 0.5,
            new ItemStack(Items.COBBLESTONE, 5));
        entity.setNoGravity(true);
        helper.getLevel().addFreshEntity(entity);
        var chuteNow = (buildcraft.factory.block.entity.ChuteBlockEntity)
            helper.getLevel().getBlockEntity(chutePos);
        buildcraft.factory.block.entity.ChuteBlockEntity.tick(
            helper.getLevel(), chutePos, helper.getLevel().getBlockState(chutePos), chuteNow);
        helper.runAfterDelay(2, () -> {
            var chute = (buildcraft.factory.block.entity.ChuteBlockEntity)
                helper.getLevel().getBlockEntity(chutePos);
            buildcraft.factory.block.entity.ChuteBlockEntity.tick(
                    helper.getLevel(), chutePos, helper.getLevel().getBlockState(chutePos), chute);
            int cobblestone = 0;
            for (int slot = 0; slot < chute.inventory().size(); slot++) {
                if (chute.inventory().getResource(slot).is(Items.COBBLESTONE)) {
                    cobblestone += chute.inventory().getAmountAsInt(slot);
                }
            }
            helper.assertValueEqual(3, cobblestone, "chute did not pick up exactly three items");
            helper.assertValueEqual(2, entity.getItem().getCount(), "chute consumed wrong entity item count");
            var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                    new ItemStack(Items.IRON_INGOT), new ItemStack(Items.CHEST),
                    new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.IRON_INGOT), new ItemStack(BCCoreItems.GEAR_STONE.get()),
                    new ItemStack(Items.IRON_INGOT),
                    ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY
            ));
            ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                    net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                    .orElseThrow().value().assemble(recipeInput);
            helper.assertTrue(crafted.is(buildcraft.factory.BCFactoryItems.CHUTE.get()),
                    "chute recipe returned wrong item");
            var drops = Block.getDrops(helper.getLevel().getBlockState(chutePos), helper.getLevel(), chutePos,
                    chute);
            helper.assertValueEqual(1, drops.size(), "chute returned wrong drop count");
            helper.assertTrue(drops.getFirst().is(buildcraft.factory.BCFactoryItems.CHUTE.get()),
                    "chute returned wrong drop");
            helper.succeed();
        });
    }

    private static void factoryDistiller(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(pos, buildcraft.factory.BCFactoryBlocks.DISTILLER.get().defaultBlockState(),
                Block.UPDATE_ALL);
        var input = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, pos, Direction.NORTH);
        var gasOutput = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, pos, Direction.UP);
        var liquidOutput = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, pos, Direction.DOWN);
        helper.assertTrue(input != null && gasOutput != null && liquidOutput != null,
                "distiller directional fluid capabilities missing");
        var hotOil = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                buildcraft.energy.BCEnergyFluids.refineryFluid("oil").heat(2).source().get());
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(16, input.insert(hotOil, 16, transaction),
                    "distiller rejected hot oil recipe input");
            helper.assertValueEqual(0, input.extract(hotOil, 1, transaction),
                    "distiller exposed input extraction");
            helper.assertValueEqual(0, gasOutput.insert(hotOil, 1, transaction),
                    "distiller gas output accepted insertion");
            transaction.commit();
        }
        var receiver = helper.getLevel().getCapability(MjAPI.CAP_RECEIVER, pos, Direction.WEST);
        helper.assertTrue(receiver != null, "distiller MJ receiver missing");
        helper.assertValueEqual(0L, receiver.receivePower(512 * MjAPI.MJ, false),
                "distiller rejected working power");
        helper.runAfterDelay(3, () -> {
            var distiller = (buildcraft.factory.block.entity.DistillerBlockEntity)
                    helper.getLevel().getBlockEntity(pos);
            helper.assertValueEqual(0, distiller.inputTank().getAmountAsInt(0),
                    "distiller did not consume exact input ratio");
            helper.assertValueEqual(8, distiller.gasOutputTank().getAmountAsInt(0),
                    "distiller produced wrong gas-side amount");
            helper.assertTrue(distiller.gasOutputTank().getResource(0).value() ==
                            buildcraft.energy.BCEnergyFluids.refineryFluid("oil_distilled").heat(2).source().get(),
                    "distiller produced wrong gas-side fluid");
            helper.assertValueEqual(1, distiller.liquidOutputTank().getAmountAsInt(0),
                    "distiller produced wrong liquid-side amount");
            helper.assertTrue(distiller.liquidOutputTank().getResource(0).value() ==
                            buildcraft.energy.BCEnergyFluids.refineryFluid("oil_residue").heat(2).source().get(),
                    "distiller produced wrong liquid-side fluid");
            helper.assertValueEqual(500 * MjAPI.MJ, distiller.storedMj(),
                    "distiller consumed wrong recipe power");
            var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 2, java.util.List.of(
                    new ItemStack(Items.REDSTONE_TORCH), new ItemStack(buildcraft.factory.BCFactoryItems.TANK.get()),
                    new ItemStack(Items.REDSTONE_TORCH),
                    new ItemStack(buildcraft.factory.BCFactoryItems.TANK.get()),
                    new ItemStack(BCCoreItems.GEAR_DIAMOND.get()),
                    new ItemStack(buildcraft.factory.BCFactoryItems.TANK.get())
            ));
            ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                    net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                    .orElseThrow().value().assemble(recipeInput);
            helper.assertTrue(crafted.is(buildcraft.factory.BCFactoryItems.DISTILLER.get()),
                    "distiller recipe returned wrong item");
            var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, distiller);
            helper.assertValueEqual(1, drops.size(), "distiller returned wrong drop count");
            helper.assertTrue(drops.getFirst().is(buildcraft.factory.BCFactoryItems.DISTILLER.get()),
                    "distiller returned wrong drop");
            helper.succeed();
        });
    }

    private static void factoryHeatExchanger(GameTestHelper helper) {
        BlockPos endPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos middlePos = helper.absolutePos(new BlockPos(2, 2, 1));
        BlockPos startPos = helper.absolutePos(new BlockPos(3, 2, 1));
        BlockState state = buildcraft.factory.BCFactoryBlocks.HEAT_EXCHANGER.get().defaultBlockState()
                .setValue(buildcraft.factory.block.HeatExchangerBlock.FACING, Direction.NORTH);
        for (BlockPos pos : java.util.List.of(endPos, middlePos, startPos)) {
            helper.getLevel().setBlock(pos, state, Block.UPDATE_ALL);
        }
        var startEntity = (buildcraft.factory.block.entity.HeatExchangerBlockEntity)
                helper.getLevel().getBlockEntity(startPos);
        buildcraft.factory.block.entity.HeatExchangerBlockEntity.tick(
                helper.getLevel(), startPos, helper.getLevel().getBlockState(startPos), startEntity);
        helper.assertTrue(helper.getLevel().getBlockState(startPos).getValue(
                        buildcraft.factory.block.HeatExchangerBlock.PART)
                        == buildcraft.factory.block.HeatExchangerBlock.Part.START,
                "heat exchanger did not form start section");
        helper.assertTrue(helper.getLevel().getBlockState(middlePos).getValue(
                        buildcraft.factory.block.HeatExchangerBlock.PART)
                        == buildcraft.factory.block.HeatExchangerBlock.Part.MIDDLE,
                "heat exchanger did not form middle section");
        helper.assertTrue(helper.getLevel().getBlockState(endPos).getValue(
                        buildcraft.factory.block.HeatExchangerBlock.PART)
                        == buildcraft.factory.block.HeatExchangerBlock.Part.END,
                "heat exchanger did not form end section");
        var hotInput = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, startPos, Direction.DOWN);
        var hotOutput = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, startPos, Direction.EAST);
        var coldInput = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, endPos, Direction.UP);
        helper.assertTrue(hotInput != null && hotOutput != null && coldInput != null,
                "heat exchanger endpoint capabilities missing");
        var coldOil = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                buildcraft.energy.BCEnergyFluids.refineryFluid("oil").heat(0).source().get());
        var seethingOil = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                buildcraft.energy.BCEnergyFluids.refineryFluid("oil").heat(2).source().get());
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(10, hotInput.insert(coldOil, 10, transaction),
                    "heat exchanger rejected heatant input");
            helper.assertValueEqual(10, coldInput.insert(seethingOil, 10, transaction),
                    "heat exchanger rejected coolant input");
            helper.assertValueEqual(0, hotInput.extract(coldOil, 1, transaction),
                    "heat exchanger exposed input extraction");
            helper.assertValueEqual(0, hotOutput.insert(coldOil, 1, transaction),
                    "heat exchanger output accepted insertion");
            transaction.commit();
        }
        for (int tick = 0; tick < 122; tick++) {
            buildcraft.factory.block.entity.HeatExchangerBlockEntity.tick(
                    helper.getLevel(), startPos, helper.getLevel().getBlockState(startPos), startEntity);
        }
        {
            var endEntity = (buildcraft.factory.block.entity.HeatExchangerBlockEntity)
                    helper.getLevel().getBlockEntity(endPos);
            var hot = buildcraft.energy.BCEnergyFluids.refineryFluid("oil").heat(1).source().get();
            helper.assertValueEqual(0, startEntity.inputTank().getAmountAsInt(0),
                    "heat exchanger left heatant input unprocessed");
            helper.assertValueEqual(10, startEntity.outputTank().getAmountAsInt(0),
                    "heat exchanger produced wrong heated amount");
            helper.assertTrue(startEntity.outputTank().getResource(0).value() == hot,
                    "heat exchanger produced wrong heated fluid");
            helper.assertValueEqual(0, endEntity.inputTank().getAmountAsInt(0),
                    "heat exchanger left coolant input unprocessed");
            helper.assertValueEqual(10, endEntity.outputTank().getAmountAsInt(0),
                    "heat exchanger produced wrong cooled amount");
            helper.assertTrue(endEntity.outputTank().getResource(0).value() == hot,
                    "heat exchanger produced wrong cooled fluid");
            java.util.List<BlockPos> longColumn = java.util.List.of(
                    helper.absolutePos(new BlockPos(1, 4, 4)), helper.absolutePos(new BlockPos(2, 4, 4)),
                    helper.absolutePos(new BlockPos(3, 4, 4)), helper.absolutePos(new BlockPos(4, 4, 4)),
                    helper.absolutePos(new BlockPos(5, 4, 4)));
            for (BlockPos columnPos : longColumn) helper.getLevel().setBlock(columnPos, state, Block.UPDATE_ALL);
            var longStart = (buildcraft.factory.block.entity.HeatExchangerBlockEntity)
                    helper.getLevel().getBlockEntity(longColumn.getLast());
            buildcraft.factory.block.entity.HeatExchangerBlockEntity.tick(helper.getLevel(), longColumn.getLast(),
                    helper.getLevel().getBlockState(longColumn.getLast()), longStart);
            var longEnd = (buildcraft.factory.block.entity.HeatExchangerBlockEntity)
                    helper.getLevel().getBlockEntity(longColumn.getFirst());
            try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                helper.assertValueEqual(20, longStart.inputTank().insert(coldOil, 20, transaction),
                        "five-block exchanger rejected heatant");
                helper.assertValueEqual(20, longEnd.inputTank().insert(seethingOil, 20, transaction),
                        "five-block exchanger rejected coolant");
                transaction.commit();
            }
            for (int tick = 0; tick < 120; tick++) {
                buildcraft.factory.block.entity.HeatExchangerBlockEntity.tick(helper.getLevel(),
                        longColumn.getLast(), helper.getLevel().getBlockState(longColumn.getLast()), longStart);
            }
            helper.assertValueEqual(20, longStart.outputTank().getAmountAsInt(0),
                    "five-block exchanger did not apply 20 mB/t throughput");
            helper.assertValueEqual(20, longEnd.outputTank().getAmountAsInt(0),
                    "five-block exchanger cooled wrong throughput");
            var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                    new ItemStack(Items.IRON_INGOT), new ItemStack(BCCoreItems.GEAR_IRON.get()),
                    new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.GLASS), new ItemStack(Items.GLASS), new ItemStack(Items.GLASS),
                    new ItemStack(Items.IRON_INGOT), new ItemStack(BCCoreItems.GEAR_IRON.get()),
                    new ItemStack(Items.IRON_INGOT)
            ));
            ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                    net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                    .orElseThrow().value().assemble(recipeInput);
            helper.assertTrue(crafted.is(buildcraft.factory.BCFactoryItems.HEAT_EXCHANGER.get()),
                    "heat exchanger recipe returned wrong item");
            var drops = Block.getDrops(helper.getLevel().getBlockState(middlePos), helper.getLevel(), middlePos,
                    helper.getLevel().getBlockEntity(middlePos));
            helper.assertValueEqual(1, drops.size(), "heat exchanger returned wrong drop count");
            helper.assertTrue(drops.getFirst().is(buildcraft.factory.BCFactoryItems.HEAT_EXCHANGER.get()),
                    "heat exchanger returned wrong drop");
            helper.succeed();
        }
    }

    private static void factoryWaterGel(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockState initial = buildcraft.factory.BCFactoryBlocks.WATER_GEL.get().defaultBlockState();
        helper.getLevel().setBlock(center, initial, Block.UPDATE_ALL);
        java.util.List<BlockPos> sources = java.util.List.of(
            center.north(), center.south(), center.east()
        );
        for (BlockPos source : sources) {
            helper.getLevel().setBlock(source, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        }
        buildcraft.factory.block.WaterGelBlock.advance(
            initial, helper.getLevel(), center, helper.getLevel().getRandom()
        );
        for (BlockPos source : sources) {
            helper.assertTrue(helper.getLevel().getBlockState(source).is(buildcraft.factory.BCFactoryBlocks.WATER_GEL.get()),
                "water gel did not replace a connected source");
            helper.assertValueEqual(helper.getLevel().getBlockState(source).getValue(
                buildcraft.factory.block.WaterGelBlock.STAGE),
                buildcraft.factory.block.WaterGelBlock.Stage.SPREAD_1, "spread stage");
        }

        BlockState gelling = initial.setValue(buildcraft.factory.block.WaterGelBlock.STAGE,
            buildcraft.factory.block.WaterGelBlock.Stage.GELLING_0);
        helper.getLevel().setBlock(center, gelling, Block.UPDATE_ALL);
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = center.relative(direction);
            if (!sources.contains(adjacent)) helper.getLevel().setBlock(adjacent, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (BlockPos source : sources) helper.getLevel().setBlock(source, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        buildcraft.factory.block.WaterGelBlock.advance(gelling, helper.getLevel(), center, helper.getLevel().getRandom());
        BlockState gellingOne = helper.getLevel().getBlockState(center);
        buildcraft.factory.block.WaterGelBlock.advance(gellingOne, helper.getLevel(), center, helper.getLevel().getRandom());
        helper.assertValueEqual(helper.getLevel().getBlockState(center).getValue(
            buildcraft.factory.block.WaterGelBlock.STAGE),
            buildcraft.factory.block.WaterGelBlock.Stage.GEL, "hardened stage");

        var drops = Block.getDrops(helper.getLevel().getBlockState(center), helper.getLevel(), center, null);
        helper.assertValueEqual(1, drops.size(), "water gel returned wrong drop count");
        helper.assertTrue(drops.getFirst().is(buildcraft.factory.BCFactoryItems.GEL.get()), "water gel returned wrong drop");

        var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 2, java.util.List.of(
            new ItemStack(buildcraft.factory.BCFactoryItems.GEL.get()), new ItemStack(Items.BUCKET)
        ));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
            net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel()
        ).orElseThrow().value().assemble(input);
        helper.assertTrue(crafted.is(Items.WATER_BUCKET), "gel recipe returned wrong item");
        helper.succeed();
    }

    private static void factoryAutoWorkbench(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlock(pos, buildcraft.factory.BCFactoryBlocks.AUTO_WORKBENCH.get()
            .defaultBlockState(), Block.UPDATE_ALL);
        var workbench = (buildcraft.factory.block.entity.AutoWorkbenchBlockEntity)
            helper.getLevel().getBlockEntity(pos);
        var planks = net.neoforged.neoforge.transfer.item.ItemResource.of(Items.OAK_PLANKS);
        workbench.blueprint().set(0, planks, 1);
        workbench.blueprint().set(3, planks, 1);

        var handler = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, pos, Direction.NORTH
        );
        helper.assertTrue(handler != null, "auto workbench item capability missing");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(2, handler.insert(0, planks, 2, transaction),
                "auto workbench rejected blueprint material");
            helper.assertValueEqual(0, handler.insert(1,
                net.neoforged.neoforge.transfer.item.ItemResource.of(Items.COBBLESTONE), 1, transaction),
                "auto workbench accepted unrelated material");
            transaction.commit();
        }
        for (int tick = 0; tick < 199; tick++) {
            buildcraft.factory.block.entity.AutoWorkbenchBlockEntity.tick(
                helper.getLevel(), pos, helper.getLevel().getBlockState(pos), workbench
            );
        }
        helper.assertValueEqual(0, workbench.result().getAmountAsInt(0),
            "auto workbench crafted before 40 MJ cycle");
        buildcraft.factory.block.entity.AutoWorkbenchBlockEntity.tick(
            helper.getLevel(), pos, helper.getLevel().getBlockState(pos), workbench
        );
        helper.assertTrue(workbench.result().getResource(0).value() == Items.STICK,
            "auto workbench produced wrong recipe output");
        helper.assertValueEqual(4, workbench.result().getAmountAsInt(0),
            "auto workbench produced wrong output count");
        helper.assertValueEqual(0L, workbench.storedPower(), "auto workbench did not debit 40 MJ");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(0, handler.extract(0, planks, 1, transaction),
                "auto workbench allowed material extraction");
            helper.assertValueEqual(4, handler.extract(9,
                net.neoforged.neoforge.transfer.item.ItemResource.of(Items.STICK), 4, transaction),
                "auto workbench rejected output extraction");
            transaction.commit();
        }
        for (int slot = 0; slot < 9; slot++) workbench.blueprint().set(slot,
            net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        var honey = net.neoforged.neoforge.transfer.item.ItemResource.of(Items.HONEY_BOTTLE);
        for (int slot : new int[] {0, 1, 3, 4}) workbench.blueprint().set(slot, honey, 1);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            for (int slot = 0; slot < 4; slot++) {
                helper.assertValueEqual(1, handler.insert(slot, honey, 1, transaction),
                    "auto workbench rejected container recipe material");
            }
            transaction.commit();
        }
        for (int tick = 0; tick < 200; tick++) {
            buildcraft.factory.block.entity.AutoWorkbenchBlockEntity.tick(
                helper.getLevel(), pos, helper.getLevel().getBlockState(pos), workbench
            );
        }
        helper.assertTrue(workbench.result().getResource(0).value() == Items.HONEY_BLOCK,
            "auto workbench produced wrong container recipe output");
        int bottles = 0;
        for (int slot = 0; slot < workbench.materials().size(); slot++) {
            if (workbench.materials().getResource(slot).value() == Items.GLASS_BOTTLE) {
                bottles += workbench.materials().getAmountAsInt(slot);
            }
        }
        helper.assertValueEqual(4, bottles, "auto workbench did not retain crafting remainders");

        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 1, java.util.List.of(
            new ItemStack(BCCoreItems.GEAR_STONE.get()), new ItemStack(Items.CRAFTING_TABLE),
            new ItemStack(BCCoreItems.GEAR_STONE.get())
        ));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
            net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel()
        ).orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.factory.BCFactoryItems.AUTO_WORKBENCH.get()),
            "auto workbench recipe returned wrong item");
        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, workbench);
        helper.assertValueEqual(1, drops.size(), "auto workbench returned wrong drop count");
        helper.assertTrue(drops.getFirst().is(buildcraft.factory.BCFactoryItems.AUTO_WORKBENCH.get()),
            "auto workbench returned wrong drop");
        helper.succeed();
    }

    private static void buildersSnapshotData(GameTestHelper helper) {
        BlockPos machine = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos min = machine.offset(2, 0, 1);
        BlockPos max = min.offset(1, 1, 1);
        helper.getLevel().setBlock(min, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(min.offset(1, 0, 0), Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        helper.getLevel().setBlock(max, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);

        var blueprint = buildcraft.builders.snapshot.SnapshotData.capture(helper.getLevel(), min, max,
                buildcraft.builders.snapshot.SnapshotKind.BLUEPRINT, Direction.NORTH, machine, "GameTest");
        helper.assertTrue(blueprint.valid(), "captured Blueprint was invalid");
        helper.assertValueEqual(blueprint.size(), new BlockPos(2, 2, 2), "Blueprint size");
        helper.assertValueEqual(blueprint.offset(), new BlockPos(2, 0, 1), "Blueprint machine offset");
        helper.assertTrue(blueprint.stateAt(BlockPos.ZERO).is(Blocks.STONE)
                        && blueprint.stateAt(new BlockPos(1, 0, 0)).is(Blocks.OAK_STAIRS)
                        && blueprint.stateAt(new BlockPos(1, 0, 0))
                        .getValue(net.minecraft.world.level.block.StairBlock.FACING) == Direction.EAST
                        && blueprint.stateAt(new BlockPos(1, 1, 1)).is(Blocks.GLASS),
                "Blueprint did not preserve its block-state palette");
        helper.assertValueEqual(blueprint.worldPosition(machine, new BlockPos(1, 0, 0),
                        net.minecraft.world.level.block.Rotation.CLOCKWISE_90), machine.offset(-1, 0, 3),
                "Blueprint clockwise placement transform");

        var encoded = buildcraft.builders.snapshot.SnapshotData.CODEC.encodeStart(
                com.mojang.serialization.JsonOps.INSTANCE, blueprint).getOrThrow();
        var decoded = buildcraft.builders.snapshot.SnapshotData.CODEC.parse(
                com.mojang.serialization.JsonOps.INSTANCE, (com.google.gson.JsonElement) encoded).getOrThrow();
        helper.assertTrue(decoded.equals(blueprint), "Blueprint codec round-trip changed data");
        ItemStack blueprintStack = new ItemStack(buildcraft.builders.BCBuildersItems.BLUEPRINT.get());
        blueprintStack.set(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get(), blueprint);
        helper.assertValueEqual(blueprintStack.get(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get()),
                blueprint, "Blueprint item component");

        var template = buildcraft.builders.snapshot.SnapshotData.capture(helper.getLevel(), min, max,
                buildcraft.builders.snapshot.SnapshotKind.TEMPLATE, Direction.NORTH, machine, "Template");
        helper.assertTrue(template.valid() && template.palette().stream().allMatch(
                        state -> state.isAir() || state.is(Blocks.STONE)),
                "Template captured block identity instead of occupancy");

        var blueprintRecipe = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER),
                new ItemStack(Items.PAPER), new ItemStack(Items.LAPIS_LAZULI), new ItemStack(Items.PAPER),
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER)));
        var craftedBlueprint = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, blueprintRecipe, helper.getLevel())
                .orElseThrow().value().assemble(blueprintRecipe);
        helper.assertTrue(craftedBlueprint.is(buildcraft.builders.BCBuildersItems.BLUEPRINT.get()),
                "Blueprint recipe output");
        var templateRecipe = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER),
                new ItemStack(Items.PAPER), new ItemStack(Items.INK_SAC), new ItemStack(Items.PAPER),
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER)));
        var craftedTemplate = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, templateRecipe, helper.getLevel())
                .orElseThrow().value().assemble(templateRecipe);
        helper.assertTrue(craftedTemplate.is(buildcraft.builders.BCBuildersItems.TEMPLATE.get()),
                "Template recipe output");
        helper.succeed();
    }

    private static void buildersArchitectTable(GameTestHelper helper) {
        BlockPos tablePos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos min = tablePos.east();
        BlockPos max = min.offset(2, 1, 1);
        helper.getLevel().setBlock(tablePos,
                buildcraft.builders.BCBuildersBlocks.ARCHITECT_TABLE.get().defaultBlockState()
                        .setValue(buildcraft.builders.block.ArchitectTableBlock.FACING, Direction.WEST),
                Block.UPDATE_ALL);
        var table = (buildcraft.builders.block.entity.ArchitectTableBlockEntity)
                helper.getLevel().getBlockEntity(tablePos);
        helper.assertTrue(table.configureArea(min, max), "Architect rejected valid bounds");
        table.setBlueprintName("West Wing");
        table.toggleRotate();
        table.toggleExcavate();
        table.toggleAllowCreative();
        helper.getLevel().setBlock(min, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(min.offset(1, 0, 0), Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        helper.getLevel().setBlock(min.offset(2, 0, 0), Blocks.COMMAND_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(max, Blocks.GLASS.defaultBlockState(), Block.UPDATE_ALL);
        table.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(
                buildcraft.builders.BCBuildersItems.BLUEPRINT.get()), 1);

        buildcraft.builders.block.entity.ArchitectTableBlockEntity.tick(helper.getLevel(), tablePos,
                helper.getLevel().getBlockState(tablePos), table);
        helper.assertValueEqual(table.cursor(), 10, "Architect Blueprint scan rate");
        helper.assertValueEqual(table.inventory().getAmountAsLong(1), 0L,
                "Architect emitted Blueprint before scanning complete volume");
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(tablePos, table.getBlockState(),
                table.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof buildcraft.builders.block.entity.ArchitectTableBlockEntity,
                "Architect block entity did not reload");
        var restored = (buildcraft.builders.block.entity.ArchitectTableBlockEntity) loaded;
        helper.assertValueEqual(restored.cursor(), 10, "reloaded Architect lost scan cursor");
        helper.assertValueEqual(restored.blueprintName(), "West Wing", "Architect did not restore Blueprint name");
        helper.assertTrue(!restored.rotate() && !restored.excavate() && restored.allowCreative(),
                "Architect did not restore capture configuration");
        helper.getLevel().setBlockEntity(restored);
        buildcraft.builders.block.entity.ArchitectTableBlockEntity.tick(helper.getLevel(), tablePos,
                helper.getLevel().getBlockState(tablePos), restored);
        helper.assertValueEqual(restored.inventory().getAmountAsLong(0), 0L,
                "Architect did not consume one blank Blueprint");
        ItemStack usedBlueprint = restored.inventory().getResource(1).toStack(1);
        var blueprint = usedBlueprint.get(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get());
        helper.assertTrue(usedBlueprint.is(buildcraft.builders.BCBuildersItems.BLUEPRINT.get())
                        && blueprint != null && blueprint.valid()
                        && blueprint.kind() == buildcraft.builders.snapshot.SnapshotKind.BLUEPRINT
                        && usedBlueprint.getMaxStackSize() == 1
                        && usedBlueprint.has(net.minecraft.core.component.DataComponents.ITEM_MODEL),
                "Architect did not emit a used Blueprint");
        helper.assertValueEqual(blueprint.size(), new BlockPos(3, 2, 2), "Architect Blueprint size");
        helper.assertValueEqual(blueprint.facing(), Direction.WEST, "Architect Blueprint facing");
        helper.assertTrue(blueprint.name().equals("West Wing") && !blueprint.rotate() && !blueprint.excavate()
                        && blueprint.allowCreative() && blueprint.creativeOnly(),
                "Architect did not write Blueprint metadata");
        helper.assertTrue(blueprint.stateAt(new BlockPos(2, 0, 0)).is(Blocks.COMMAND_BLOCK),
                "Allow Creative Architect omitted a creative-only block");
        helper.assertTrue(blueprint.stateAt(new BlockPos(1, 0, 0)).is(Blocks.OAK_STAIRS)
                        && blueprint.stateAt(new BlockPos(1, 0, 0))
                        .getValue(net.minecraft.world.level.block.StairBlock.FACING) == Direction.SOUTH,
                "Architect Blueprint lost directional block state");

        restored.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        restored.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(
                buildcraft.builders.BCBuildersItems.TEMPLATE.get()), 1);
        buildcraft.builders.block.entity.ArchitectTableBlockEntity.tick(helper.getLevel(), tablePos,
                helper.getLevel().getBlockState(tablePos), restored);
        ItemStack usedTemplate = restored.inventory().getResource(1).toStack(1);
        var template = usedTemplate.get(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get());
        helper.assertTrue(template != null && template.kind() == buildcraft.builders.snapshot.SnapshotKind.TEMPLATE
                        && template.palette().stream().allMatch(state -> state.isAir() || state.is(Blocks.STONE)),
                "Architect Template did not reduce capture to occupancy");

        restored.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        restored.toggleAllowCreative();
        restored.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(
                buildcraft.builders.BCBuildersItems.BLUEPRINT.get()), 1);
        for (int tick = 0; tick < 2; tick++) buildcraft.builders.block.entity.ArchitectTableBlockEntity.tick(
                helper.getLevel(), tablePos, helper.getLevel().getBlockState(tablePos), restored);
        var restricted = restored.inventory().getResource(1).toStack(1)
                .get(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get());
        helper.assertTrue(restricted != null && !restricted.creativeOnly()
                        && restricted.stateAt(new BlockPos(2, 0, 0)).isAir(),
                "Architect captured a creative-only block while Allow Creative was disabled");

        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.BLACK_DYE), new ItemStack(buildcraft.core.BCCoreItems.MARKER_VOLUME.get()), new ItemStack(Items.BLACK_DYE),
                new ItemStack(Items.YELLOW_DYE), new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.YELLOW_DYE),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_DIAMOND.get()), new ItemStack(buildcraft.builders.BCBuildersItems.BLUEPRINT.get()),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_DIAMOND.get())));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.builders.BCBuildersItems.ARCHITECT_TABLE.get()),
                "Architect recipe output");
        var drops = Block.getDrops(helper.getLevel().getBlockState(tablePos), helper.getLevel(), tablePos,
                restored, null, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(buildcraft.builders.BCBuildersItems.ARCHITECT_TABLE.get()),
                "Architect loot output");
        helper.succeed();
    }

    private static void buildersBuilder(GameTestHelper helper) {
        BlockPos builderPos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(builderPos, buildcraft.builders.BCBuildersBlocks.BUILDER.get().defaultBlockState(),
                Block.UPDATE_ALL);
        var builder = (buildcraft.builders.block.entity.BuilderBlockEntity)
                helper.getLevel().getBlockEntity(builderPos);
        var palette = java.util.List.of(Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState(),
                Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.EAST),
                Blocks.GLASS.defaultBlockState());
        var blueprint = new buildcraft.builders.snapshot.SnapshotData(
                buildcraft.builders.snapshot.SnapshotKind.BLUEPRINT, new BlockPos(2, 1, 2), Direction.NORTH,
                new BlockPos(2, 0, 0), palette, java.util.List.of(1, 2, 0, 3), "Builder GameTest",
                true, false, false);
        ItemStack blueprintStack = new ItemStack(buildcraft.builders.BCBuildersItems.BLUEPRINT.get());
        blueprintStack.set(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get(), blueprint);
        builder.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(blueprintStack), 1);
        builder.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.STONE), 1);
        builder.inventory().set(2, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.OAK_STAIRS), 1);
        builder.inventory().set(3, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.GLASS), 1);
        BlockPos first = builderPos.offset(2, 0, 0);
        BlockPos stairs = builderPos.offset(3, 0, 0);
        BlockPos glass = builderPos.offset(3, 0, 1);
        helper.getLevel().setBlock(first, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        long required = builderCost(builderPos, first) * 2
                + builderCost(builderPos, stairs) + builderCost(builderPos, glass);
        helper.assertValueEqual(0L, builder.mjReceiver().receivePower(required, false),
                "Builder rejected exact construction MJ");

        for (int tick = 0; tick < 3; tick++) buildcraft.builders.block.entity.BuilderBlockEntity.tick(
                helper.getLevel(), builderPos, helper.getLevel().getBlockState(builderPos), builder);
        helper.assertTrue(helper.getLevel().getBlockState(first).is(Blocks.DIRT)
                        && builder.storedMj() == builderCost(builderPos, first) * 2,
                "non-excavating Builder changed obstruction or failed to build unobstructed targets");
        builder.setCanExcavate(true);
        buildcraft.builders.block.entity.BuilderBlockEntity.tick(helper.getLevel(), builderPos,
                helper.getLevel().getBlockState(builderPos), builder);
        helper.assertTrue(helper.getLevel().getBlockState(first).isAir(),
                "excavating Builder did not clear obstruction first");
        buildcraft.builders.block.entity.BuilderBlockEntity.tick(helper.getLevel(), builderPos,
                helper.getLevel().getBlockState(builderPos), builder);
        helper.assertTrue(helper.getLevel().getBlockState(first).is(Blocks.STONE),
                "Builder did not place first Blueprint block");

        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(builderPos, builder.getBlockState(),
                builder.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof buildcraft.builders.block.entity.BuilderBlockEntity,
                "Builder block entity did not reload");
        var restored = (buildcraft.builders.block.entity.BuilderBlockEntity) loaded;
        helper.assertValueEqual(restored.cursor(), 1, "reloaded Builder lost construction cursor");
        helper.getLevel().setBlockEntity(restored);
        for (int tick = 0; tick < 8; tick++) buildcraft.builders.block.entity.BuilderBlockEntity.tick(
                helper.getLevel(), builderPos, helper.getLevel().getBlockState(builderPos), restored);
        helper.assertTrue(restored.finished() && helper.getLevel().getBlockState(stairs).is(Blocks.OAK_STAIRS)
                        && helper.getLevel().getBlockState(stairs)
                        .getValue(net.minecraft.world.level.block.StairBlock.FACING) == Direction.EAST
                        && helper.getLevel().getBlockState(glass).is(Blocks.GLASS),
                "Builder did not complete exact Blueprint states");
        helper.assertValueEqual(restored.storedMj(), 0L, "Builder construction MJ accounting");

        var template = new buildcraft.builders.snapshot.SnapshotData(
                buildcraft.builders.snapshot.SnapshotKind.TEMPLATE, new BlockPos(2, 1, 1), Direction.NORTH,
                new BlockPos(2, 0, 3), java.util.List.of(Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState()),
                java.util.List.of(1, 0), "Template GameTest");
        ItemStack templateStack = new ItemStack(buildcraft.builders.BCBuildersItems.TEMPLATE.get());
        templateStack.set(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get(), template);
        restored.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(templateStack), 1);
        restored.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.COBBLESTONE), 1);
        BlockPos templateTarget = builderPos.offset(2, 0, 3);
        long templateCost = builderCost(builderPos, templateTarget);
        helper.assertValueEqual(0L, restored.mjReceiver().receivePower(templateCost, false),
                "Builder rejected Template MJ");
        for (int tick = 0; tick < 5; tick++) buildcraft.builders.block.entity.BuilderBlockEntity.tick(
                helper.getLevel(), builderPos, helper.getLevel().getBlockState(builderPos), restored);
        helper.assertTrue(helper.getLevel().getBlockState(templateTarget).is(Blocks.COBBLESTONE)
                        && helper.getLevel().getBlockState(templateTarget.east()).isAir(),
                "Template Builder did not use arbitrary block or preserve empty mask");

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new buildcraft.builders.menu.BuilderMenu(47, player.getInventory(), builderPos);
        helper.assertTrue(menu.clickMenuButton(player, 10) && menu.clickMenuButton(player, 11)
                        && menu.clickMenuButton(player, 2), "Builder menu rejected controls");
        helper.assertTrue(restored.rotation() == net.minecraft.world.level.block.Rotation.CLOCKWISE_90
                        && !restored.canExcavate()
                        && restored.controlMode() == buildcraft.api.core.IControllable.ControlMode.LOOP,
                "Builder menu selected wrong controls");
        var reloaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(builderPos, restored.getBlockState(),
                restored.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(reloaded instanceof buildcraft.builders.block.entity.BuilderBlockEntity saved
                        && saved.rotation() == net.minecraft.world.level.block.Rotation.CLOCKWISE_90
                        && !saved.canExcavate()
                        && saved.controlMode() == buildcraft.api.core.IControllable.ControlMode.LOOP,
                "reloaded Builder lost controls");

        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.BLACK_DYE), new ItemStack(buildcraft.core.BCCoreItems.MARKER_VOLUME.get()), new ItemStack(Items.BLACK_DYE),
                new ItemStack(Items.YELLOW_DYE), new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.YELLOW_DYE),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_DIAMOND.get()), new ItemStack(Items.CHEST),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_DIAMOND.get())));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.builders.BCBuildersItems.BUILDER.get()), "Builder recipe output");
        helper.succeed();
    }

    private static long builderCost(BlockPos builder, BlockPos target) {
        return (long) ((Math.sqrt(target.distSqr(builder)) + 10) * MjAPI.MJ);
    }

    private static void buildersReplacer(GameTestHelper helper) {
        BlockPos replacerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(replacerPos,
                buildcraft.builders.BCBuildersBlocks.REPLACER.get().defaultBlockState(), Block.UPDATE_ALL);
        var replacer = (buildcraft.builders.block.entity.ReplacerBlockEntity)
                helper.getLevel().getBlockEntity(replacerPos);
        var eastStairs = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.EAST);
        var westStairs = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.WEST);
        var replacement = Blocks.SPRUCE_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH);
        var blueprint = new buildcraft.builders.snapshot.SnapshotData(
                buildcraft.builders.snapshot.SnapshotKind.BLUEPRINT, new BlockPos(3, 1, 1), Direction.NORTH,
                new BlockPos(2, 0, 0), java.util.List.of(Blocks.AIR.defaultBlockState(), eastStairs, westStairs),
                java.util.List.of(1, 2, 1), "Replacer GameTest");
        ItemStack blueprintStack = new ItemStack(buildcraft.builders.BCBuildersItems.BLUEPRINT.get());
        blueprintStack.set(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get(), blueprint);
        ItemStack from = new ItemStack(buildcraft.builders.BCBuildersItems.SINGLE_SCHEMATIC.get());
        from.set(buildcraft.builders.BCBuildersDataComponents.SCHEMATIC_STATE.get(), eastStairs);
        ItemStack to = new ItemStack(buildcraft.builders.BCBuildersItems.SINGLE_SCHEMATIC.get());
        to.set(buildcraft.builders.BCBuildersDataComponents.SCHEMATIC_STATE.get(), replacement);
        replacer.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(blueprintStack), 1);
        replacer.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.of(from), 1);
        replacer.inventory().set(2, net.neoforged.neoforge.transfer.item.ItemResource.of(to), 1);

        buildcraft.builders.block.entity.ReplacerBlockEntity.tick(helper.getLevel(), replacerPos,
                helper.getLevel().getBlockState(replacerPos), replacer);
        ItemStack replacedStack = replacer.inventory().getResource(0).toStack(1);
        var replaced = replacedStack.get(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get());
        helper.assertTrue(replaced != null && replaced.stateAt(BlockPos.ZERO).equals(replacement)
                        && replaced.stateAt(new BlockPos(1, 0, 0)).equals(westStairs)
                        && replaced.stateAt(new BlockPos(2, 0, 0)).equals(replacement),
                "Replacer did not replace only exact palette states");
        helper.assertTrue(replacedStack.is(buildcraft.builders.BCBuildersItems.BLUEPRINT.get())
                        && replaced.size().equals(blueprint.size()) && replaced.offset().equals(blueprint.offset())
                        && replaced.facing() == blueprint.facing() && replaced.name().equals(blueprint.name())
                        && replacer.inventory().getAmountAsLong(1) == 0
                        && replacer.inventory().getAmountAsLong(2) == 0,
                "Replacer lost Blueprint metadata or did not consume matched schematics");

        ItemStack noMatch = new ItemStack(buildcraft.builders.BCBuildersItems.SINGLE_SCHEMATIC.get());
        noMatch.set(buildcraft.builders.BCBuildersDataComponents.SCHEMATIC_STATE.get(), Blocks.DIRT.defaultBlockState());
        replacer.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.of(noMatch), 1);
        replacer.inventory().set(2, net.neoforged.neoforge.transfer.item.ItemResource.of(to), 1);
        buildcraft.builders.block.entity.ReplacerBlockEntity.tick(helper.getLevel(), replacerPos,
                helper.getLevel().getBlockState(replacerPos), replacer);
        helper.assertTrue(replacer.inventory().getAmountAsLong(1) == 1
                        && replacer.inventory().getAmountAsLong(2) == 1,
                "Replacer consumed schematics without a matching palette state");

        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(replacerPos, replacer.getBlockState(),
                replacer.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof buildcraft.builders.block.entity.ReplacerBlockEntity restored
                        && restored.inventory().getResource(0).toStack(1)
                        .has(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get())
                        && restored.inventory().getResource(1).toStack(1)
                        .get(buildcraft.builders.BCBuildersDataComponents.SCHEMATIC_STATE.get()).is(Blocks.DIRT),
                "Replacer inventory or typed schematic state did not reload");

        var schematicRecipe = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                new ItemStack(Items.LAPIS_LAZULI), new ItemStack(Items.LAPIS_LAZULI)));
        ItemStack craftedSchematic = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, schematicRecipe, helper.getLevel())
                .orElseThrow().value().assemble(schematicRecipe);
        helper.assertTrue(craftedSchematic.is(buildcraft.builders.BCBuildersItems.SINGLE_SCHEMATIC.get()),
                "Single Schematic recipe output");
        var replacerRecipe = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.BLACK_DYE), new ItemStack(buildcraft.core.BCCoreItems.MARKER_VOLUME.get()), new ItemStack(Items.BLACK_DYE),
                new ItemStack(Items.RED_DYE), new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.RED_DYE),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_GOLD.get()), new ItemStack(Items.CHEST),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_GOLD.get())));
        ItemStack craftedReplacer = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, replacerRecipe, helper.getLevel())
                .orElseThrow().value().assemble(replacerRecipe);
        helper.assertTrue(craftedReplacer.is(buildcraft.builders.BCBuildersItems.REPLACER.get()),
                "Replacer recipe output");
        var drops = Block.getDrops(helper.getLevel().getBlockState(replacerPos), helper.getLevel(), replacerPos,
                replacer, null, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(buildcraft.builders.BCBuildersItems.REPLACER.get()),
                "Replacer loot output");
        helper.succeed();
    }

    private static void buildersQuarry(GameTestHelper helper) {
        BlockPos quarryPos = helper.absolutePos(new BlockPos(1, 3, 1));
        BlockPos min = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos max = helper.absolutePos(new BlockPos(5, 6, 5));
        helper.getLevel().setBlock(quarryPos, buildcraft.builders.BCBuildersBlocks.QUARRY.get().defaultBlockState()
                .setValue(buildcraft.builders.block.QuarryBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        var quarry = (buildcraft.builders.block.entity.QuarryBlockEntity)
                helper.getLevel().getBlockEntity(quarryPos);
        helper.assertTrue(quarry.configureArea(min, max), "Quarry rejected valid 3x5x3 area");
        BlockPos stone = new BlockPos(min.getX() + 1, max.getY() - 1, min.getZ() + 1);
        BlockPos dirt = stone.below();
        helper.getLevel().setBlock(stone, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(dirt, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(dirt.below(), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
        long frameEnergy = 28 * buildcraft.builders.block.entity.QuarryBlockEntity.FRAME_COST;
        long stoneEnergy = 80 * MjAPI.MJ;
        long dirtEnergy = 48 * MjAPI.MJ;
        long required = frameEnergy + stoneEnergy + dirtEnergy;
        helper.assertValueEqual(0L, quarry.mjReceiver().receivePower(required, false),
                "Quarry rejected exact frame and mining MJ");

        quarry.setControlMode(buildcraft.api.core.IControllable.ControlMode.OFF);
        buildcraft.builders.block.entity.QuarryBlockEntity.tick(helper.getLevel(), quarryPos,
                helper.getLevel().getBlockState(quarryPos), quarry);
        helper.assertValueEqual(quarry.frameCursor(), 0, "disabled Quarry advanced frame construction");
        quarry.setControlMode(buildcraft.api.core.IControllable.ControlMode.ON);
        for (int tick = 0; tick < 10; tick++) buildcraft.builders.block.entity.QuarryBlockEntity.tick(
                helper.getLevel(), quarryPos, helper.getLevel().getBlockState(quarryPos), quarry);
        java.util.Set<Long> expectedChunks = new java.util.HashSet<>();
        expectedChunks.add(net.minecraft.world.level.ChunkPos.pack(quarryPos.getX() >> 4, quarryPos.getZ() >> 4));
        for (int chunkX = min.getX() >> 4; chunkX <= max.getX() >> 4; chunkX++) {
            for (int chunkZ = min.getZ() >> 4; chunkZ <= max.getZ() >> 4; chunkZ++)
                expectedChunks.add(net.minecraft.world.level.ChunkPos.pack(chunkX, chunkZ));
        }
        helper.assertValueEqual(quarry.forcedChunkCount(), expectedChunks.size(),
                "Quarry did not retain its machine and work-area chunks");
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(quarryPos, quarry.getBlockState(),
                quarry.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof buildcraft.builders.block.entity.QuarryBlockEntity,
                "Quarry block entity did not reload");
        var restored = (buildcraft.builders.block.entity.QuarryBlockEntity) loaded;
        helper.assertValueEqual(restored.frameCursor(), quarry.frameCursor(), "reloaded Quarry lost frame cursor");
        helper.assertValueEqual(restored.storedMj(), quarry.storedMj(), "reloaded Quarry lost stored MJ");
        helper.assertValueEqual(restored.areaMin(), min, "reloaded Quarry lost area minimum");
        helper.assertValueEqual(restored.areaMax(), max, "reloaded Quarry lost area maximum");
        helper.assertTrue(restored.head() != null, "reloaded Quarry lost mechanical-arm position");
        helper.getLevel().setBlockEntity(restored);

        for (int tick = 0; tick < 80 && restored.stage()
                != buildcraft.builders.block.entity.QuarryBlockEntity.Stage.DONE; tick++) {
            buildcraft.builders.block.entity.QuarryBlockEntity.tick(helper.getLevel(), quarryPos,
                    helper.getLevel().getBlockState(quarryPos), restored);
        }
        long frames = BlockPos.betweenClosedStream(min, max)
                .filter(pos -> helper.getLevel().getBlockState(pos).is(buildcraft.builders.BCBuildersBlocks.FRAME.get()))
                .count();
        helper.assertValueEqual(frames, 28L, "Quarry frame edge count");
        BlockState corner = helper.getLevel().getBlockState(min);
        helper.assertTrue(corner.getValue(buildcraft.builders.block.FrameBlock.UP)
                        && corner.getValue(buildcraft.builders.block.FrameBlock.SOUTH)
                        && corner.getValue(buildcraft.builders.block.FrameBlock.EAST),
                "Quarry frame corner did not connect on all three axes");
        helper.assertTrue(restored.stage() == buildcraft.builders.block.entity.QuarryBlockEntity.Stage.DONE
                        && helper.getLevel().getBlockState(stone).isAir()
                        && helper.getLevel().getBlockState(dirt).isAir(),
                "Quarry did not finish its interior columns");
        int stoneDrops = 0;
        int dirtDrops = 0;
        for (int slot = 0; slot < restored.internalDrops().size(); slot++) {
            if (restored.internalDrops().getResource(slot).is(Items.COBBLESTONE))
                stoneDrops += restored.internalDrops().getAmountAsInt(slot);
            if (restored.internalDrops().getResource(slot).is(Items.DIRT))
                dirtDrops += restored.internalDrops().getAmountAsInt(slot);
        }
        helper.assertTrue(stoneDrops == 1 && dirtDrops == 1, "Quarry did not retain exact mined drops");
        helper.assertValueEqual(restored.storedMj(), 0L, "Quarry frame/mining MJ accounting");
        helper.assertTrue(!restored.hasWork(), "completed Quarry still reports work");

        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_IRON.get()), new ItemStack(Items.REDSTONE),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_IRON.get()),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_GOLD.get()), new ItemStack(buildcraft.core.BCCoreItems.GEAR_IRON.get()),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_GOLD.get()),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_DIAMOND.get()), new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_DIAMOND.get())));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.builders.BCBuildersItems.QUARRY.get()), "Quarry recipe output");
        var drops = Block.getDrops(helper.getLevel().getBlockState(quarryPos), helper.getLevel(), quarryPos,
                restored, null, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(buildcraft.builders.BCBuildersItems.QUARRY.get()),
                "Quarry loot output");
        restored.destroy();
        helper.assertValueEqual(restored.forcedChunkCount(), 0, "removed Quarry retained chunk tickets");
        helper.assertTrue(BlockPos.betweenClosedStream(min, max).noneMatch(pos ->
                helper.getLevel().getBlockState(pos).is(buildcraft.builders.BCBuildersBlocks.FRAME.get())),
                "Quarry did not remove its generated frame");
        helper.getLevel().removeBlock(quarryPos, false);
        helper.succeed();
    }

    private static void buildersBlueprintLibrary(GameTestHelper helper) {
        BlockPos libraryPos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(libraryPos,
                buildcraft.builders.BCBuildersBlocks.BLUEPRINT_LIBRARY.get().defaultBlockState(), Block.UPDATE_ALL);
        var library = (buildcraft.builders.block.entity.BlueprintLibraryBlockEntity)
                helper.getLevel().getBlockEntity(libraryPos);
        int baseline = buildcraft.builders.library.BlueprintLibrarySavedData.get(helper.getLevel()).entries().size();
        String name = "Library GameTest " + libraryPos.asLong();
        var snapshot = new buildcraft.builders.snapshot.SnapshotData(
                buildcraft.builders.snapshot.SnapshotKind.BLUEPRINT, new BlockPos(2, 1, 1), Direction.WEST,
                new BlockPos(3, 0, -1), java.util.List.of(Blocks.AIR.defaultBlockState(),
                Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH)),
                java.util.List.of(1, 0), name);
        ItemStack used = new ItemStack(buildcraft.builders.BCBuildersItems.BLUEPRINT.get());
        used.set(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get(), snapshot);
        library.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(used), 1);
        for (int tick = 0; tick < 50; tick++) buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.tick(
                helper.getLevel(), libraryPos, helper.getLevel().getBlockState(libraryPos), library);
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(libraryPos, library.getBlockState(),
                library.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof buildcraft.builders.block.entity.BlueprintLibraryBlockEntity,
                "Blueprint Library block entity did not reload");
        var restored = (buildcraft.builders.block.entity.BlueprintLibraryBlockEntity) loaded;
        helper.assertValueEqual(restored.progressIn(), 50, "reloaded Library lost store progress");
        helper.assertTrue(restored.inventory().getResource(0).toStack(1)
                .has(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get()),
                "reloaded Library lost store input");
        helper.getLevel().setBlockEntity(restored);
        for (int tick = 50; tick < buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.PROCESS_TIME; tick++)
            buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.tick(helper.getLevel(), libraryPos,
                    helper.getLevel().getBlockState(libraryPos), restored);
        helper.assertTrue(restored.inventory().getAmountAsLong(0) == 0
                        && restored.inventory().getResource(1).toStack(1).is(buildcraft.builders.BCBuildersItems.BLUEPRINT.get())
                        && restored.entryCount() == baseline + 1 && restored.selectedName().equals(name),
                "Library did not store and return used Blueprint");

        restored.inventory().set(2, net.neoforged.neoforge.transfer.item.ItemResource.of(
                buildcraft.builders.BCBuildersItems.BLUEPRINT.get()), 1);
        for (int tick = 0; tick < buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.PROCESS_TIME; tick++)
            buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.tick(helper.getLevel(), libraryPos,
                    helper.getLevel().getBlockState(libraryPos), restored);
        ItemStack downloaded = restored.inventory().getResource(3).toStack(1);
        helper.assertValueEqual(downloaded.get(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get()), snapshot,
                "Library download lost exact typed Snapshot");

        restored.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        restored.inventory().set(3, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        restored.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(used), 1);
        for (int tick = 0; tick < buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.PROCESS_TIME; tick++)
            buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.tick(helper.getLevel(), libraryPos,
                    helper.getLevel().getBlockState(libraryPos), restored);
        helper.assertValueEqual(restored.entryCount(), baseline + 1, "Library duplicated identical content");

        var templateData = new buildcraft.builders.snapshot.SnapshotData(
                buildcraft.builders.snapshot.SnapshotKind.TEMPLATE, new BlockPos(1, 1, 1), Direction.NORTH, BlockPos.ZERO,
                java.util.List.of(Blocks.STONE.defaultBlockState()), java.util.List.of(0), name + " Template");
        ItemStack template = new ItemStack(buildcraft.builders.BCBuildersItems.TEMPLATE.get());
        template.set(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get(), templateData);
        restored.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        restored.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(template), 1);
        for (int tick = 0; tick < buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.PROCESS_TIME; tick++)
            buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.tick(helper.getLevel(), libraryPos,
                    helper.getLevel().getBlockState(libraryPos), restored);
        helper.assertValueEqual(restored.entryCount(), baseline + 2, "Library did not store Template entry");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new buildcraft.builders.menu.BlueprintLibraryMenu(48, player.getInventory(), libraryPos);
        helper.assertTrue(menu.clickMenuButton(player, 0) && menu.clickMenuButton(player, 1)
                        && menu.clickMenuButton(player, 2), "Library menu rejected navigation or deletion");
        helper.assertValueEqual(restored.entryCount(), baseline + 1, "Library menu did not delete selected entry");

        ItemStack writtenBook = new ItemStack(Items.WRITTEN_BOOK);
        writtenBook.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT,
                net.minecraft.world.item.component.WrittenBookContent.EMPTY);
        restored.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        restored.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(writtenBook), 1);
        for (int tick = 0; tick < buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.PROCESS_TIME; tick++)
            buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.tick(helper.getLevel(), libraryPos,
                    helper.getLevel().getBlockState(libraryPos), restored);
        restored.inventory().set(2, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.WRITABLE_BOOK), 1);
        for (int tick = 0; tick < buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.PROCESS_TIME; tick++)
            buildcraft.builders.block.entity.BlueprintLibraryBlockEntity.tick(helper.getLevel(), libraryPos,
                    helper.getLevel().getBlockState(libraryPos), restored);
        ItemStack loadedBook = restored.inventory().getResource(3).toStack(1);
        helper.assertTrue(loadedBook.is(Items.WRITTEN_BOOK)
                        && loadedBook.has(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT),
                "Library did not round-trip a written book through a writable book");
        helper.assertTrue(restored.deleteSelected(), "Library did not remove test book entry");

        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 1, java.util.List.of(
                new ItemStack(buildcraft.builders.BCBuildersItems.BLUEPRINT.get()),
                new ItemStack(Items.BOOKSHELF), new ItemStack(Items.REDSTONE)));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.builders.BCBuildersItems.BLUEPRINT_LIBRARY.get()),
                "Blueprint Library recipe output");
        var drops = Block.getDrops(helper.getLevel().getBlockState(libraryPos), helper.getLevel(), libraryPos,
                restored, null, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(buildcraft.builders.BCBuildersItems.BLUEPRINT_LIBRARY.get()),
                "Blueprint Library loot output");
        helper.succeed();
    }

    private static void roboticsRedstoneBoard(GameTestHelper helper) {
        for (buildcraft.robotics.RobotBoardType type : buildcraft.robotics.RobotBoardType.values()) {
            ItemStack stack = buildcraft.robotics.BCRoboticsItems.board(type);
            helper.assertValueEqual(stack.get(buildcraft.robotics.BCRoboticsDataComponents.BOARD_TYPE.get()), type,
                    "Redstone Board lost its type");
            helper.assertTrue(stack.has(net.minecraft.core.component.DataComponents.ITEM_MODEL),
                    "Redstone Board did not select its colour model");
            var encoded = buildcraft.robotics.RobotBoardType.CODEC.encodeStart(
                    com.mojang.serialization.JsonOps.INSTANCE, type).getOrThrow();
            helper.assertValueEqual(buildcraft.robotics.RobotBoardType.CODEC.parse(
                    com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow(), type,
                    "Redstone Board type codec round-trip");
        }
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER),
                new ItemStack(Items.PAPER), new ItemStack(Items.REDSTONE), new ItemStack(Items.PAPER),
                new ItemStack(Items.PAPER), new ItemStack(Items.PAPER), new ItemStack(Items.PAPER)));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow().value().assemble(input);
        helper.assertTrue(crafted.is(buildcraft.robotics.BCRoboticsItems.REDSTONE_BOARD.get()),
                "Redstone Board recipe output");
        helper.succeed();
    }

    private static void roboticsRequester(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(pos, buildcraft.robotics.BCRoboticsBlocks.REQUESTER.get().defaultBlockState(),
                Block.UPDATE_ALL);
        var requester = (buildcraft.robotics.block.entity.RequesterBlockEntity) helper.getLevel().getBlockEntity(pos);
        requester.setRequest(0, new ItemStack(Items.STONE, 12));
        requester.setRequest(1, new ItemStack(Items.DIRT, 5));
        helper.assertValueEqual(requester.getRequest(0).getCount(), 12, "Requester initial remaining count");
        ItemStack rejected = requester.offerItem(0, new ItemStack(Items.DIRT, 3));
        helper.assertValueEqual(rejected.getCount(), 3, "Requester accepted a nonmatching delivery");
        helper.assertTrue(requester.offerItem(0, new ItemStack(Items.STONE, 8)).isEmpty(),
                "Requester rejected a matching partial delivery");
        helper.assertValueEqual(requester.getRequest(0).getCount(), 4, "Requester partial remaining count");
        ItemStack excess = requester.offerItem(0, new ItemStack(Items.STONE, 10));
        helper.assertValueEqual(excess.getCount(), 6, "Requester did not cap delivery at requested count");
        helper.assertTrue(requester.fulfilled(0) && !requester.fulfilled(1), "Requester fulfillment state");
        helper.assertValueEqual(requester.comparatorLevel(), 10, "Requester comparator fulfillment ratio");

        var capability = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, pos, Direction.NORTH);
        helper.assertTrue(capability != null && capability.size() == 20, "Requester item capability");
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
                     net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int rejectedInsert = capability.insert(1,
                    net.neoforged.neoforge.transfer.item.ItemResource.of(Items.STONE), 2, transaction);
            helper.assertValueEqual(rejectedInsert, 0, "Requester capability accepted wrong item");
        }

        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, requester.getBlockState(),
                requester.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof buildcraft.robotics.block.entity.RequesterBlockEntity,
                "Requester block entity did not reload");
        var restored = (buildcraft.robotics.block.entity.RequesterBlockEntity) loaded;
        helper.assertValueEqual(restored.requestTemplate(0).getCount(), 12, "Requester lost request template");
        helper.assertValueEqual(restored.stored(0).getCount(), 12, "Requester lost delivered inventory");
        helper.getLevel().setBlockEntity(restored);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new buildcraft.robotics.menu.RequesterMenu(1, player.getInventory(), pos);
        menu.setCarried(new ItemStack(Items.GOLD_INGOT, 7));
        menu.clicked(2, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        helper.assertTrue(restored.requestTemplate(2).is(Items.GOLD_INGOT)
                        && restored.requestTemplate(2).getCount() == 7,
                "Requester menu did not edit request template");

        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.PISTON), new ItemStack(Items.IRON_INGOT),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_IRON.get()), new ItemStack(Items.CHEST),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_IRON.get()), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.REDSTONE), new ItemStack(Items.IRON_INGOT)));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.robotics.BCRoboticsItems.REQUESTER.get()), "Requester recipe output");
        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, requester,
                null, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(buildcraft.robotics.BCRoboticsItems.REQUESTER.get()),
                "Requester block loot");
        helper.succeed();
    }

    private static void roboticsZoneData(GameTestHelper helper) {
        var zone = new buildcraft.robotics.zone.ZonePlan();
        zone.set(-1, -1, true);
        zone.set(0, 0, true);
        zone.set(16, 32, true);
        helper.assertTrue(zone.get(-1, -1) && zone.get(0, 0) && zone.get(16, 32) && zone.size() == 3,
                "Zone chunk-bitset addressing");
        zone.set(0, 0, false);
        helper.assertTrue(!zone.get(0, 0) && zone.size() == 2, "Zone cell removal");
        var encoded = buildcraft.robotics.zone.ZonePlan.CODEC.encodeStart(
                com.mojang.serialization.JsonOps.INSTANCE, zone).getOrThrow();
        var decoded = buildcraft.robotics.zone.ZonePlan.CODEC.parse(
                com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow();
        helper.assertValueEqual(decoded, zone, "Zone codec round-trip");
        BlockPos random = decoded.random(new java.util.Random(1), 70);
        helper.assertTrue(random != null && random.getY() == 70 && decoded.contains(random),
                "Zone random position escaped selection");

        ItemStack map = new ItemStack(buildcraft.core.BCCoreItems.MAP_LOCATION.get());
        buildcraft.robotics.zone.ZoneMapLocation.set(map, zone, "Farm plots");
        helper.assertValueEqual(buildcraft.core.BCCoreItems.MAP_LOCATION.get().getType(map),
                buildcraft.api.items.MapLocationType.ZONE, "Zone Map Location type");
        helper.assertValueEqual(buildcraft.core.BCCoreItems.MAP_LOCATION.get().getStoredName(map),
                "Farm plots", "Zone Map Location name");
        helper.assertValueEqual(buildcraft.robotics.zone.ZoneMapLocation.get(map), zone,
                "Zone Map Location component");
        helper.succeed();
    }

    private static void roboticsZonePlanner(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlock(pos, buildcraft.robotics.BCRoboticsBlocks.ZONE_PLANNER.get().defaultBlockState(),
                Block.UPDATE_ALL);
        var planner = (buildcraft.robotics.block.entity.ZonePlannerBlockEntity) helper.getLevel().getBlockEntity(pos);
        planner.setMapName("Orchard");
        int red = net.minecraft.world.item.DyeColor.RED.getId();
        helper.assertTrue(planner.edit(red, -2, -1, 1, 2, true), "Zone Planner rejected rectangle edit");
        helper.assertValueEqual(planner.layer(red).size(), 16, "Zone Planner rectangle size");
        ItemStack redBrush = buildcraft.core.item.ItemPaintbrush.colored(
                buildcraft.core.BCCoreItems.PAINTBRUSH.get(), net.minecraft.world.item.DyeColor.RED);
        planner.inventory().set(buildcraft.robotics.block.entity.ZonePlannerBlockEntity.SLOT_OUTPUT_BRUSH,
                net.neoforged.neoforge.transfer.item.ItemResource.of(redBrush), 1);
        planner.inventory().set(buildcraft.robotics.block.entity.ZonePlannerBlockEntity.SLOT_OUTPUT_MAP,
                net.neoforged.neoforge.transfer.item.ItemResource.of(buildcraft.core.BCCoreItems.MAP_LOCATION.get()), 1);
        for (int tick = 0; tick < 50; tick++) buildcraft.robotics.block.entity.ZonePlannerBlockEntity.tick(
                helper.getLevel(), pos, helper.getLevel().getBlockState(pos), planner);
        helper.assertValueEqual(planner.progressOutput(), 50, "Zone Planner output progress");

        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, planner.getBlockState(),
                planner.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof buildcraft.robotics.block.entity.ZonePlannerBlockEntity,
                "Zone Planner block entity did not reload");
        var restored = (buildcraft.robotics.block.entity.ZonePlannerBlockEntity) loaded;
        helper.assertValueEqual(restored.progressOutput(), 50, "Zone Planner lost output progress");
        helper.assertValueEqual(restored.layer(red).size(), 16, "Zone Planner lost layer data");
        helper.getLevel().setBlockEntity(restored);
        for (int tick = 50; tick < buildcraft.robotics.block.entity.ZonePlannerBlockEntity.PROCESS_TIME; tick++)
            buildcraft.robotics.block.entity.ZonePlannerBlockEntity.tick(
                    helper.getLevel(), pos, helper.getLevel().getBlockState(pos), restored);
        ItemStack exported = restored.inventory().getResource(
                buildcraft.robotics.block.entity.ZonePlannerBlockEntity.SLOT_OUTPUT_RESULT).toStack(1);
        var worldZone = buildcraft.robotics.zone.ZoneMapLocation.get(exported);
        helper.assertTrue(worldZone != null && worldZone.size() == 16
                        && worldZone.get(pos.getX() - 2, pos.getZ() - 1)
                        && worldZone.get(pos.getX() + 1, pos.getZ() + 2),
                "Zone Planner export lost world-coordinate selection");
        helper.assertValueEqual(buildcraft.core.BCCoreItems.MAP_LOCATION.get().getStoredName(exported),
                "Orchard", "Zone Planner export name");

        restored.inventory().set(buildcraft.robotics.block.entity.ZonePlannerBlockEntity.SLOT_OUTPUT_RESULT,
                net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        int blue = net.minecraft.world.item.DyeColor.BLUE.getId();
        ItemStack blueBrush = buildcraft.core.item.ItemPaintbrush.colored(
                buildcraft.core.BCCoreItems.PAINTBRUSH.get(), net.minecraft.world.item.DyeColor.BLUE);
        restored.inventory().set(buildcraft.robotics.block.entity.ZonePlannerBlockEntity.SLOT_INPUT_BRUSH,
                net.neoforged.neoforge.transfer.item.ItemResource.of(blueBrush), 1);
        restored.inventory().set(buildcraft.robotics.block.entity.ZonePlannerBlockEntity.SLOT_INPUT_MAP,
                net.neoforged.neoforge.transfer.item.ItemResource.of(exported), 1);
        for (int tick = 0; tick < buildcraft.robotics.block.entity.ZonePlannerBlockEntity.PROCESS_TIME; tick++)
            buildcraft.robotics.block.entity.ZonePlannerBlockEntity.tick(
                    helper.getLevel(), pos, helper.getLevel().getBlockState(pos), restored);
        helper.assertValueEqual(restored.layer(blue), restored.layer(red), "Zone Planner import offset/layer");
        ItemStack cleanResult = restored.inventory().getResource(
                buildcraft.robotics.block.entity.ZonePlannerBlockEntity.SLOT_INPUT_RESULT).toStack(1);
        helper.assertTrue(cleanResult.is(buildcraft.core.BCCoreItems.MAP_LOCATION.get())
                        && buildcraft.core.BCCoreItems.MAP_LOCATION.get().getType(cleanResult)
                        == buildcraft.api.items.MapLocationType.CLEAN,
                "Zone Planner import did not return a clean map");
        helper.assertTrue(java.util.Arrays.stream(restored.preview()).anyMatch(color -> color != 0),
                "Zone Planner did not synchronize terrain preview");

        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.REDSTONE), new ItemStack(Items.IRON_INGOT),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_GOLD.get()), new ItemStack(Items.MAP),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_GOLD.get()), new ItemStack(Items.IRON_INGOT),
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_DIAMOND.get()), new ItemStack(Items.IRON_INGOT)));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.robotics.BCRoboticsItems.ZONE_PLANNER.get()),
                "Zone Planner recipe output");
        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, restored,
                null, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(buildcraft.robotics.BCRoboticsItems.ZONE_PLANNER.get()),
                "Zone Planner block loot");
        helper.succeed();
    }

    private static void buildersConstructionMarker(GameTestHelper helper) {
        BlockPos architectPos = helper.absolutePos(new BlockPos(1, 3, 1));
        BlockPos mainPos = architectPos.east();
        BlockPos markerPos = helper.absolutePos(new BlockPos(5, 3, 1));
        helper.getLevel().setBlock(architectPos,
                buildcraft.builders.BCBuildersBlocks.ARCHITECT_TABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        var architect = (buildcraft.builders.block.entity.ArchitectTableBlockEntity)
                helper.getLevel().getBlockEntity(architectPos);
        helper.assertTrue(architect.configureArea(mainPos, mainPos), "Architect rejected compound primary area");
        helper.getLevel().setBlock(mainPos, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(markerPos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(markerPos,
                buildcraft.builders.BCBuildersBlocks.CONSTRUCTION_MARKER.get().defaultBlockState(), Block.UPDATE_ALL);
        var marker = (buildcraft.builders.block.entity.ConstructionMarkerBlockEntity)
                helper.getLevel().getBlockEntity(markerPos);
        var stairs = Blocks.SPRUCE_STAIRS.defaultBlockState()
                .setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.WEST);
        var linkedSnapshot = new buildcraft.builders.snapshot.SnapshotData(
                buildcraft.builders.snapshot.SnapshotKind.BLUEPRINT, new BlockPos(2, 1, 1), Direction.SOUTH,
                new BlockPos(1, 0, 0), java.util.List.of(stairs, Blocks.GLASS.defaultBlockState()),
                java.util.List.of(0, 1), "Linked Marker");
        ItemStack linkedBlueprint = new ItemStack(buildcraft.builders.BCBuildersItems.BLUEPRINT.get());
        linkedBlueprint.set(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get(), linkedSnapshot);
        helper.assertTrue(marker.setBlueprint(linkedBlueprint), "Construction Marker rejected used Blueprint");

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack linker = new ItemStack(buildcraft.builders.BCBuildersItems.CONSTRUCTION_MARKER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, linker);
        var architectHit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(architectPos),
                Direction.UP, architectPos, false);
        var markerHit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(markerPos),
                Direction.UP, markerPos, false);
        helper.assertTrue(buildcraft.builders.BCBuildersItems.CONSTRUCTION_MARKER.get().useOn(
                new net.minecraft.world.item.context.UseOnContext(helper.getLevel(), player,
                        net.minecraft.world.InteractionHand.MAIN_HAND, linker, architectHit)).consumesAction(),
                "Construction Marker item did not start link");
        helper.assertTrue(linker.has(buildcraft.builders.BCBuildersDataComponents.CONSTRUCTION_LINK.get())
                        && linker.has(net.minecraft.core.component.DataComponents.ITEM_MODEL),
                "Construction Marker link state or recording model missing");
        helper.assertTrue(buildcraft.builders.BCBuildersItems.CONSTRUCTION_MARKER.get().useOn(
                new net.minecraft.world.item.context.UseOnContext(helper.getLevel(), player,
                        net.minecraft.world.InteractionHand.MAIN_HAND, linker, markerHit)).consumesAction(),
                "Construction Marker item did not finish link");
        helper.assertTrue(architect.linkedMarkers().equals(java.util.List.of(markerPos))
                        && !linker.has(buildcraft.builders.BCBuildersDataComponents.CONSTRUCTION_LINK.get())
                        && !linker.has(net.minecraft.core.component.DataComponents.ITEM_MODEL),
                "Construction Marker did not register or clear link state");

        var loadedArchitect = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(architectPos,
                architect.getBlockState(), architect.saveWithFullMetadata(helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());
        var loadedMarker = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(markerPos,
                marker.getBlockState(), marker.saveWithFullMetadata(helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());
        helper.assertTrue(loadedArchitect instanceof buildcraft.builders.block.entity.ArchitectTableBlockEntity restoredArchitect
                        && restoredArchitect.linkedMarkers().equals(java.util.List.of(markerPos)),
                "Architect lost linked Construction Marker on reload");
        helper.assertTrue(loadedMarker instanceof buildcraft.builders.block.entity.ConstructionMarkerBlockEntity restoredMarker
                        && linkedSnapshot.equals(restoredMarker.snapshot()),
                "Construction Marker lost Blueprint on reload");
        architect = (buildcraft.builders.block.entity.ArchitectTableBlockEntity) loadedArchitect;
        helper.getLevel().setBlockEntity(architect);
        architect.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(
                buildcraft.builders.BCBuildersItems.BLUEPRINT.get()), 1);
        buildcraft.builders.block.entity.ArchitectTableBlockEntity.tick(helper.getLevel(), architectPos,
                helper.getLevel().getBlockState(architectPos), architect);
        var composite = architect.inventory().getResource(1).toStack(1)
                .get(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get());
        helper.assertTrue(composite != null && composite.valid(), "Architect did not emit compound Blueprint");
        helper.assertValueEqual(composite.size(), new BlockPos(1, 1, 6), "compound Blueprint union size");
        helper.assertValueEqual(composite.offset(), new BlockPos(0, 0, 1), "compound Blueprint union offset");
        helper.assertTrue(composite.stateAt(BlockPos.ZERO).is(Blocks.STONE)
                        && composite.stateAt(new BlockPos(0, 0, 4)).equals(
                                stairs.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90))
                        && composite.stateAt(new BlockPos(0, 0, 5)).is(Blocks.GLASS),
                "compound Blueprint lost primary or linked exact states");

        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(1, 2, java.util.List.of(
                new ItemStack(buildcraft.core.BCCoreItems.GEAR_GOLD.get()), new ItemStack(Items.REDSTONE_TORCH)));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.builders.BCBuildersItems.CONSTRUCTION_MARKER.get()),
                "Construction Marker recipe output");
        var drops = Block.getDrops(helper.getLevel().getBlockState(markerPos), helper.getLevel(), markerPos,
                marker, null, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(buildcraft.builders.BCBuildersItems.CONSTRUCTION_MARKER.get()),
                "Construction Marker loot output");
        helper.succeed();
    }

    private static void buildersFiller(GameTestHelper helper) {
        BlockPos fillerPos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos first = fillerPos.north();
        BlockPos last = first.east(2);
        var volumeData = buildcraft.core.marker.VolumeSavedData.get(helper.getLevel());
        volumeData.addMarker(first);
        volumeData.addMarker(last);
        helper.assertTrue(volumeData.connect(first, last), "filler test volume markers did not connect");
        helper.getLevel().setBlock(fillerPos,
                buildcraft.builders.BCBuildersBlocks.FILLER.get().defaultBlockState(), Block.UPDATE_ALL);
        var filler = (buildcraft.builders.block.entity.FillerBlockEntity)
                helper.getLevel().getBlockEntity(fillerPos);
        insertPipeItem(filler.resources(), Items.COBBLESTONE, 3);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(12 * MjAPI.MJ, false),
                "filler rejected nominal MJ input");

        BlockPos pipePos = fillerPos.south();
        var pipeBlock = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        helper.getLevel().setBlock(pipePos, pipeBlock.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_ITEM), Block.UPDATE_ALL);
        var pipe = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.MACHINE_CONTROL_OFF))));
        filler.setControlMode(buildcraft.api.core.IControllable.ControlMode.ON);
        pipe.setAttachment(Direction.NORTH, gate);
        tickPipes(helper, 1, pipePos);
        helper.assertTrue(filler.controlMode() == buildcraft.api.core.IControllable.ControlMode.OFF,
                "Gate Machine Off action did not control the Filler");
        buildcraft.builders.block.entity.FillerBlockEntity.tick(helper.getLevel(), fillerPos,
                helper.getLevel().getBlockState(fillerPos), filler);
        helper.assertTrue(helper.getLevel().getBlockState(first).isAir(),
                "disabled Filler placed a block");

        pipe.attachment(Direction.NORTH).set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.MACHINE_CONTROL_ON))));
        tickPipes(helper, 1, pipePos);
        helper.assertTrue(filler.controlMode() == buildcraft.api.core.IControllable.ControlMode.ON,
                "Gate Machine On action did not control the Filler");
        for (int tick = 0; tick < 4; tick++) {
            buildcraft.builders.block.entity.FillerBlockEntity.tick(helper.getLevel(), fillerPos,
                    helper.getLevel().getBlockState(fillerPos), filler);
        }
        for (BlockPos target : BlockPos.betweenClosed(first, last)) {
            helper.assertTrue(helper.getLevel().getBlockState(target).is(Blocks.COBBLESTONE),
                    "Filler did not fill the complete marker volume");
        }
        helper.assertValueEqual(0L, filler.resources().getAmountAsLong(0),
                "Filler did not consume exactly three resources");
        helper.assertValueEqual(0L, filler.storedMj(), "Filler did not consume 4 MJ per block");
        helper.assertTrue(filler.finished() && !filler.hasWork(),
                "Filler did not report completed On-mode work");

        var menuPlayer = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new buildcraft.builders.menu.FillerMenu(41, menuPlayer.getInventory(), fillerPos);
        helper.assertTrue(menu.clickMenuButton(menuPlayer, 2), "Filler menu rejected Loop mode");
        helper.assertTrue(filler.controlMode() == buildcraft.api.core.IControllable.ControlMode.LOOP
                        && filler.hasWork(),
                "Filler Loop mode did not remain active after completion");

        var restored = reloadBuildersFiller(helper, fillerPos, filler);
        helper.assertTrue(restored.areaMin() != null && restored.areaMin().equals(first),
                "reloaded Filler lost its area minimum");
        helper.assertTrue(restored.areaMax() != null && restored.areaMax().equals(last),
                "reloaded Filler lost its area maximum");
        helper.assertTrue(restored.controlMode() == buildcraft.api.core.IControllable.ControlMode.LOOP,
                "reloaded Filler lost its control mode");
        helper.assertTrue(restored.finished(), "reloaded Filler lost its completion state");

        net.minecraft.world.item.crafting.CraftingInput recipeInput =
                net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                        new ItemStack(Items.BLACK_DYE), new ItemStack(buildcraft.core.BCCoreItems.MARKER_VOLUME.get()),
                        new ItemStack(Items.BLACK_DYE), new ItemStack(Items.YELLOW_DYE),
                        new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.YELLOW_DYE),
                        new ItemStack(buildcraft.core.BCCoreItems.GEAR_GOLD.get()), new ItemStack(Items.CHEST),
                        new ItemStack(buildcraft.core.BCCoreItems.GEAR_GOLD.get())));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.builders.BCBuildersItems.FILLER.get()),
                "Filler recipe returned the wrong item");
        var drops = Block.getDrops(helper.getLevel().getBlockState(fillerPos), helper.getLevel(), fillerPos,
                filler, null, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(buildcraft.builders.BCBuildersItems.FILLER.get()),
                "Filler loot table did not return itself");
        helper.succeed();
    }

    private static void buildersFillerPatterns(GameTestHelper helper) {
        BlockPos fillerPos = helper.absolutePos(new BlockPos(1, 3, 3));
        BlockPos min = fillerPos.east();
        BlockPos max = min.offset(2, 2, 2);
        helper.getLevel().setBlock(fillerPos,
                buildcraft.builders.BCBuildersBlocks.FILLER.get().defaultBlockState(), Block.UPDATE_ALL);
        var filler = (buildcraft.builders.block.entity.FillerBlockEntity)
                helper.getLevel().getBlockEntity(fillerPos);
        helper.assertTrue(filler.configureArea(min, max), "Filler rejected valid 3x3x3 bounds");

        insertPipeItem(filler.resources(), Items.STONE, 26);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(104 * MjAPI.MJ, false),
                "Box pattern rejected nominal MJ input");
        filler.setPattern(buildcraft.builders.FillerPattern.BOX);
        helper.assertValueEqual(filler.areaMin(), min,
                "Filler did not copy the marker minimum");
        helper.assertValueEqual(filler.areaMax(), max,
                "Filler did not copy the marker maximum");
        helper.assertValueEqual(26L, filler.resources().getAmountAsLong(0),
                "Box pattern resources were not inserted");
        helper.assertValueEqual(104 * MjAPI.MJ, filler.storedMj(),
                "Box pattern MJ was not stored");
        tickFiller(helper, fillerPos, filler, 27);
        helper.assertValueEqual(countBlocks(helper, min, max, Blocks.STONE), 26,
                "Box pattern did not place the six boundary planes");
        helper.assertTrue(helper.getLevel().getBlockState(min.offset(1, 1, 1)).isAir(),
                "Box pattern filled its interior");
        helper.assertValueEqual(0L, filler.storedMj(), "Box pattern used the wrong MJ total");

        for (BlockPos target : BlockPos.betweenClosed(min, max)) {
            helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        insertPipeItem(filler.resources(), Items.GLASS, 20);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(80 * MjAPI.MJ, false),
                "Frame pattern rejected nominal MJ input");
        filler.setPattern(buildcraft.builders.FillerPattern.FRAME);
        tickFiller(helper, fillerPos, filler, 21);
        helper.assertValueEqual(countBlocks(helper, min, max, Blocks.GLASS), 20,
                "Frame pattern did not place exactly the twelve edges");
        helper.assertTrue(helper.getLevel().getBlockState(min.offset(1, 1, 0)).isAir(),
                "Frame pattern filled a face interior");

        for (BlockPos target : BlockPos.betweenClosed(min, max)) {
            helper.getLevel().setBlock(target, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_ALL);
        }
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(108 * MjAPI.MJ, false),
                "Clear pattern rejected nominal MJ input");
        filler.setPattern(buildcraft.builders.FillerPattern.CLEAR);
        tickFiller(helper, fillerPos, filler, 28);
        helper.assertValueEqual(countBlocks(helper, min, max, Blocks.AIR), 27,
                "Clear pattern did not excavate the full volume");
        long recovered = 0;
        for (int slot = 0; slot < filler.resources().size(); slot++) {
            recovered += filler.resources().getAmountAsLong(slot);
        }
        helper.assertValueEqual(recovered, 27L, "Clear pattern did not recover block drops");
        helper.assertValueEqual(0L, filler.storedMj(), "Clear pattern used the wrong MJ total");

        BlockPos center = min.offset(1, 1, 1);
        helper.getLevel().setBlock(center, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(4 * MjAPI.MJ, false),
                "None pattern rejected stored MJ");
        filler.setPattern(buildcraft.builders.FillerPattern.NONE);
        tickFiller(helper, fillerPos, filler, 2);
        helper.assertTrue(helper.getLevel().getBlockState(center).is(Blocks.DIRT),
                "None pattern mutated the area");
        helper.assertValueEqual(4 * MjAPI.MJ, filler.storedMj(), "None pattern consumed MJ");

        var menuPlayer = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new buildcraft.builders.menu.FillerMenu(42, menuPlayer.getInventory(), fillerPos);
        helper.assertTrue(menu.clickMenuButton(menuPlayer, 13), "Filler menu rejected Box pattern");
        helper.assertTrue(filler.pattern() == buildcraft.builders.FillerPattern.BOX,
                "Filler menu selected the wrong pattern");
        var restored = reloadBuildersFiller(helper, fillerPos, filler);
        helper.assertTrue(restored.pattern() == buildcraft.builders.FillerPattern.BOX,
                "reloaded Filler lost its selected pattern");
        helper.assertTrue(min.equals(restored.areaMin()) && max.equals(restored.areaMax()),
                "Filler lost its copied area bounds");
        helper.succeed();
    }

    private static void buildersFillerAdvancedPatterns(GameTestHelper helper) {
        BlockPos fillerPos = helper.absolutePos(new BlockPos(1, 2, 2));
        BlockPos min = fillerPos.east();
        BlockPos max = min.offset(4, 2, 4);
        helper.getLevel().setBlock(fillerPos,
                buildcraft.builders.BCBuildersBlocks.FILLER.get().defaultBlockState(), Block.UPDATE_ALL);
        var filler = (buildcraft.builders.block.entity.FillerBlockEntity)
                helper.getLevel().getBlockEntity(fillerPos);
        helper.assertTrue(filler.configureArea(min, max), "Pyramid Filler rejected valid bounds");
        filler.setVerticalDirection(Direction.UP);
        filler.setPattern(buildcraft.builders.FillerPattern.PYRAMID);
        insertPipeItem(filler.resources(), Items.STONE, 35);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(140 * MjAPI.MJ, false),
                "Pyramid pattern rejected nominal MJ input");
        tickFiller(helper, fillerPos, filler, 36);
        helper.assertValueEqual(countBlocks(helper, min, max, Blocks.STONE), 35,
                "Pyramid pattern placed the wrong total");
        helper.assertValueEqual(countBlocks(helper, min,
                new BlockPos(max.getX(), min.getY(), max.getZ()), Blocks.STONE), 25,
                "Pyramid base layer was not 5x5");
        helper.assertValueEqual(countBlocks(helper, min.offset(1, 1, 1),
                max.offset(-1, -1, -1), Blocks.STONE), 9,
                "Pyramid middle layer was not 3x3");
        helper.assertTrue(helper.getLevel().getBlockState(min.offset(2, 2, 2)).is(Blocks.STONE),
                "Pyramid apex was missing");
        helper.assertValueEqual(0L, filler.storedMj(), "Pyramid pattern used the wrong MJ total");

        for (BlockPos target : BlockPos.betweenClosed(min, max)) {
            helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        filler.setHorizontalDirection(Direction.EAST);
        filler.setVerticalDirection(Direction.UP);
        filler.setPattern(buildcraft.builders.FillerPattern.STAIRS);
        insertPipeItem(filler.resources(), Items.GLASS, 60);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(240 * MjAPI.MJ, false),
                "Stairs pattern rejected nominal MJ input");
        tickFiller(helper, fillerPos, filler, 61);
        helper.assertValueEqual(countBlocks(helper, min, max, Blocks.GLASS), 60,
                "east/up Stairs pattern placed the wrong total");
        helper.assertValueEqual(countBlocks(helper, min,
                new BlockPos(max.getX(), min.getY(), max.getZ()), Blocks.GLASS), 25,
                "east/up Stairs base was not a full plane");
        helper.assertTrue(helper.getLevel().getBlockState(min.offset(0, 1, 2)).isAir()
                        && helper.getLevel().getBlockState(min.offset(1, 1, 2)).is(Blocks.GLASS),
                "east/up Stairs did not advance one column per layer");
        helper.assertTrue(helper.getLevel().getBlockState(min.offset(1, 2, 2)).isAir()
                        && helper.getLevel().getBlockState(min.offset(2, 2, 2)).is(Blocks.GLASS),
                "east/up Stairs did not advance two columns at the top");

        var menuPlayer = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new buildcraft.builders.menu.FillerMenu(43, menuPlayer.getInventory(), fillerPos);
        helper.assertTrue(menu.clickMenuButton(menuPlayer, 30), "Filler menu rejected downward direction");
        helper.assertTrue(menu.clickMenuButton(menuPlayer, 31), "Filler menu rejected horizontal rotation");
        helper.assertTrue(filler.verticalDirection() == Direction.DOWN
                        && filler.horizontalDirection() == Direction.SOUTH,
                "Filler menu selected the wrong Stairs directions");

        BlockPos smallMax = min.offset(2, 1, 2);
        for (BlockPos target : BlockPos.betweenClosed(min, max)) {
            helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        helper.assertTrue(filler.configureArea(min, smallMax), "Stairs Filler rejected reduced bounds");
        insertPipeItem(filler.resources(), Items.BRICKS, 15);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(60 * MjAPI.MJ, false),
                "down/south Stairs rejected nominal MJ input");
        tickFiller(helper, fillerPos, filler, 16);
        helper.assertValueEqual(countBlocks(helper, min, smallMax, Blocks.BRICKS), 15,
                "down/south Stairs pattern placed the wrong total");
        helper.assertValueEqual(countBlocks(helper,
                new BlockPos(min.getX(), smallMax.getY(), min.getZ()), smallMax, Blocks.BRICKS), 9,
                "down/south Stairs top was not a full plane");
        helper.assertTrue(helper.getLevel().getBlockState(min.offset(1, 0, 0)).isAir()
                        && helper.getLevel().getBlockState(min.offset(1, 0, 1)).is(Blocks.BRICKS),
                "down/south Stairs did not advance south on its lower layer");

        var restored = reloadBuildersFiller(helper, fillerPos, filler);
        helper.assertTrue(restored.pattern() == buildcraft.builders.FillerPattern.STAIRS,
                "reloaded Filler lost its Stairs pattern");
        helper.assertTrue(restored.verticalDirection() == Direction.DOWN
                        && restored.horizontalDirection() == Direction.SOUTH,
                "reloaded Filler lost its Stairs directions");
        helper.succeed();
    }

    private static void buildersFillerPyramidCentres(GameTestHelper helper) {
        BlockPos fillerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos min = fillerPos.east();
        BlockPos max = min.offset(4, 2, 4);
        helper.getLevel().setBlock(fillerPos,
                buildcraft.builders.BCBuildersBlocks.FILLER.get().defaultBlockState(), Block.UPDATE_ALL);
        var filler = (buildcraft.builders.block.entity.FillerBlockEntity)
                helper.getLevel().getBlockEntity(fillerPos);
        helper.assertTrue(filler.configureArea(min, max), "Pyramid Filler rejected valid bounds");
        filler.setPattern(buildcraft.builders.FillerPattern.PYRAMID);
        filler.setVerticalDirection(Direction.UP);

        int[] totals = { 50, 40, 50, 40, 35, 40, 50, 40, 50 };
        for (int center = 0; center < totals.length; center++) {
            filler.setPyramidCenter(center);
            int expected = totals[center];
            insertPipeItem(filler.resources(), Items.STONE, expected);
            helper.assertValueEqual(0L, filler.mjReceiver().receivePower(expected * 4L * MjAPI.MJ, false),
                    "Pyramid centre " + center + " rejected nominal MJ input");
            tickFiller(helper, fillerPos, filler, expected + 1);
            helper.assertValueEqual(countBlocks(helper, min, max, Blocks.STONE), expected,
                    "Pyramid centre " + center + " placed the wrong layer total");
            if (center == 0) {
                helper.assertTrue(helper.getLevel().getBlockState(min.offset(0, 2, 0)).is(Blocks.STONE)
                                && helper.getLevel().getBlockState(min.offset(2, 2, 2)).is(Blocks.STONE)
                                && helper.getLevel().getBlockState(min.offset(3, 2, 2)).isAir(),
                        "north-west Pyramid centre shrank toward the wrong corner");
            }
            clearTestArea(helper, min, max);
        }

        filler.setPyramidCenter(4);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new buildcraft.builders.menu.FillerMenu(46, player.getInventory(), fillerPos);
        helper.assertTrue(menu.clickMenuButton(player, 37), "Filler menu rejected Pyramid centre cycle");
        helper.assertValueEqual(filler.pyramidCenter(), 5, "Filler menu selected the wrong Pyramid centre");
        var restored = reloadBuildersFiller(helper, fillerPos, filler);
        helper.assertTrue(restored.pattern() == buildcraft.builders.FillerPattern.PYRAMID
                        && restored.verticalDirection() == Direction.UP
                        && restored.pyramidCenter() == 5,
                "reloaded Filler lost its Pyramid centre");
        helper.succeed();
    }

    private static void buildersFillerSpherePatterns(GameTestHelper helper) {
        BlockPos fillerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos min = fillerPos.east();
        BlockPos max = min.offset(4, 4, 4);
        helper.getLevel().setBlock(fillerPos,
                buildcraft.builders.BCBuildersBlocks.FILLER.get().defaultBlockState(), Block.UPDATE_ALL);
        var filler = (buildcraft.builders.block.entity.FillerBlockEntity)
                helper.getLevel().getBlockEntity(fillerPos);
        helper.assertTrue(filler.configureArea(min, max), "Sphere Filler rejected valid bounds");

        filler.setHollow(false);
        filler.setPattern(buildcraft.builders.FillerPattern.SPHERE);
        insertPipeItem(filler.resources(), Items.STONE, 81);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(324 * MjAPI.MJ, false),
                "filled Sphere rejected nominal MJ input");
        tickFiller(helper, fillerPos, filler, 82);
        helper.assertValueEqual(countBlocks(helper, min, max, Blocks.STONE), 81,
                "filled 5x5x5 Sphere placed the wrong total");
        BlockPos center = min.offset(2, 2, 2);
        helper.assertTrue(helper.getLevel().getBlockState(center).is(Blocks.STONE)
                        && helper.getLevel().getBlockState(center.offset(2, 0, 0)).is(Blocks.STONE)
                        && helper.getLevel().getBlockState(center.offset(-2, 0, 0)).is(Blocks.STONE),
                "filled Sphere was not symmetric across its centre");
        helper.assertTrue(helper.getLevel().getBlockState(min).isAir()
                        && helper.getLevel().getBlockState(max).isAir(),
                "filled Sphere included bounding-box corners");

        clearTestArea(helper, min, max);
        filler.setHollow(true);
        insertPipeItem(filler.resources(), Items.GLASS, 54);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(216 * MjAPI.MJ, false),
                "hollow Sphere rejected nominal MJ input");
        tickFiller(helper, fillerPos, filler, 55);
        helper.assertValueEqual(countBlocks(helper, min, max, Blocks.GLASS), 54,
                "hollow 5x5x5 Sphere placed the wrong shell total");
        helper.assertTrue(helper.getLevel().getBlockState(center).isAir(),
                "hollow Sphere filled its interior");

        clearTestArea(helper, min, max);
        filler.setHollow(false);
        filler.setSphereFacing(Direction.DOWN);
        filler.setSphereRotation(0);
        filler.setPattern(buildcraft.builders.FillerPattern.HEMISPHERE);
        insertPipeItem(filler.resources(), Items.BRICKS, 69);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(276 * MjAPI.MJ, false),
                "Hemisphere rejected nominal MJ input");
        tickFiller(helper, fillerPos, filler, 70);
        helper.assertValueEqual(countBlocks(helper, min, max, Blocks.BRICKS), 69,
                "Down-facing Hemisphere placed the wrong total");

        clearTestArea(helper, min, max);
        filler.setPattern(buildcraft.builders.FillerPattern.QUARTER_SPHERE);
        insertPipeItem(filler.resources(), Items.SMOOTH_STONE, 70);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(280 * MjAPI.MJ, false),
                "Quarter Sphere rejected nominal MJ input");
        tickFiller(helper, fillerPos, filler, 71);
        helper.assertValueEqual(countBlocks(helper, min, max, Blocks.SMOOTH_STONE), 70,
                "Down/West Quarter Sphere placed the wrong total");

        clearTestArea(helper, min, max);
        filler.setPattern(buildcraft.builders.FillerPattern.EIGHTH_SPHERE);
        insertPipeItem(filler.resources(), Items.DEEPSLATE, 69);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(276 * MjAPI.MJ, false),
                "Eighth Sphere rejected nominal MJ input");
        tickFiller(helper, fillerPos, filler, 70);
        helper.assertValueEqual(countBlocks(helper, min, max, Blocks.DEEPSLATE), 69,
                "Down/West/North Eighth Sphere placed the wrong total");

        var menuPlayer = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new buildcraft.builders.menu.FillerMenu(44, menuPlayer.getInventory(), fillerPos);
        helper.assertTrue(menu.clickMenuButton(menuPlayer, 32), "Filler menu rejected hollow toggle");
        helper.assertTrue(menu.clickMenuButton(menuPlayer, 33), "Filler menu rejected sphere facing cycle");
        helper.assertTrue(menu.clickMenuButton(menuPlayer, 34), "Filler menu rejected sphere rotation");
        helper.assertTrue(filler.hollow() && filler.sphereFacing() == Direction.UP
                        && filler.sphereRotation() == 1,
                "Filler menu selected the wrong sphere parameters");
        var restored = reloadBuildersFiller(helper, fillerPos, filler);
        helper.assertTrue(restored.pattern() == buildcraft.builders.FillerPattern.EIGHTH_SPHERE
                        && restored.hollow() && restored.sphereFacing() == Direction.UP
                        && restored.sphereRotation() == 1,
                "reloaded Filler lost its sphere parameters");
        helper.succeed();
    }

    private static void clearTestArea(GameTestHelper helper, BlockPos min, BlockPos max) {
        for (BlockPos target : BlockPos.betweenClosed(min, max)) {
            helper.getLevel().setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void buildersFiller2dPatterns(GameTestHelper helper) {
        BlockPos fillerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos min = fillerPos.east();
        BlockPos max = min.offset(4, 2, 6);
        helper.getLevel().setBlock(fillerPos,
                buildcraft.builders.BCBuildersBlocks.FILLER.get().defaultBlockState(), Block.UPDATE_ALL);
        var filler = (buildcraft.builders.block.entity.FillerBlockEntity)
                helper.getLevel().getBlockEntity(fillerPos);
        helper.assertTrue(filler.configureArea(min, max), "2D Filler rejected valid bounds");
        filler.setShapeAxis(Direction.Axis.Y);
        filler.setShapeRotation(0);
        filler.setHollow(false);

        var patterns = new buildcraft.builders.FillerPattern[] {
                buildcraft.builders.FillerPattern.ARC,
                buildcraft.builders.FillerPattern.CIRCLE,
                buildcraft.builders.FillerPattern.HEXAGON,
                buildcraft.builders.FillerPattern.OCTAGON,
                buildcraft.builders.FillerPattern.PENTAGON,
                buildcraft.builders.FillerPattern.SEMICIRCLE,
                buildcraft.builders.FillerPattern.SQUARE,
                buildcraft.builders.FillerPattern.TRIANGLE
        };
        for (var pattern : patterns) {
            filler.setPattern(pattern);
            int expected = countShape2d(pattern, min, max, Direction.Axis.Y, 0, false);
            helper.assertTrue(expected > 0 && expected <= 105,
                    pattern + " generated an invalid filled template size");
            insertPipeItem(filler.resources(), Items.STONE, expected);
            helper.assertValueEqual(0L, filler.mjReceiver().receivePower(expected * 4L * MjAPI.MJ, false),
                    pattern + " rejected nominal MJ input");
            tickFiller(helper, fillerPos, filler, expected + 1);
            helper.assertValueEqual(countBlocks(helper, min, max, Blocks.STONE), expected,
                    pattern + " did not place its complete Y-axis extrusion");
            clearTestArea(helper, min, max);
        }

        filler.setPattern(buildcraft.builders.FillerPattern.SQUARE);
        filler.setHollow(true);
        int hollowSquare = countShape2d(buildcraft.builders.FillerPattern.SQUARE,
                min, max, Direction.Axis.Y, 0, true);
        helper.assertValueEqual(hollowSquare, 60, "hollow 5x7 Square extrusion geometry");
        insertPipeItem(filler.resources(), Items.GLASS, hollowSquare);
        helper.assertValueEqual(0L, filler.mjReceiver().receivePower(hollowSquare * 4L * MjAPI.MJ, false),
                "hollow Square rejected nominal MJ input");
        tickFiller(helper, fillerPos, filler, hollowSquare + 1);
        helper.assertValueEqual(countBlocks(helper, min, max, Blocks.GLASS), 60,
                "hollow Square placed the wrong total");
        helper.assertTrue(helper.getLevel().getBlockState(min.offset(2, 1, 3)).isAir(),
                "hollow Square filled its interior");
        var rotatedTriangle = buildcraft.builders.FillerShape2d.create(
                buildcraft.builders.FillerPattern.TRIANGLE, min, max, Direction.Axis.Y, 1, true);
        helper.assertTrue(rotatedTriangle.includes(min.offset(4, 1, 3)),
                "rotated Triangle did not move its apex clockwise");
        var zTriangle = buildcraft.builders.FillerShape2d.create(
                buildcraft.builders.FillerPattern.TRIANGLE, min, max, Direction.Axis.Z, 0, true);
        helper.assertTrue(zTriangle.includes(min.offset(2, 0, 6)),
                "Z-axis Triangle was not extruded across its selected axis");

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new buildcraft.builders.menu.FillerMenu(45, player.getInventory(), fillerPos);
        Direction.Axis oldAxis = filler.shapeAxis();
        helper.assertTrue(menu.clickMenuButton(player, 35), "Filler menu rejected 2D axis cycle");
        helper.assertTrue(menu.clickMenuButton(player, 36), "Filler menu rejected 2D rotation");
        helper.assertTrue(menu.clickMenuButton(player, 32), "Filler menu rejected 2D hollow toggle");
        helper.assertTrue(filler.shapeAxis() != oldAxis && filler.shapeRotation() == 1 && !filler.hollow(),
                "Filler menu selected the wrong 2D parameters");
        var restored = reloadBuildersFiller(helper, fillerPos, filler);
        helper.assertTrue(restored.pattern() == buildcraft.builders.FillerPattern.SQUARE
                        && restored.shapeAxis() == filler.shapeAxis()
                        && restored.shapeRotation() == 1 && !restored.hollow(),
                "reloaded Filler lost its 2D parameters");
        helper.succeed();
    }

    private static int countShape2d(buildcraft.builders.FillerPattern pattern, BlockPos min, BlockPos max,
                                    Direction.Axis axis, int rotation, boolean hollow) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (buildcraft.builders.FillerShape2d.includes(pattern, pos, min, max, axis, rotation, hollow)) count++;
        }
        return count;
    }

    private static void tickFiller(GameTestHelper helper, BlockPos pos,
            buildcraft.builders.block.entity.FillerBlockEntity filler, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            buildcraft.builders.block.entity.FillerBlockEntity.tick(helper.getLevel(), pos,
                    helper.getLevel().getBlockState(pos), filler);
        }
    }

    private static int countBlocks(GameTestHelper helper, BlockPos min, BlockPos max, Block block) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (helper.getLevel().getBlockState(pos).is(block)) count++;
        }
        return count;
    }

    private static buildcraft.builders.block.entity.FillerBlockEntity reloadBuildersFiller(
            GameTestHelper helper, BlockPos pos,
            buildcraft.builders.block.entity.FillerBlockEntity filler) {
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                pos, filler.getBlockState(), filler.saveWithFullMetadata(helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());
        if (!(loaded instanceof buildcraft.builders.block.entity.FillerBlockEntity restored)) {
            throw new IllegalStateException("Filler block entity did not reload from its saved tag");
        }
        return restored;
    }

    private static void siliconChipsets(GameTestHelper helper) {
        java.util.Set<buildcraft.silicon.ChipsetType> found = java.util.EnumSet.noneOf(
            buildcraft.silicon.ChipsetType.class);
        for (buildcraft.silicon.ChipsetType type : buildcraft.silicon.ChipsetType.values()) {
            ItemStack stack = buildcraft.silicon.BCSiliconItems.chipset(type);
            helper.assertTrue(stack.is(buildcraft.silicon.BCSiliconItems.REDSTONE_CHIPSET.get()),
                "chipset subtype changed historical item registry id");
            var decoded = stack.get(buildcraft.silicon.BCSiliconDataComponents.CHIPSET_TYPE.get());
            helper.assertValueEqual(type, decoded, "chipset component subtype");
            helper.assertTrue(found.add(decoded), "duplicate chipset subtype");
            helper.assertTrue(!stack.getHoverName().getString().isBlank(), "chipset localized name missing");
        }
        helper.assertValueEqual(5, found.size(), "wrong chipset subtype count");
        helper.succeed();
    }

    private static void siliconGateItems(GameTestHelper helper) {
        int variants = 0;
        for (var material : buildcraft.silicon.gate.GateMaterial.values()) {
            for (var logic : buildcraft.silicon.gate.GateLogic.values()) {
                for (var modifier : buildcraft.silicon.gate.GateModifier.values()) {
                    if (material == buildcraft.silicon.gate.GateMaterial.CLAY_BRICK
                            && (logic != buildcraft.silicon.gate.GateLogic.AND
                            || modifier != buildcraft.silicon.gate.GateModifier.NO_MODIFIER)) continue;
                    ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(material, logic, modifier);
                    helper.assertTrue(gate.is(buildcraft.silicon.BCSiliconItems.PLUG_GATE.get()),
                            "gate variant changed registry item");
                    helper.assertValueEqual(material,
                            gate.get(buildcraft.silicon.BCSiliconDataComponents.GATE_MATERIAL.get()),
                            "gate material component changed");
                    helper.assertValueEqual(logic,
                            gate.get(buildcraft.silicon.BCSiliconDataComponents.GATE_LOGIC.get()),
                            "gate logic component changed");
                    helper.assertValueEqual(modifier,
                            gate.get(buildcraft.silicon.BCSiliconDataComponents.GATE_MODIFIER.get()),
                            "gate modifier component changed");
                    helper.assertValueEqual(material.slots() / modifier.slotDivisor(),
                            buildcraft.silicon.item.GateItem.slots(gate), "gate slot contract changed");
                    helper.assertTrue(!gate.getHoverName().getString().isBlank(), "gate name missing");
                    variants++;
                }
            }
        }
        helper.assertValueEqual(25, variants, "wrong gate variant count");

        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY,
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.REDSTONE), new ItemStack(Items.IRON_INGOT),
                ItemStack.EMPTY, new ItemStack(Items.COBBLESTONE), ItemStack.EMPTY));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.silicon.BCSiliconItems.PLUG_GATE.get()),
                "iron gate recipe returned wrong item");
        helper.assertValueEqual(buildcraft.silicon.gate.GateMaterial.IRON,
                crafted.get(buildcraft.silicon.BCSiliconDataComponents.GATE_MATERIAL.get()),
                "iron gate recipe lost material component");
        helper.assertValueEqual(buildcraft.silicon.gate.GateLogic.AND,
                crafted.get(buildcraft.silicon.BCSiliconDataComponents.GATE_LOGIC.get()),
                "iron gate recipe lost logic component");
        helper.succeed();
    }

    private static void siliconLaserAssembly(GameTestHelper helper) {
        BlockPos laserPos = helper.absolutePos(new BlockPos(1, 2, 2));
        BlockPos tablePos = helper.absolutePos(new BlockPos(4, 2, 2));
        helper.getLevel().setBlock(laserPos, buildcraft.silicon.BCSiliconBlocks.LASER.get()
            .defaultBlockState().setValue(buildcraft.silicon.block.LaserBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        helper.getLevel().setBlock(tablePos, buildcraft.silicon.BCSiliconBlocks.ASSEMBLY_TABLE.get()
            .defaultBlockState(), Block.UPDATE_ALL);
        var laser = (buildcraft.silicon.block.entity.LaserBlockEntity) helper.getLevel().getBlockEntity(laserPos);
        var table = (buildcraft.silicon.block.entity.AssemblyTableBlockEntity)
            helper.getLevel().getBlockEntity(tablePos);
        table.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.REDSTONE), 1);
        helper.assertValueEqual(10_000L * MjAPI.MJ, table.getRequiredLaserPower(),
            "assembly table requested wrong red chipset power");
        helper.assertValueEqual(0L, laser.mjReceiver().receivePower(516L * MjAPI.MJ, false),
            "laser rejected valid MJ input");
        buildcraft.silicon.block.entity.LaserBlockEntity.tick(
            helper.getLevel(), laserPos, helper.getLevel().getBlockState(laserPos), laser);
        helper.assertValueEqual(4L * MjAPI.MJ, table.storedLaserPower(),
            "laser did not transfer historical 4 MJ/t");
        helper.assertValueEqual(tablePos, laser.targetPos(), "laser selected wrong aligned target");
        helper.assertValueEqual(0L, table.receiveLaserPower(table.getRequiredLaserPower()),
            "assembly table rejected required laser power");
        buildcraft.silicon.block.entity.AssemblyTableBlockEntity.tick(
            helper.getLevel(), tablePos, helper.getLevel().getBlockState(tablePos), table);
        ItemStack chipset = ItemStack.EMPTY;
        for (int slot = 0; slot < table.inventory().size(); slot++) {
            if (table.inventory().getResource(slot).value() == buildcraft.silicon.BCSiliconItems.REDSTONE_CHIPSET.get()) {
                chipset = table.inventory().getResource(slot).toStack(table.inventory().getAmountAsInt(slot));
                break;
            }
        }
        helper.assertTrue(!chipset.isEmpty(), "assembly table did not produce chipset");
        helper.assertValueEqual(buildcraft.silicon.ChipsetType.RED,
            chipset.get(buildcraft.silicon.BCSiliconDataComponents.CHIPSET_TYPE.get()),
            "assembly table produced wrong chipset subtype");
        helper.assertValueEqual(0L, table.storedLaserPower(), "assembly table did not debit completed recipe power");
        for (int slot = 0; slot < table.inventory().size(); slot++) {
            table.inventory().set(slot, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        }
        table.setSelectedType(buildcraft.silicon.ChipsetType.DIAMOND);
        table.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.REDSTONE), 1);
        table.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.DIAMOND), 1);
        helper.assertValueEqual(80_000L * MjAPI.MJ, table.getRequiredLaserPower(),
            "assembly table ignored selected diamond recipe");
        table.receiveLaserPower(table.getRequiredLaserPower());
        buildcraft.silicon.block.entity.AssemblyTableBlockEntity.tick(
            helper.getLevel(), tablePos, helper.getLevel().getBlockState(tablePos), table);
        ItemStack diamondChipset = ItemStack.EMPTY;
        for (int slot = 0; slot < table.inventory().size(); slot++) {
            if (table.inventory().getResource(slot).value() == buildcraft.silicon.BCSiliconItems.REDSTONE_CHIPSET.get()) {
                diamondChipset = table.inventory().getResource(slot).toStack(table.inventory().getAmountAsInt(slot));
            }
        }
        helper.assertValueEqual(buildcraft.silicon.ChipsetType.DIAMOND,
            diamondChipset.get(buildcraft.silicon.BCSiliconDataComponents.CHIPSET_TYPE.get()),
            "assembly table selection fell back to red recipe");

        for (int slot = 0; slot < table.inventory().size(); slot++) {
            table.inventory().set(slot, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        }
        ItemStack baseGate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.OR,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        table.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(baseGate), 1);
        table.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.of(
                buildcraft.silicon.BCSiliconItems.chipset(buildcraft.silicon.ChipsetType.DIAMOND)), 1);
        helper.assertValueEqual(80_000L * MjAPI.MJ, table.getRequiredLaserPower(),
                "iron diamond gate modifier requested wrong power");
        table.receiveLaserPower(table.getRequiredLaserPower());
        buildcraft.silicon.block.entity.AssemblyTableBlockEntity.tick(
                helper.getLevel(), tablePos, helper.getLevel().getBlockState(tablePos), table);
        ItemStack upgradedGate = ItemStack.EMPTY;
        for (int slot = 0; slot < table.inventory().size(); slot++) {
            if (table.inventory().getResource(slot).value() == buildcraft.silicon.BCSiliconItems.PLUG_GATE.get()) {
                upgradedGate = table.inventory().getResource(slot).toStack(table.inventory().getAmountAsInt(slot));
            }
        }
        helper.assertValueEqual(buildcraft.silicon.gate.GateLogic.OR,
                upgradedGate.get(buildcraft.silicon.BCSiliconDataComponents.GATE_LOGIC.get()),
                "gate modifier assembly changed gate logic");
        helper.assertValueEqual(buildcraft.silicon.gate.GateModifier.DIAMOND,
                upgradedGate.get(buildcraft.silicon.BCSiliconDataComponents.GATE_MODIFIER.get()),
                "gate modifier assembly returned wrong modifier");

        for (int slot = 0; slot < table.inventory().size(); slot++) {
            table.inventory().set(slot, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        }
        ItemStack goldGate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.GOLD,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        table.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(goldGate), 1);
        table.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.of(
                buildcraft.silicon.BCSiliconItems.chipset(buildcraft.silicon.ChipsetType.QUARTZ)), 1);
        helper.assertValueEqual(140_000L * MjAPI.MJ, table.getRequiredLaserPower(),
                "gold quartz gate modifier requested wrong power");
        table.receiveLaserPower(table.getRequiredLaserPower());
        buildcraft.silicon.block.entity.AssemblyTableBlockEntity.tick(
                helper.getLevel(), tablePos, helper.getLevel().getBlockState(tablePos), table);
        ItemStack goldUpgrade = ItemStack.EMPTY;
        for (int slot = 0; slot < table.inventory().size(); slot++) {
            if (table.inventory().getResource(slot).value() == buildcraft.silicon.BCSiliconItems.PLUG_GATE.get()) {
                goldUpgrade = table.inventory().getResource(slot).toStack(table.inventory().getAmountAsInt(slot));
            }
        }
        helper.assertValueEqual(buildcraft.silicon.gate.GateMaterial.GOLD,
                goldUpgrade.get(buildcraft.silicon.BCSiliconDataComponents.GATE_MATERIAL.get()),
                "gold gate modifier assembly changed material");
        helper.assertValueEqual(buildcraft.silicon.gate.GateModifier.QUARTZ,
                goldUpgrade.get(buildcraft.silicon.BCSiliconDataComponents.GATE_MODIFIER.get()),
                "gold gate modifier assembly returned wrong modifier");

        for (int slot = 0; slot < table.inventory().size(); slot++) {
            table.inventory().set(slot, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        }
        table.setSelection(buildcraft.silicon.recipe.AssemblySelection.GATE_OR);
        table.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(
                buildcraft.silicon.BCSiliconItems.chipset(buildcraft.silicon.ChipsetType.GOLD)), 1);
        helper.assertValueEqual(80_000L * MjAPI.MJ, table.getRequiredLaserPower(),
                "gold OR gate requested wrong assembly power");
        table.receiveLaserPower(table.getRequiredLaserPower());
        buildcraft.silicon.block.entity.AssemblyTableBlockEntity.tick(
                helper.getLevel(), tablePos, helper.getLevel().getBlockState(tablePos), table);
        ItemStack assembledGate = ItemStack.EMPTY;
        for (int slot = 0; slot < table.inventory().size(); slot++) {
            if (table.inventory().getResource(slot).value() == buildcraft.silicon.BCSiliconItems.PLUG_GATE.get()) {
                assembledGate = table.inventory().getResource(slot).toStack(table.inventory().getAmountAsInt(slot));
            }
        }
        helper.assertValueEqual(buildcraft.silicon.gate.GateMaterial.GOLD,
                assembledGate.get(buildcraft.silicon.BCSiliconDataComponents.GATE_MATERIAL.get()),
                "gold gate assembly returned wrong material");
        helper.assertValueEqual(buildcraft.silicon.gate.GateLogic.OR,
                assembledGate.get(buildcraft.silicon.BCSiliconDataComponents.GATE_LOGIC.get()),
                "gold gate assembly ignored OR selection");

        for (int slot = 0; slot < table.inventory().size(); slot++) {
            table.inventory().set(slot, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        }
        table.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(Items.CLOCK), 1);
        helper.assertValueEqual(500L * MjAPI.MJ, table.getRequiredLaserPower(),
                "timer plug requested wrong assembly power");
        table.receiveLaserPower(table.getRequiredLaserPower());
        buildcraft.silicon.block.entity.AssemblyTableBlockEntity.tick(
                helper.getLevel(), tablePos, helper.getLevel().getBlockState(tablePos), table);
        boolean timerFound = false;
        for (int slot = 0; slot < table.inventory().size(); slot++) {
            if (table.inventory().getResource(slot).value() == buildcraft.silicon.BCSiliconItems.PLUG_TIMER.get()) {
                timerFound = true;
            }
        }
        helper.assertTrue(timerFound, "assembly table did not produce timer plug");

        var laserRecipe = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
            new ItemStack(Items.REDSTONE), new ItemStack(Items.REDSTONE), new ItemStack(Items.OBSIDIAN),
            new ItemStack(Items.REDSTONE), new ItemStack(Items.DIAMOND), new ItemStack(Items.DIAMOND),
            new ItemStack(Items.REDSTONE), new ItemStack(Items.REDSTONE), new ItemStack(Items.OBSIDIAN)
        ));
        ItemStack craftedLaser = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
            net.minecraft.world.item.crafting.RecipeType.CRAFTING, laserRecipe, helper.getLevel()
        ).orElseThrow().value().assemble(laserRecipe);
        helper.assertTrue(craftedLaser.is(buildcraft.silicon.BCSiliconItems.LASER.get()),
            "laser recipe returned wrong item");
        var laserDrops = Block.getDrops(helper.getLevel().getBlockState(laserPos), helper.getLevel(), laserPos, laser);
        var tableDrops = Block.getDrops(helper.getLevel().getBlockState(tablePos), helper.getLevel(), tablePos, table);
        helper.assertTrue(laserDrops.size() == 1 && laserDrops.getFirst().is(buildcraft.silicon.BCSiliconItems.LASER.get()),
            "laser returned wrong loot");
        helper.assertTrue(tableDrops.size() == 1
            && tableDrops.getFirst().is(buildcraft.silicon.BCSiliconItems.ASSEMBLY_TABLE.get()),
            "assembly table returned wrong loot");
        helper.succeed();
    }

    private static void siliconIntegrationTable(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlock(pos,
                buildcraft.silicon.BCSiliconBlocks.INTEGRATION_TABLE.get().defaultBlockState(), Block.UPDATE_ALL);
        var table = (buildcraft.silicon.block.entity.IntegrationTableBlockEntity)
                helper.getLevel().getBlockEntity(pos);
        var handler = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, pos, Direction.NORTH);
        helper.assertTrue(handler != null, "integration table item capability missing");
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.GOLD,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.QUARTZ);
        var gateResource = net.neoforged.neoforge.transfer.item.ItemResource.of(gate);
        var redChipset = net.neoforged.neoforge.transfer.item.ItemResource.of(
                buildcraft.silicon.BCSiliconItems.chipset(buildcraft.silicon.ChipsetType.RED));
        var diamondChipset = net.neoforged.neoforge.transfer.item.ItemResource.of(
                buildcraft.silicon.BCSiliconItems.chipset(buildcraft.silicon.ChipsetType.DIAMOND));
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(1, handler.insert(0, gateResource, 1, transaction),
                    "integration table rejected gate target");
            helper.assertValueEqual(1, handler.insert(1, redChipset, 1, transaction),
                    "integration table rejected red chipset integration");
            helper.assertValueEqual(0, handler.insert(2, diamondChipset, 1, transaction),
                    "integration table accepted unrelated chipset");
            helper.assertValueEqual(0, handler.extract(0, gateResource, 1, transaction),
                    "integration table allowed target extraction");
            transaction.commit();
        }
        ItemStack preview = table.preview();
        helper.assertValueEqual(buildcraft.silicon.gate.GateLogic.OR,
                preview.get(buildcraft.silicon.BCSiliconDataComponents.GATE_LOGIC.get()),
                "integration preview did not toggle gate logic");
        helper.assertValueEqual(buildcraft.silicon.gate.GateModifier.QUARTZ,
                preview.get(buildcraft.silicon.BCSiliconDataComponents.GATE_MODIFIER.get()),
                "integration preview lost gate modifier");
        helper.assertValueEqual(25_000L * MjAPI.MJ, table.getRequiredLaserPower(),
                "integration table requested wrong power");
        helper.assertValueEqual(0L, table.receiveLaserPower(table.getRequiredLaserPower()),
                "integration table rejected laser power");
        buildcraft.silicon.block.entity.IntegrationTableBlockEntity.tick(
                helper.getLevel(), pos, helper.getLevel().getBlockState(pos), table);
        helper.assertValueEqual(0L, table.storedLaserPower(), "integration table did not debit power");
        helper.assertValueEqual(0, table.target().getAmountAsInt(0), "integration table did not consume gate");
        helper.assertValueEqual(0, table.integrations().getAmountAsInt(0),
                "integration table did not consume red chipset");
        ItemStack output = table.result().getResource(0).toStack(table.result().getAmountAsInt(0));
        helper.assertValueEqual(buildcraft.silicon.gate.GateMaterial.GOLD,
                output.get(buildcraft.silicon.BCSiliconDataComponents.GATE_MATERIAL.get()),
                "integration output lost gate material");
        helper.assertValueEqual(buildcraft.silicon.gate.GateLogic.OR,
                output.get(buildcraft.silicon.BCSiliconDataComponents.GATE_LOGIC.get()),
                "integration output returned wrong logic");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(1, handler.extract(9,
                    net.neoforged.neoforge.transfer.item.ItemResource.of(output), 1, transaction),
                    "integration table rejected output extraction");
            transaction.commit();
        }

        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.OBSIDIAN), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.OBSIDIAN),
                new ItemStack(Items.OBSIDIAN),
                buildcraft.silicon.BCSiliconItems.chipset(buildcraft.silicon.ChipsetType.IRON),
                new ItemStack(Items.OBSIDIAN), new ItemStack(Items.OBSIDIAN),
                new ItemStack(BCCoreItems.GEAR_DIAMOND.get()), new ItemStack(Items.OBSIDIAN)));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.silicon.BCSiliconItems.INTEGRATION_TABLE.get()),
                "integration table recipe returned wrong item");
        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos, table);
        helper.assertValueEqual(1, drops.size(), "integration table returned wrong drop count");
        helper.assertTrue(drops.getFirst().is(buildcraft.silicon.BCSiliconItems.INTEGRATION_TABLE.get()),
                "integration table returned wrong drop");
        helper.succeed();
    }

    private static void siliconPipeAttachments(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 2, 3);
        BlockPos pos = helper.absolutePos(relative);
        helper.getLevel().setBlock(pos, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get()
                .defaultBlockState().setValue(buildcraft.transport.block.PipeHolderBlock.TYPE,
                        buildcraft.transport.PipeType.COBBLESTONE_ITEM), Block.UPDATE_ALL);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(pos);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.NETHER_BRICK,
                buildcraft.silicon.gate.GateLogic.OR,
                buildcraft.silicon.gate.GateModifier.DIAMOND);
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.REDSTONE_ACTIVE,
                                buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT),
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT))));
        helper.assertTrue(buildcraft.silicon.BCSiliconItems.PLUG_GATE.get()
                        .useOn(useContext(helper, player, gate, relative)).consumesAction(),
                "gate item did not install on pipe side");
        helper.assertTrue(gate.isEmpty(), "gate installation did not consume survival stack");
        ItemStack installed = holder.attachment(Direction.UP);
        helper.assertValueEqual(buildcraft.silicon.gate.GateMaterial.NETHER_BRICK,
                installed.get(buildcraft.silicon.BCSiliconDataComponents.GATE_MATERIAL.get()),
                "pipe attachment lost gate material");
        helper.assertValueEqual(buildcraft.silicon.gate.GateModifier.DIAMOND,
                installed.get(buildcraft.silicon.BCSiliconDataComponents.GATE_MODIFIER.get()),
                "pipe attachment lost gate modifier");
        ItemStack copier = new ItemStack(buildcraft.silicon.BCSiliconItems.GATE_COPIER.get());
        helper.assertTrue(buildcraft.silicon.BCSiliconItems.GATE_COPIER.get()
                        .useOn(useContext(helper, player, copier, relative)).consumesAction(),
                "gate copier did not copy attached program");
        helper.assertValueEqual(2,
                copier.get(buildcraft.silicon.BCSiliconDataComponents.COPIED_GATE_PROGRAM.get()).rules().size(),
                "gate copier copied wrong rule count");
        var gateMenu = new buildcraft.silicon.menu.GateMenu(31, player.getInventory(), pos, Direction.UP);
        helper.assertValueEqual(2, gateMenu.ruleSlots(), "gate menu ignored modifier slot divisor");
        helper.assertValueEqual(buildcraft.silicon.gate.GateLogic.OR, gateMenu.logic(),
                "gate menu synchronized wrong logic");
        helper.assertTrue(gateMenu.clickMenuButton(player, 0), "gate menu rejected trigger cycle");
        helper.assertValueEqual(buildcraft.silicon.gate.GateTrigger.REDSTONE_INACTIVE,
                holder.attachment(Direction.UP).get(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get())
                        .rules().getFirst().trigger(), "gate menu did not persist cycled trigger");
        helper.assertTrue(gateMenu.clickMenuButton(player, 3), "gate menu rejected row clear");
        helper.assertValueEqual(1,
                holder.attachment(Direction.UP).get(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get())
                        .rules().size(), "gate menu did not persist row clear");
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), pos, helper.getLevel().getBlockState(pos), holder);
        helper.assertValueEqual(15, helper.getLevel().getSignal(pos, Direction.NORTH),
                "OR gate did not activate redstone output from true rule");
        ItemStack timer = new ItemStack(buildcraft.silicon.BCSiliconItems.PLUG_TIMER.get());
        helper.assertTrue(!buildcraft.silicon.BCSiliconItems.PLUG_TIMER.get()
                        .useOn(useContext(helper, player, timer, relative)).consumesAction(),
                "pipe accepted a second attachment on one side");
        helper.assertValueEqual(1, timer.getCount(), "rejected attachment consumed item");
        ItemStack removed = holder.takeAttachment(Direction.UP);
        helper.assertTrue(removed.is(buildcraft.silicon.BCSiliconItems.PLUG_GATE.get()),
                "pipe returned wrong attachment");
        ItemStack basicGate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.CLAY_BRICK,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.UP, basicGate),
                "pipe rejected copier paste target");
        helper.assertTrue(buildcraft.silicon.BCSiliconItems.GATE_COPIER.get()
                        .useOn(useContext(helper, player, copier, relative)).consumesAction(),
                "gate copier did not paste attached program");
        helper.assertValueEqual(1,
                holder.attachment(Direction.UP).get(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get())
                        .rules().size(), "gate copier did not truncate to target slot count");
        holder.takeAttachment(Direction.UP);
        helper.assertTrue(buildcraft.silicon.BCSiliconItems.PLUG_TIMER.get()
                        .useOn(useContext(helper, player, timer, relative)).consumesAction(),
                "timer item did not install after side was cleared");
        helper.assertTrue(holder.attachment(Direction.UP).is(buildcraft.silicon.BCSiliconItems.PLUG_TIMER.get()),
                "pipe stored wrong utility attachment");
        helper.succeed();
    }

    private static void transportPipeShellColors(GameTestHelper helper) {
        BlockPos firstPos = new BlockPos(1, 2, 1);
        BlockPos secondPos = firstPos.east();
        BlockState pipeState = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState()
                .setValue(buildcraft.transport.block.PipeHolderBlock.TYPE,
                        buildcraft.transport.PipeType.COBBLESTONE_ITEM);
        helper.setBlock(firstPos, pipeState);
        helper.setBlock(secondPos, pipeState);
        var first = helper.getBlockEntity(firstPos,
                buildcraft.transport.block.entity.PipeHolderBlockEntity.class);
        var second = helper.getBlockEntity(secondPos,
                buildcraft.transport.block.entity.PipeHolderBlockEntity.class);

        first.setShellColor(net.minecraft.world.item.DyeColor.RED);
        second.setShellColor(net.minecraft.world.item.DyeColor.BLUE);
        helper.assertFalse(helper.getBlockState(firstPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST),
                "Differently coloured pipes connected");

        second.setShellColor(net.minecraft.world.item.DyeColor.RED);
        helper.assertTrue(helper.getBlockState(firstPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST),
                "Matching coloured pipes did not connect");

        second.setShellColor(null);
        helper.assertTrue(helper.getBlockState(firstPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST),
                "Uncoloured pipe did not connect to a coloured pipe");

        ItemStack colored = buildcraft.transport.BCTransportItems.PIPE_COBBLE_ITEM.get()
                .createStack(net.minecraft.world.item.DyeColor.GREEN);
        second.applyComponentsFromItemStack(colored);
        helper.assertValueEqual(net.minecraft.world.item.DyeColor.GREEN, second.shellColor(),
                "Pipe item colour was not applied to its block entity");
        helper.assertFalse(helper.getBlockState(firstPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST),
                "Applying a pipe item colour did not refresh connections");

        var drops = Block.getDrops(helper.getBlockState(secondPos), helper.getLevel(),
                helper.absolutePos(secondPos), second);
        helper.assertValueEqual(1, drops.size(), "Coloured pipe drop count");
        helper.assertValueEqual(net.minecraft.world.item.DyeColor.GREEN,
                buildcraft.transport.item.PipeItem.color(drops.getFirst()),
                "Coloured pipe drop lost its colour");
        helper.assertValueEqual(net.minecraft.world.item.DyeColor.WHITE, second.pipeColor(),
                "Shell colour changed the Lapis/Daizuli behaviour colour");

        var coloredRecipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 1, java.util.List.of(
                new ItemStack(Items.COBBLESTONE), new ItemStack(Items.BLUE_STAINED_GLASS),
                new ItemStack(Items.COBBLESTONE)));
        ItemStack coloredResult = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, coloredRecipeInput, helper.getLevel())
                .orElseThrow().value().assemble(coloredRecipeInput);
        helper.assertTrue(coloredResult.is(buildcraft.transport.BCTransportItems.PIPE_COBBLE_ITEM.get()),
                "Stained glass did not craft the matching pipe type");
        helper.assertValueEqual(8, coloredResult.getCount(), "Coloured pipe recipe output count");
        helper.assertValueEqual(net.minecraft.world.item.DyeColor.BLUE,
                buildcraft.transport.item.PipeItem.color(coloredResult), "Recipe lost stained-glass colour");

        var upgradeInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                coloredResult.copyWithCount(1), new ItemStack(buildcraft.transport.BCTransportItems.WATERPROOF.get())));
        ItemStack fluidResult = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, upgradeInput, helper.getLevel())
                .orElseThrow().value().assemble(upgradeInput);
        helper.assertTrue(fluidResult.is(buildcraft.transport.BCTransportItems.PIPE_COBBLE_FLUID.get()),
                "Coloured item pipe did not upgrade to fluid pipe");
        helper.assertValueEqual(net.minecraft.world.item.DyeColor.BLUE,
                buildcraft.transport.item.PipeItem.color(fluidResult), "Upgrade lost pipe colour");

        var undoInput = net.minecraft.world.item.crafting.CraftingInput.of(1, 1,
                java.util.List.of(fluidResult));
        ItemStack undoResult = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, undoInput, helper.getLevel())
                .orElseThrow().value().assemble(undoInput);
        helper.assertTrue(undoResult.is(buildcraft.transport.BCTransportItems.PIPE_COBBLE_ITEM.get()),
                "Fluid pipe undo did not restore item pipe");
        helper.assertValueEqual(net.minecraft.world.item.DyeColor.BLUE,
                buildcraft.transport.item.PipeItem.color(undoResult), "Undo recipe lost pipe colour");

        ItemStack plainFluid = new ItemStack(buildcraft.transport.BCTransportItems.PIPE_COBBLE_FLUID.get());
        var plainUndoInput = net.minecraft.world.item.crafting.CraftingInput.of(1, 1,
                java.util.List.of(plainFluid));
        ItemStack plainUndoResult = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, plainUndoInput, helper.getLevel())
                .orElseThrow().value().assemble(plainUndoInput);
        helper.assertTrue(plainUndoResult.is(buildcraft.transport.BCTransportItems.PIPE_COBBLE_ITEM.get()),
                "Uncoloured fluid pipe undo recipe was not restored");
        helper.assertTrue(buildcraft.transport.item.PipeItem.color(plainUndoResult) == null,
                "Uncoloured undo recipe invented a pipe colour");

        var unsupportedRfInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_GOLD_POWER.get()),
                new ItemStack(Items.REDSTONE)));
        helper.assertTrue(buildcraft.transport.recipe.PipeUpgradeRecipe.INSTANCE
                        .assemble(unsupportedRfInput).isEmpty(),
                "Gold power pipe gained an RF upgrade absent from the historical recipe registry");
        helper.succeed();
    }

    private static void siliconSensorTimerPlugs(GameTestHelper helper) {
        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState(), Block.UPDATE_ALL);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);

        helper.getLevel().setBlock(pipePos.east(), Blocks.GLOWSTONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(holder.installAttachment(Direction.EAST,
                new ItemStack(buildcraft.silicon.BCSiliconItems.PLUG_LIGHT_SENSOR.get())),
                "light sensor could not be installed");
        helper.assertFalse(holder.attachmentBlocksConnection(Direction.EAST),
                "light sensor incorrectly blocked its pipe side");
        helper.runAfterDelay(2, () -> {
        helper.assertTrue(helper.getLevel().getMaxLocalRawBrightness(pipePos.east()) >= 8,
                "light source did not illuminate the Light Sensor side");
        ItemStack lightGate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        lightGate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.LIGHT_HIGH,
                                buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT))));
        helper.assertTrue(holder.installAttachment(Direction.UP, lightGate),
                "light-sensor test gate could not be installed");
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), pipePos, helper.getLevel().getBlockState(pipePos), holder);
        helper.assertTrue(holder.gateRedstoneOutput(),
                "bright Light Sensor trigger did not activate gate output");

        holder.takeAttachment(Direction.EAST);
        helper.assertTrue(holder.installAttachment(Direction.EAST,
                new ItemStack(buildcraft.silicon.BCSiliconItems.PLUG_TIMER.get())),
                "timer could not be installed");
        helper.assertTrue(holder.attachmentBlocksConnection(Direction.EAST),
                "timer did not block its historical pipe side");
        helper.assertTrue(buildcraft.silicon.item.GateItem.timerActive(200L, 5),
                "five-second Timer trigger missed its pulse boundary");
        helper.assertFalse(buildcraft.silicon.item.GateItem.timerActive(201L, 5),
                "five-second Timer trigger remained active outside its pulse boundary");
        helper.succeed();
        });
    }

    private static void siliconPulsarGate(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState wood = block.defaultBlockState().setValue(buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.WOOD_ITEM);
        BlockState cobble = block.defaultBlockState().setValue(buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_ITEM);
        BlockPos sourcePos = helper.absolutePos(new BlockPos(1, 1, 2));
        BlockPos woodPos = sourcePos.east();
        BlockPos cobblePos = woodPos.east();
        BlockPos targetPos = cobblePos.east();
        helper.getLevel().setBlock(sourcePos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(woodPos, wood, Block.UPDATE_ALL);
        helper.getLevel().setBlock(cobblePos, cobble, Block.UPDATE_ALL);
        helper.getLevel().setBlock(targetPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        var source = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(sourcePos);
        source.setItem(0, new ItemStack(Items.COAL));
        tickPipes(helper, 1, woodPos);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(woodPos);
        helper.assertValueEqual(Direction.WEST, holder.extractionDirection(),
                "pulsar test wood pipe did not face source");
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.PULSAR_CONSTANT,
                                java.util.Optional.of(Direction.DOWN)))));
        helper.assertTrue(holder.installAttachment(Direction.UP, gate), "pulsar test rejected gate");
        helper.assertTrue(holder.installAttachment(Direction.DOWN,
                new ItemStack(buildcraft.silicon.BCSiliconItems.PLUG_PULSAR.get())),
                "pulsar test rejected pulsar");
        tickPipes(helper, 19, woodPos);
        helper.assertValueEqual(1, containerCount(source, Items.COAL),
                "pulsar emitted before its 20 tick period");
        tickPipes(helper, 1, woodPos);
        helper.assertValueEqual(0, containerCount(source, Items.COAL),
                "pulsar did not feed 1 MJ into wood pipe");
        helper.assertValueEqual(1, holder.travellingCount(), "pulsar extraction did not enter pipe flow");

        holder.attachment(Direction.UP).set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.PULSAR_SINGLE,
                                java.util.Optional.of(Direction.DOWN)))));
        source.setItem(0, new ItemStack(Items.COAL));
        tickPipes(helper, 19, woodPos);
        helper.assertValueEqual(1, containerCount(source, Items.COAL),
                "single pulsar emitted before its 20 tick period");
        tickPipes(helper, 1, woodPos);
        helper.assertValueEqual(0, containerCount(source, Items.COAL),
                "single pulsar did not emit once on activation");
        source.setItem(0, new ItemStack(Items.COAL));
        tickPipes(helper, 40, woodPos);
        helper.assertValueEqual(1, containerCount(source, Items.COAL),
                "single pulsar repeated while its trigger stayed active");

        ItemStack pulsarStack = holder.attachment(Direction.DOWN);
        var pulsarItem = (buildcraft.silicon.item.PulsarItem) pulsarStack.getItem();
        pulsarItem.toggleManual(holder, pulsarStack);
        helper.assertTrue(pulsarStack.getOrDefault(
                        buildcraft.silicon.BCSiliconDataComponents.PULSAR_MANUALLY_ENABLED.get(), false),
                "manual pulsar toggle did not persist its enabled component");
        ItemStack removedPulsar = holder.takeAttachment(Direction.DOWN);
        helper.assertTrue(removedPulsar.getOrDefault(
                        buildcraft.silicon.BCSiliconDataComponents.PULSAR_MANUALLY_ENABLED.get(), false),
                "removing the pulsar lost its manual state component");
        helper.assertTrue(holder.installAttachment(Direction.DOWN, removedPulsar),
                "manually enabled pulsar could not be reinstalled");
        pulsarStack = holder.attachment(Direction.DOWN);
        tickPipes(helper, 20, woodPos);
        helper.assertValueEqual(0, containerCount(source, Items.COAL),
                "manually enabled pulsar did not feed the wood pipe");
        pulsarItem.toggleManual(holder, pulsarStack);
        helper.assertFalse(pulsarStack.getOrDefault(
                        buildcraft.silicon.BCSiliconDataComponents.PULSAR_MANUALLY_ENABLED.get(), false),
                "manual pulsar toggle did not persist its disabled component");
        source.setItem(0, new ItemStack(Items.COAL));
        tickPipes(helper, 40, woodPos);
        helper.assertValueEqual(1, containerCount(source, Items.COAL),
                "manually disabled pulsar continued feeding the wood pipe");
        helper.succeed();
    }

    private static void siliconPlaceholderTables(GameTestHelper helper) {
        BlockPos chargingRelative = new BlockPos(2, 8, 2);
        BlockPos programmingRelative = chargingRelative.east(2);
        helper.setBlock(chargingRelative, buildcraft.silicon.BCSiliconBlocks.CHARGING_TABLE.get());
        helper.setBlock(programmingRelative, buildcraft.silicon.BCSiliconBlocks.PROGRAMMING_TABLE.get());
        var charging = helper.getLevel().getBlockEntity(helper.absolutePos(chargingRelative));
        var programming = helper.getLevel().getBlockEntity(helper.absolutePos(programmingRelative));
        helper.assertTrue(charging instanceof buildcraft.silicon.block.entity.ChargingTableBlockEntity,
                "Charging Table created wrong block entity");
        helper.assertTrue(programming instanceof buildcraft.silicon.block.entity.ProgrammingTableBlockEntity,
                "Programming Table created wrong block entity");
        var chargingTarget = (buildcraft.api.mj.ILaserTarget) charging;
        var programmingTarget = (buildcraft.api.mj.ILaserTarget) programming;
        helper.assertValueEqual(0L, chargingTarget.getRequiredLaserPower(), "Charging Table placeholder requested power");
        helper.assertValueEqual(0L, programmingTarget.getRequiredLaserPower(), "Programming Table placeholder requested power");
        helper.assertValueEqual(3 * MjAPI.MJ, chargingTarget.receiveLaserPower(3 * MjAPI.MJ),
                "Charging Table placeholder consumed laser power");
        helper.assertValueEqual(3 * MjAPI.MJ, programmingTarget.receiveLaserPower(3 * MjAPI.MJ),
                "Programming Table placeholder consumed laser power");
        for (BlockPos relative : java.util.List.of(chargingRelative, programmingRelative)) {
            BlockPos pos = helper.absolutePos(relative);
            BlockState state = helper.getBlockState(relative);
            double height = state.getShape(helper.getLevel(), pos).bounds().maxY;
            helper.assertTrue(Math.abs(height - 9 / 16.0) < 1.0E-9, "Laser table did not retain 9/16 height");
            var drops = Block.getDrops(state, helper.getLevel(), pos, helper.getLevel().getBlockEntity(pos));
            helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(state.getBlock().asItem())),
                    "Placeholder laser table did not drop itself");
        }
        helper.succeed();
    }

    private static void siliconAdvancedCraftingTable(GameTestHelper helper) {
        BlockPos tablePos = helper.absolutePos(new BlockPos(4, 2, 2));
        BlockPos laserPos = helper.absolutePos(new BlockPos(1, 2, 2));
        helper.getLevel().setBlock(tablePos,
                buildcraft.silicon.BCSiliconBlocks.ADVANCED_CRAFTING_TABLE.get().defaultBlockState(),
                Block.UPDATE_ALL);
        helper.getLevel().setBlock(laserPos, buildcraft.silicon.BCSiliconBlocks.LASER.get().defaultBlockState()
                .setValue(buildcraft.silicon.block.LaserBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        var table = (buildcraft.silicon.block.entity.AdvancedCraftingTableBlockEntity)
                helper.getLevel().getBlockEntity(tablePos);
        var laser = (buildcraft.silicon.block.entity.LaserBlockEntity)
                helper.getLevel().getBlockEntity(laserPos);
        var planks = net.neoforged.neoforge.transfer.item.ItemResource.of(Items.OAK_PLANKS);
        table.blueprint().set(0, planks, 1);
        table.blueprint().set(3, planks, 1);

        var handler = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, tablePos, Direction.NORTH);
        helper.assertTrue(handler != null, "advanced crafting table item capability missing");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(2, handler.insert(0, planks, 2, transaction),
                    "advanced crafting table rejected blueprint material");
            helper.assertValueEqual(0, handler.insert(1,
                    net.neoforged.neoforge.transfer.item.ItemResource.of(Items.COBBLESTONE), 1, transaction),
                    "advanced crafting table accepted unrelated material");
            transaction.commit();
        }
        helper.assertValueEqual(500L * MjAPI.MJ, table.getRequiredLaserPower(),
                "advanced crafting table requested wrong laser power");
        helper.assertValueEqual(0L, laser.mjReceiver().receivePower(516L * MjAPI.MJ, false),
                "laser rejected advanced crafting table test power");
        buildcraft.silicon.block.entity.LaserBlockEntity.tick(
                helper.getLevel(), laserPos, helper.getLevel().getBlockState(laserPos), laser);
        helper.assertValueEqual(4L * MjAPI.MJ, table.storedLaserPower(),
                "laser did not transfer 4 MJ to advanced crafting table");
        helper.assertValueEqual(tablePos, laser.targetPos(),
                "laser did not target advanced crafting table");
        helper.assertValueEqual(0L, table.receiveLaserPower(table.getRequiredLaserPower()),
                "advanced crafting table rejected remaining laser power");
        buildcraft.silicon.block.entity.AdvancedCraftingTableBlockEntity.tick(
                helper.getLevel(), tablePos, helper.getLevel().getBlockState(tablePos), table);
        helper.assertTrue(table.results().getResource(0).value() == Items.STICK,
                "advanced crafting table produced wrong recipe output");
        helper.assertValueEqual(4, table.results().getAmountAsInt(0),
                "advanced crafting table produced wrong output count");
        helper.assertValueEqual(0L, table.storedLaserPower(),
                "advanced crafting table did not debit 500 MJ");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(0, handler.extract(0, planks, 1, transaction),
                    "advanced crafting table allowed material extraction");
            helper.assertValueEqual(4, handler.extract(15,
                    net.neoforged.neoforge.transfer.item.ItemResource.of(Items.STICK), 4, transaction),
                    "advanced crafting table rejected output extraction");
            transaction.commit();
        }

        for (int slot = 0; slot < 9; slot++)
            table.blueprint().set(slot, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
        var honey = net.neoforged.neoforge.transfer.item.ItemResource.of(Items.HONEY_BOTTLE);
        for (int slot : new int[] {0, 1, 3, 4}) table.blueprint().set(slot, honey, 1);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            for (int slot = 0; slot < 4; slot++) {
                helper.assertValueEqual(1, handler.insert(slot, honey, 1, transaction),
                        "advanced crafting table rejected container recipe material");
            }
            transaction.commit();
        }
        table.receiveLaserPower(table.getRequiredLaserPower());
        buildcraft.silicon.block.entity.AdvancedCraftingTableBlockEntity.tick(
                helper.getLevel(), tablePos, helper.getLevel().getBlockState(tablePos), table);
        helper.assertTrue(table.results().getResource(0).value() == Items.HONEY_BLOCK,
                "advanced crafting table produced wrong container recipe output");
        int bottles = 0;
        for (int slot = 0; slot < table.materials().size(); slot++) {
            if (table.materials().getResource(slot).value() == Items.GLASS_BOTTLE) {
                bottles += table.materials().getAmountAsInt(slot);
            }
        }
        helper.assertValueEqual(4, bottles,
                "advanced crafting table did not retain crafting remainders");

        ItemStack redChipset = buildcraft.silicon.BCSiliconItems.chipset(buildcraft.silicon.ChipsetType.RED);
        ItemStack diamondChipset = buildcraft.silicon.BCSiliconItems.chipset(buildcraft.silicon.ChipsetType.DIAMOND);
        java.util.function.Function<ItemStack, net.minecraft.world.item.crafting.CraftingInput> recipeInput = chipset ->
                net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                        new ItemStack(Items.OBSIDIAN), new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.OBSIDIAN),
                        new ItemStack(Items.OBSIDIAN), new ItemStack(Items.CHEST), new ItemStack(Items.OBSIDIAN),
                        new ItemStack(Items.OBSIDIAN), chipset, new ItemStack(Items.OBSIDIAN)));
        var redInput = recipeInput.apply(redChipset);
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, redInput, helper.getLevel())
                .orElseThrow().value().assemble(redInput);
        helper.assertTrue(crafted.is(buildcraft.silicon.BCSiliconItems.ADVANCED_CRAFTING_TABLE.get()),
                "advanced crafting table recipe returned wrong item");
        helper.assertTrue(helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING,
                recipeInput.apply(diamondChipset), helper.getLevel()).isEmpty(),
                "advanced crafting table recipe accepted a non-red chipset");
        var drops = Block.getDrops(helper.getLevel().getBlockState(tablePos), helper.getLevel(), tablePos, table);
        helper.assertValueEqual(1, drops.size(), "advanced crafting table returned wrong drop count");
        helper.assertTrue(drops.getFirst().is(buildcraft.silicon.BCSiliconItems.ADVANCED_CRAFTING_TABLE.get()),
                "advanced crafting table returned wrong drop");
        helper.succeed();
    }

    private static void transportWoodFluidPipe(GameTestHelper helper) {
        BlockPos tankPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos woodPos = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockPos cobblePos = helper.absolutePos(new BlockPos(2, 1, 0));
        BlockState engineState = BCCoreBlocks.ENGINE.get().defaultBlockState()
                .setValue(BlockEngine.ENGINE_TYPE, EnumEngineType.IRON);
        helper.getLevel().setBlock(tankPos, engineState, net.minecraft.world.level.block.Block.UPDATE_ALL);
        var pipeBlock = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        helper.getLevel().setBlock(woodPos, pipeBlock.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.WOOD_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(cobblePos, pipeBlock.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        var engine = (CombustionEngineBlockEntity) helper.getLevel().getBlockEntity(tankPos);
        var fuel = net.neoforged.neoforge.transfer.fluid.FluidResource.of(BCEnergyFluids.FUEL_LIGHT.get());
        engine.tanks().set(CombustionEngineBlockEntity.RESIDUE_TANK, fuel, 1_000);
        helper.assertTrue(helper.getLevel().getBlockState(woodPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.WEST), "wood fluid pipe did not connect to tank");
        helper.assertTrue(helper.getLevel().getBlockState(woodPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST), "wood fluid pipe did not connect to separated pipe");

        BlockPos otherWoodPos = helper.absolutePos(new BlockPos(4, 1, 0));
        BlockPos thirdWoodPos = helper.absolutePos(new BlockPos(5, 1, 0));
        BlockState woodState = pipeBlock.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.WOOD_FLUID);
        helper.getLevel().setBlock(otherWoodPos, woodState, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(thirdWoodPos, woodState, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.assertTrue(!helper.getLevel().getBlockState(otherWoodPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST), "wood fluid pipes connected to each other");

        var sealantInput = net.minecraft.world.item.crafting.CraftingInput.of(1, 1,
                java.util.List.of(new ItemStack(Items.SLIME_BALL)));
        ItemStack sealant = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, sealantInput, helper.getLevel())
                .orElseThrow().value().assemble(sealantInput);
        helper.assertTrue(sealant.is(buildcraft.transport.BCTransportItems.WATERPROOF.get()),
                "slime did not craft historical Pipe Sealant");
        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_WOOD_ITEM.get()), sealant));
        ItemStack recipeOutput = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(recipeOutput.is(buildcraft.transport.BCTransportItems.PIPE_WOOD_FLUID.get()),
                "wood fluid recipe returned wrong item");

        helper.runAfterDelay(2, () -> {
            var wood = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                    helper.getLevel().getBlockEntity(woodPos);
            helper.assertValueEqual(wood.extractionDirection(), net.minecraft.core.Direction.WEST,
                    "wood fluid pipe selected wrong extraction direction");
            helper.assertValueEqual(wood.mjReceiver().receivePower(100_000, false), 90_000L,
                    "wood fluid pipe returned paid extraction power");
            helper.assertValueEqual(engine.tanks().getAmountAsInt(CombustionEngineBlockEntity.RESIDUE_TANK), 990,
                    "wood fluid pipe extracted wrong amount");
            helper.assertValueEqual(wood.fluidBuffer().getAmountAsInt(0), 10,
                    "wood fluid pipe buffered wrong amount");
            var drops = net.minecraft.world.level.block.Block.getDrops(
                    helper.getLevel().getBlockState(woodPos), helper.getLevel(), woodPos, wood);
            helper.assertValueEqual(drops.size(), 1, "wood fluid pipe returned wrong drop count");
            helper.assertTrue(drops.getFirst().is(buildcraft.transport.BCTransportItems.PIPE_WOOD_FLUID.get()),
                    "wood fluid pipe returned wrong drop item");
            helper.succeed();
        });
    }

    private static void transportFastIsolatedFluidPipes(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos goldPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos stonePos = goldPos.east();
        helper.getLevel().setBlock(goldPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.GOLD_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(stonePos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.STONE_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.assertTrue(helper.getLevel().getBlockState(goldPos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST), "gold fluid pipe did not connect separated material");
        helper.assertValueEqual(buildcraft.transport.PipeType.GOLD_FLUID.fluidTransferRate(), 80,
                "gold fluid transfer rate");
        helper.assertValueEqual(buildcraft.transport.PipeType.SANDSTONE_FLUID.fluidTransferRate(), 20,
                "sandstone fluid transfer rate");
        var gold = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(goldPos);
        var stone = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(stonePos);
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);
        gold.fluidBuffer().set(0, water, 500);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), goldPos, helper.getLevel().getBlockState(goldPos), gold);
        helper.assertValueEqual(gold.fluidBuffer().getAmountAsInt(0), 420, "gold pipe retained wrong amount");
        helper.assertValueEqual(stone.fluidBuffer().getAmountAsInt(0), 80, "gold pipe transferred wrong amount");

        BlockPos sandstonePos = helper.absolutePos(new BlockPos(4, 2, 1));
        BlockPos cobblePos = sandstonePos.east();
        BlockPos tankPos = sandstonePos.south();
        helper.getLevel().setBlock(sandstonePos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.SANDSTONE_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(cobblePos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(tankPos, BCCoreBlocks.ENGINE.get().defaultBlockState().setValue(
                BlockEngine.ENGINE_TYPE, EnumEngineType.IRON), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.assertTrue(helper.getLevel().getBlockState(sandstonePos).getValue(
                buildcraft.transport.block.PipeHolderBlock.EAST), "sandstone fluid pipe did not connect to pipe");
        helper.assertTrue(!helper.getLevel().getBlockState(sandstonePos).getValue(
                buildcraft.transport.block.PipeHolderBlock.SOUTH), "sandstone fluid pipe connected to tank");
        helper.assertTrue(helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                sandstonePos, net.minecraft.core.Direction.SOUTH) == null,
                "sandstone fluid pipe exposed capability to tank");

        assertFluidUpgradeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_GOLD_ITEM.get(),
                buildcraft.transport.BCTransportItems.PIPE_GOLD_FLUID.get());
        assertFluidUpgradeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_SANDSTONE_ITEM.get(),
                buildcraft.transport.BCTransportItems.PIPE_SANDSTONE_FLUID.get());
        var goldDrops = net.minecraft.world.level.block.Block.getDrops(
                helper.getLevel().getBlockState(goldPos), helper.getLevel(), goldPos, gold);
        helper.assertTrue(goldDrops.size() == 1
                && goldDrops.getFirst().is(buildcraft.transport.BCTransportItems.PIPE_GOLD_FLUID.get()),
                "gold fluid pipe returned wrong drop");
        helper.succeed();
    }

    private static void assertFluidUpgradeRecipe(GameTestHelper helper, net.minecraft.world.item.Item inputPipe,
            net.minecraft.world.item.Item outputPipe) {
        var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                new ItemStack(inputPipe), new ItemStack(buildcraft.transport.BCTransportItems.WATERPROOF.get())));
        ItemStack output = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow().value().assemble(input);
        helper.assertTrue(output.is(outputPipe), "fluid upgrade recipe returned wrong item");
    }

    private static void transportIronFluidPipe(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos ironPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos westPos = ironPos.west();
        BlockPos eastPos = ironPos.east();
        helper.getLevel().setBlock(ironPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.IRON_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        BlockState cobble = block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_FLUID);
        helper.getLevel().setBlock(westPos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(eastPos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        var iron = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(ironPos);
        var west = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(westPos);
        var east = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(eastPos);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), ironPos, helper.getLevel().getBlockState(ironPos), iron);
        helper.assertTrue(iron.routingDirection() != null, "iron fluid pipe did not select output");
        while (iron.routingDirection() != net.minecraft.core.Direction.EAST) {
            helper.assertTrue(iron.rotatePipeDirection(), "iron fluid pipe failed to rotate output");
        }
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);
        var input = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                ironPos, net.minecraft.core.Direction.WEST);
        var output = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                ironPos, net.minecraft.core.Direction.EAST);
        helper.assertTrue(input != null && output != null, "iron fluid sided capability missing");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(input.insert(water, 200, transaction), 200,
                    "iron fluid input rejected fluid");
            helper.assertValueEqual(output.insert(water, 50, transaction), 0,
                    "iron fluid output accepted input");
            transaction.commit();
        }
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), ironPos, helper.getLevel().getBlockState(ironPos), iron);
        helper.assertValueEqual(west.fluidBuffer().getAmountAsInt(0), 0,
                "iron fluid pipe sent fluid to input side");
        helper.assertValueEqual(east.fluidBuffer().getAmountAsInt(0), 40,
                "iron fluid pipe sent wrong amount to output side");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(output.extract(water, 10, transaction), 10,
                    "iron fluid output did not permit extraction");
            transaction.commit();
        }
        helper.assertValueEqual(iron.fluidBuffer().getAmountAsInt(0), 150,
                "iron fluid output extraction changed wrong amount");
        assertFluidUpgradeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_IRON_ITEM.get(),
                buildcraft.transport.BCTransportItems.PIPE_IRON_FLUID.get());
        var drops = net.minecraft.world.level.block.Block.getDrops(
                helper.getLevel().getBlockState(ironPos), helper.getLevel(), ironPos, iron);
        helper.assertTrue(drops.size() == 1
                && drops.getFirst().is(buildcraft.transport.BCTransportItems.PIPE_IRON_FLUID.get()),
                "iron fluid pipe returned wrong drop");
        helper.succeed();
    }

    private static void transportClayVoidFluidPipes(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos clayPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos westPos = clayPos.west();
        BlockPos eastPos = clayPos.east();
        BlockPos tankPos = clayPos.south();
        BlockState cobble = block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_FLUID);
        helper.getLevel().setBlock(clayPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.CLAY_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(westPos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(eastPos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(tankPos, BCCoreBlocks.ENGINE.get().defaultBlockState().setValue(
                BlockEngine.ENGINE_TYPE, EnumEngineType.IRON), net.minecraft.world.level.block.Block.UPDATE_ALL);
        var clay = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(clayPos);
        var west = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(westPos);
        var east = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(eastPos);
        var tank = (CombustionEngineBlockEntity) helper.getLevel().getBlockEntity(tankPos);
        var fuel = net.neoforged.neoforge.transfer.fluid.FluidResource.of(BCEnergyFluids.FUEL_LIGHT.get());
        clay.fluidBuffer().set(0, fuel, 200);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), clayPos, helper.getLevel().getBlockState(clayPos), clay);
        helper.assertValueEqual(tank.tanks().getAmountAsInt(CombustionEngineBlockEntity.FUEL_TANK), 40,
                "clay fluid pipe did not prioritize tank");
        helper.assertValueEqual(west.fluidBuffer().getAmountAsInt(0) + east.fluidBuffer().getAmountAsInt(0), 0,
                "clay fluid pipe sent fluid to pipe before tank");
        tank.tanks().set(CombustionEngineBlockEntity.FUEL_TANK, fuel, CombustionEngineBlockEntity.TANK_CAPACITY);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), clayPos, helper.getLevel().getBlockState(clayPos), clay);
        helper.assertValueEqual(west.fluidBuffer().getAmountAsInt(0) + east.fluidBuffer().getAmountAsInt(0), 40,
                "clay fluid pipe did not fall back to pipe");

        BlockPos voidPos = helper.absolutePos(new BlockPos(2, 2, 5));
        BlockPos voidTargetPos = voidPos.east();
        helper.getLevel().setBlock(voidPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.VOID_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(voidTargetPos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        var voidPipe = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(voidPos);
        var voidTarget = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(voidTargetPos);
        voidPipe.fluidBuffer().set(0, fuel, 200);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), voidPos, helper.getLevel().getBlockState(voidPos), voidPipe);
        helper.assertValueEqual(voidPipe.fluidBuffer().getAmountAsInt(0), 120,
                "void fluid pipe discarded wrong amount");
        helper.assertValueEqual(voidTarget.fluidBuffer().getAmountAsInt(0), 0,
                "void fluid pipe forwarded discarded fluid");
        assertFluidUpgradeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_CLAY_ITEM.get(),
                buildcraft.transport.BCTransportItems.PIPE_CLAY_FLUID.get());
        assertFluidUpgradeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_VOID_ITEM.get(),
                buildcraft.transport.BCTransportItems.PIPE_VOID_FLUID.get());
        var drops = net.minecraft.world.level.block.Block.getDrops(
                helper.getLevel().getBlockState(voidPos), helper.getLevel(), voidPos, voidPipe);
        helper.assertTrue(drops.size() == 1
                && drops.getFirst().is(buildcraft.transport.BCTransportItems.PIPE_VOID_FLUID.get()),
                "void fluid pipe returned wrong drop");
        helper.succeed();
    }

    private static void transportDiamondFluidPipe(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos pipePos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos eastPos = pipePos.east();
        BlockPos southPos = pipePos.south();
        BlockPos northPos = pipePos.north();
        helper.getLevel().setBlock(pipePos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.DIAMOND_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        BlockState cobble = block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_FLUID);
        helper.getLevel().setBlock(eastPos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(southPos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(northPos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        var diamond = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        var east = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(eastPos);
        var south = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(southPos);
        var north = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(northPos);
        ItemStack waterFilter = ItemFragileFluidContainer.create(BCCoreItems.FRAGILE_FLUID_SHARD.get(),
                new net.neoforged.neoforge.fluids.FluidStack(
                        net.minecraft.world.level.material.Fluids.WATER, 100));
        ItemStack lavaFilter = ItemFragileFluidContainer.create(BCCoreItems.FRAGILE_FLUID_SHARD.get(),
                new net.neoforged.neoforge.fluids.FluidStack(
                        net.minecraft.world.level.material.Fluids.LAVA, 100));
        diamond.setDiamondRouteFilter(net.minecraft.core.Direction.EAST.ordinal() * 9, waterFilter);
        diamond.setDiamondRouteFilter(net.minecraft.core.Direction.SOUTH.ordinal() * 9, lavaFilter);
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);
        diamond.fluidBuffer().set(0, water, 160);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), pipePos, helper.getLevel().getBlockState(pipePos), diamond);
        helper.assertValueEqual(east.fluidBuffer().getAmountAsInt(0), 80,
                "diamond fluid pipe ignored matching filter");
        helper.assertValueEqual(south.fluidBuffer().getAmountAsInt(0), 0,
                "diamond fluid pipe used nonmatching filter");
        helper.assertValueEqual(north.fluidBuffer().getAmountAsInt(0), 0,
                "diamond fluid pipe used fallback before match");

        diamond.fluidBuffer().set(0, net.neoforged.neoforge.transfer.fluid.FluidResource.EMPTY, 0);
        var fuel = net.neoforged.neoforge.transfer.fluid.FluidResource.of(BCEnergyFluids.FUEL_LIGHT.get());
        diamond.fluidBuffer().set(0, fuel, 160);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), pipePos, helper.getLevel().getBlockState(pipePos), diamond);
        helper.assertValueEqual(north.fluidBuffer().getAmountAsInt(0), 80,
                "diamond fluid pipe did not use empty-filter fallback");
        diamond.setDiamondRouteFilter(net.minecraft.core.Direction.NORTH.ordinal() * 9, lavaFilter);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), pipePos, helper.getLevel().getBlockState(pipePos), diamond);
        helper.assertValueEqual(diamond.fluidBuffer().getAmountAsInt(0), 80,
                "diamond fluid pipe used configured nonmatching output");

        net.minecraft.nbt.CompoundTag saved = diamond.saveWithFullMetadata(helper.getLevel().registryAccess());
        var loaded = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                        pipePos, helper.getLevel().getBlockState(pipePos), saved, helper.getLevel().registryAccess());
        helper.assertTrue(loaded != null && !loaded.diamondRouteFilters()
                .get(net.minecraft.core.Direction.EAST.ordinal() * 9).isEmpty(),
                "diamond fluid filters failed codec reload");
        var menuPlayer = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        menuPlayer.setPos(pipePos.getX() + 0.5, pipePos.getY() + 0.5, pipePos.getZ() + 0.5);
        var menu = new buildcraft.transport.menu.DiamondRouteMenu(0, menuPlayer.getInventory(), pipePos);
        helper.assertTrue(menu.stillValid(menuPlayer) && menu.slots.size() == 90,
                "diamond fluid menu did not expose 54 filters plus inventory");
        assertFluidUpgradeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_DIAMOND_ITEM.get(),
                buildcraft.transport.BCTransportItems.PIPE_DIAMOND_FLUID.get());
        var drops = net.minecraft.world.level.block.Block.getDrops(
                helper.getLevel().getBlockState(pipePos), helper.getLevel(), pipePos, diamond);
        helper.assertTrue(drops.size() == 1
                && drops.getFirst().is(buildcraft.transport.BCTransportItems.PIPE_DIAMOND_FLUID.get()),
                "diamond fluid pipe returned wrong drop");
        helper.succeed();
    }

    private static void transportDiamondWoodFluidPipe(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos pipePos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos tankPos = pipePos.west();
        BlockPos outputPos = pipePos.east();
        helper.getLevel().setBlock(pipePos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.DIAMOND_WOOD_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(outputPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_FLUID),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(tankPos, BCCoreBlocks.ENGINE.get().defaultBlockState().setValue(
                BlockEngine.ENGINE_TYPE, EnumEngineType.IRON), net.minecraft.world.level.block.Block.UPDATE_ALL);
        var pipe = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        var tank = (CombustionEngineBlockEntity) helper.getLevel().getBlockEntity(tankPos);
        ItemStack waterFilter = ItemFragileFluidContainer.create(BCCoreItems.FRAGILE_FLUID_SHARD.get(),
                new net.neoforged.neoforge.fluids.FluidStack(
                        net.minecraft.world.level.material.Fluids.WATER, 100));
        pipe.setDiamondFilter(0, waterFilter);
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);
        var fuel = net.neoforged.neoforge.transfer.fluid.FluidResource.of(BCEnergyFluids.FUEL_LIGHT.get());
        tank.tanks().set(CombustionEngineBlockEntity.RESIDUE_TANK, water, 500);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), pipePos, helper.getLevel().getBlockState(pipePos), pipe);
        helper.assertValueEqual(pipe.extractionDirection(), net.minecraft.core.Direction.WEST,
                "diamond wooden fluid pipe selected wrong source");
        helper.assertValueEqual(pipe.mjReceiver().receivePower(MjAPI.MJ, false), 920_000L,
                "diamond wooden whitelist charged wrong power");
        helper.assertValueEqual(tank.tanks().getAmountAsInt(CombustionEngineBlockEntity.RESIDUE_TANK), 420,
                "diamond wooden whitelist extracted wrong amount");
        helper.assertValueEqual(pipe.fluidBuffer().getAmountAsInt(0), 80,
                "diamond wooden whitelist buffered wrong amount");

        pipe.fluidBuffer().set(0, net.neoforged.neoforge.transfer.fluid.FluidResource.EMPTY, 0);
        tank.tanks().set(CombustionEngineBlockEntity.RESIDUE_TANK, fuel, 500);
        helper.assertValueEqual(pipe.mjReceiver().receivePower(MjAPI.MJ, false), MjAPI.MJ,
                "diamond wooden whitelist extracted nonmatching fluid");
        pipe.setDiamondFilterMode(buildcraft.transport.block.entity.PipeHolderBlockEntity.DiamondFilterMode.BLACK_LIST);
        helper.assertValueEqual(pipe.mjReceiver().receivePower(MjAPI.MJ, false), 920_000L,
                "diamond wooden blacklist rejected nonmatching fluid");
        helper.assertValueEqual(pipe.fluidBuffer().getAmountAsInt(0), 80,
                "diamond wooden blacklist buffered wrong amount");

        pipe.fluidBuffer().set(0, net.neoforged.neoforge.transfer.fluid.FluidResource.EMPTY, 0);
        tank.tanks().set(CombustionEngineBlockEntity.RESIDUE_TANK, water, 500);
        pipe.setDiamondFilterMode(buildcraft.transport.block.entity.PipeHolderBlockEntity.DiamondFilterMode.ROUND_ROBIN);
        helper.assertValueEqual(pipe.mjReceiver().receivePower(MjAPI.MJ, false), MjAPI.MJ,
                "diamond wooden legacy round-robin unexpectedly extracted fluid");

        net.minecraft.nbt.CompoundTag saved = pipe.saveWithFullMetadata(helper.getLevel().registryAccess());
        var loaded = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                        pipePos, helper.getLevel().getBlockState(pipePos), saved, helper.getLevel().registryAccess());
        helper.assertTrue(loaded != null && !loaded.diamondFilters().getFirst().isEmpty()
                && loaded.diamondFilterMode()
                    == buildcraft.transport.block.entity.PipeHolderBlockEntity.DiamondFilterMode.ROUND_ROBIN,
                "diamond wooden fluid settings failed codec reload");
        var menuPlayer = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        menuPlayer.setPos(pipePos.getX() + 0.5, pipePos.getY() + 0.5, pipePos.getZ() + 0.5);
        var menu = new buildcraft.transport.menu.DiamondWoodMenu(0, menuPlayer.getInventory(), pipePos);
        helper.assertTrue(menu.stillValid(menuPlayer) && menu.slots.size() == 45,
                "diamond wooden fluid menu did not expose filters plus inventory");
        assertFluidUpgradeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_DIAMOND_WOOD_ITEM.get(),
                buildcraft.transport.BCTransportItems.PIPE_DIAMOND_WOOD_FLUID.get());
        var drops = net.minecraft.world.level.block.Block.getDrops(
                helper.getLevel().getBlockState(pipePos), helper.getLevel(), pipePos, pipe);
        helper.assertTrue(drops.size() == 1
                && drops.getFirst().is(buildcraft.transport.BCTransportItems.PIPE_DIAMOND_WOOD_FLUID.get()),
                "diamond wooden fluid pipe returned wrong drop");
        helper.succeed();
    }

    private static void transportRfPipes(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos woodPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos goldPos = woodPos.east();
        BlockPos targetPos = goldPos.east();
        helper.getLevel().setBlock(woodPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.WOOD_RF), Block.UPDATE_ALL);
        helper.getLevel().setBlock(goldPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.GOLD_RF), Block.UPDATE_ALL);
        helper.getLevel().setBlock(targetPos,
                buildcraft.core.BCCoreBlocks.ENGINE.get().defaultBlockState().setValue(
                        buildcraft.core.block.BlockEngine.ENGINE_TYPE,
                        buildcraft.api.enums.EnumEngineType.RF), Block.UPDATE_ALL);
        var wood = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(woodPos);
        var gold = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(goldPos);
        var target = (buildcraft.energy.block.entity.RfEngineBlockEntity)
                helper.getLevel().getBlockEntity(targetPos);
        var input = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
                woodPos, Direction.WEST);
        helper.assertTrue(input != null, "Wooden RF Pipe did not expose Energy input");
        helper.assertTrue(helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
                goldPos, Direction.UP) == null, "Golden RF Pipe incorrectly exposed Energy input");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(160, input.insert(500, transaction),
                    "Wooden RF Pipe ignored its 160 FE/t input limit");
            transaction.commit();
        }
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(helper.getLevel(), woodPos,
                helper.getLevel().getBlockState(woodPos), wood);
        helper.assertValueEqual(160, gold.rfStored(), "RF energy did not cross first pipe edge");
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(helper.getLevel(), goldPos,
                helper.getLevel().getBlockState(goldPos), gold);
        helper.assertValueEqual(0, target.energy().getAmountAsInt(),
                "RF energy crossed two pipe edges in one server tick");

        helper.assertValueEqual(40, buildcraft.transport.PipeType.COBBLESTONE_RF.rfTransferRate(),
                "Cobblestone RF rate");
        helper.assertValueEqual(80, buildcraft.transport.PipeType.STONE_RF.rfTransferRate(), "Stone RF rate");
        helper.assertValueEqual(320, buildcraft.transport.PipeType.QUARTZ_RF.rfTransferRate(), "Quartz RF rate");
        helper.assertValueEqual(2_560, buildcraft.transport.PipeType.DIAMOND_RF.rfTransferRate(),
                "Diamond RF rate");
        helper.assertFalse(buildcraft.transport.PipeType.COBBLESTONE_RF.connectsTo(
                buildcraft.transport.PipeType.STONE_RF), "Cobblestone and Stone RF pipes connected");
        helper.assertTrue(buildcraft.transport.PipeType.GOLD_RF.connectsTo(
                buildcraft.transport.PipeType.STONE_RF), "Golden RF Pipe did not bridge RF materials");
        helper.assertFalse(buildcraft.transport.PipeType.WOOD_RF.connectsTo(
                buildcraft.transport.PipeType.DIAMOND_WOOD_RF), "two Wooden RF inputs connected");

        BlockPos ironPos = helper.absolutePos(new BlockPos(6, 2, 1));
        helper.getLevel().setBlock(ironPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.IRON_RF), Block.UPDATE_ALL);
        var iron = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(ironPos);
        iron.activatePowerLimit(2);
        helper.assertValueEqual(80, iron.effectiveRfTransferRate(), "Iron RF limiter shift");
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(goldPos, gold.getBlockState(),
                gold.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof buildcraft.transport.block.entity.PipeHolderBlockEntity
                        && ((buildcraft.transport.block.entity.PipeHolderBlockEntity) loaded).rfStored() == 160,
                "RF Pipe failed codec reload");
        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_WOOD_POWER.get()),
                new ItemStack(Items.REDSTONE)));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.transport.BCTransportItems.PIPE_WOOD_RF.get()),
                "Wooden RF Pipe recipe output");
        var drops = Block.getDrops(helper.getLevel().getBlockState(goldPos), helper.getLevel(), goldPos, gold);
        helper.assertTrue(drops.size() == 1
                        && drops.getFirst().is(buildcraft.transport.BCTransportItems.PIPE_GOLD_RF.get()),
                "Golden RF Pipe loot output");
        helper.runAfterDelay(1, () -> {
            buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(helper.getLevel(), goldPos,
                    helper.getLevel().getBlockState(goldPos), gold);
            helper.assertValueEqual(160, target.energy().getAmountAsInt(),
                    "RF pipe network did not deliver into adjacent Energy receiver");
            helper.assertValueEqual(0, gold.rfStored(), "Golden RF Pipe retained delivered Energy");
            helper.succeed();
        });
    }

    private static void transportPipePlugs(GameTestHelper helper) {
        var pipeBlock = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos firstPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos secondPos = firstPos.east();
        BlockState itemPipe = pipeBlock.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_ITEM);
        helper.getLevel().setBlock(firstPos, itemPipe, Block.UPDATE_ALL);
        helper.getLevel().setBlock(secondPos, itemPipe, Block.UPDATE_ALL);
        var first = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(firstPos);
        helper.assertTrue(first.getBlockState().getValue(buildcraft.transport.block.PipeHolderBlock.EAST),
                "adjacent item pipes did not initially connect");
        helper.assertTrue(first.installAttachment(Direction.EAST,
                new ItemStack(buildcraft.transport.BCTransportItems.PLUG_BLOCKER.get())),
                "Blocker Plug installation failed");
        helper.assertFalse(helper.getLevel().getBlockState(firstPos).getValue(
                        buildcraft.transport.block.PipeHolderBlock.EAST),
                "Blocker Plug did not remove its pipe arm");
        helper.assertFalse(helper.getLevel().getBlockState(secondPos).getValue(
                        buildcraft.transport.block.PipeHolderBlock.WEST),
                "Blocker Plug did not disconnect the neighbouring pipe");
        helper.assertTrue(first.takeAttachment(Direction.EAST)
                        .is(buildcraft.transport.BCTransportItems.PLUG_BLOCKER.get()),
                "Blocker Plug could not be removed");
        helper.assertTrue(helper.getLevel().getBlockState(firstPos).getValue(
                        buildcraft.transport.block.PipeHolderBlock.EAST),
                "pipe connection did not recover after Blocker removal");

        BlockPos powerPos = helper.absolutePos(new BlockPos(5, 2, 1));
        helper.getLevel().setBlock(powerPos, pipeBlock.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.WOOD_POWER), Block.UPDATE_ALL);
        var power = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(powerPos);
        IMjToRfStatus previous = MjAPI.getRfStatus();
        try {
            MjAPI.setRfStatus(new IMjToRfStatus() {
                @Override public MjRfConversion getConversion() { return MjRfConversion.createDefault(); }
                @Override public boolean isAutoconvertEnabled() { return true; }
            });
            helper.assertTrue(power.installAttachment(Direction.UP,
                    new ItemStack(buildcraft.transport.BCTransportItems.PLUG_POWER_ADAPTOR.get())),
                    "Power Adaptor installation failed");
            helper.assertTrue(MjAPI.isRfAutoConversionEnabled(), "RF auto-conversion did not enable");
            helper.assertTrue(power.attachment(Direction.UP)
                            .is(buildcraft.transport.BCTransportItems.PLUG_POWER_ADAPTOR.get()),
                    "Power Adaptor attachment disappeared");
            helper.assertTrue(power.attachmentPowerReceiver() != null,
                    "Power Adaptor pipe has no MJ receiver");
            helper.assertTrue(power.attachmentEnergyHandler(Direction.UP) != null,
                    "Power Adaptor did not create its Energy handler");
            var energy = helper.getLevel().getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
                    powerPos, Direction.UP);
            helper.assertTrue(energy != null, "Power Adaptor did not expose NeoForge Energy input");
            try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                helper.assertValueEqual(50, energy.insert(50, transaction),
                        "Power Adaptor rejected Energy input");
                transaction.commit();
            }
            helper.assertValueEqual(5 * MjAPI.MJ, power.powerStored(),
                    "Power Adaptor converted Energy at the wrong MJ ratio");
        } finally {
            MjAPI.setRfStatus(previous);
        }
        helper.assertTrue(helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,
                powerPos, Direction.NORTH) == null,
                "Power Adaptor exposed Energy on an unconfigured side");
        var blockerInput = net.minecraft.world.item.crafting.CraftingInput.of(1, 1,
                java.util.List.of(new ItemStack(buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get())));
        ItemStack blockers = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, blockerInput, helper.getLevel())
                .orElseThrow().value().assemble(blockerInput);
        helper.assertTrue(blockers.is(buildcraft.transport.BCTransportItems.PLUG_BLOCKER.get())
                        && blockers.getCount() == 4,
                "Blocker Plug recipe output");
        var adaptorInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get()),
                new ItemStack(Items.GOLD_INGOT),
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get()),
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get()),
                new ItemStack(BCCoreItems.GEAR_STONE.get()),
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get()),
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get()),
                new ItemStack(Items.REDSTONE),
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get())));
        ItemStack adaptors = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, adaptorInput, helper.getLevel())
                .orElseThrow().value().assemble(adaptorInput);
        helper.assertTrue(adaptors.is(buildcraft.transport.BCTransportItems.PLUG_POWER_ADAPTOR.get())
                        && adaptors.getCount() == 4,
                "Power Adaptor recipe output");
        helper.succeed();
    }

    private static void transportFilteredBuffer(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(pos,
                buildcraft.transport.BCTransportBlocks.FILTERED_BUFFER.get().defaultBlockState(), Block.UPDATE_ALL);
        var buffer = (buildcraft.transport.block.entity.FilteredBufferBlockEntity)
                helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(buffer != null, "Filtered Buffer has no block entity");
        ItemStack namedStone = new ItemStack(Items.STONE);
        namedStone.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Exact"));
        buffer.setFilter(0, namedStone);
        buffer.setFilter(1, new ItemStack(Items.DIRT));
        var capability = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, pos, Direction.NORTH);
        helper.assertTrue(capability != null && capability.size() == 9,
                "Filtered Buffer did not expose its nine filtered slots");
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(0, capability.insert(0,
                    net.neoforged.neoforge.transfer.item.ItemResource.of(new ItemStack(Items.STONE)),
                    8, transaction), "Filtered Buffer ignored exact filter components");
            helper.assertValueEqual(8, capability.insert(0,
                    net.neoforged.neoforge.transfer.item.ItemResource.of(namedStone), 8, transaction),
                    "Filtered Buffer rejected matching item");
            transaction.commit();
        }
        helper.assertValueEqual(8L, buffer.inventory().getAmountAsLong(0),
                "Filtered Buffer did not retain inserted items");
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, buffer.getBlockState(),
                buffer.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof buildcraft.transport.block.entity.FilteredBufferBlockEntity,
                "Filtered Buffer did not reload as its registered block entity");
        var restored = (buildcraft.transport.block.entity.FilteredBufferBlockEntity) loaded;
        helper.assertTrue(ItemStack.isSameItemSameComponents(namedStone, restored.filter(0))
                        && restored.inventory().getAmountAsLong(0) == 8,
                "Filtered Buffer lost filter or inventory data on reload");
        helper.getLevel().setBlockEntity(restored);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new buildcraft.transport.menu.FilteredBufferMenu(91, player.getInventory(), pos);
        menu.setCarried(new ItemStack(Items.GOLD_INGOT, 32));
        menu.clicked(2, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
        helper.assertTrue(restored.filter(2).is(Items.GOLD_INGOT) && restored.filter(2).getCount() == 1,
                "Filtered Buffer menu did not store a phantom one-item filter");
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                new ItemStack(Items.OAK_PLANKS),
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_DIAMOND_ITEM.get()),
                new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.CHEST), new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.PISTON), new ItemStack(Items.OAK_PLANKS)));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow().value().assemble(input);
        helper.assertTrue(crafted.is(buildcraft.transport.BCTransportItems.FILTERED_BUFFER.get()),
                "Filtered Buffer recipe output");
        var drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                restored, null, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1
                        && drops.getFirst().is(buildcraft.transport.BCTransportItems.FILTERED_BUFFER.get()),
                "Filtered Buffer loot output");
        helper.succeed();
    }

    private static void transportPipeFoundation(GameTestHelper helper) {
        buildcraft.transport.block.PipeHolderBlock block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        net.minecraft.world.entity.player.Player placingPlayer =
            helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        placingPlayer.setItemInHand(InteractionHand.MAIN_HAND,
            new ItemStack(buildcraft.transport.BCTransportItems.PIPE_STONE_ITEM.get()));
        BlockPos clicked = helper.absolutePos(new BlockPos(6, 0, 0));
        BlockHitResult placementHit = new BlockHitResult(Vec3.atCenterOf(clicked),
            net.minecraft.core.Direction.UP, clicked, false);
        BlockState placementState = block.getStateForPlacement(new net.minecraft.world.item.context.BlockPlaceContext(
            new net.minecraft.world.item.context.UseOnContext(placingPlayer, InteractionHand.MAIN_HAND, placementHit)
        ));
        helper.assertTrue(placementState != null && placementState.getValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE) == buildcraft.transport.PipeType.STONE_ITEM,
            "stone pipe item did not select its holder type");
        BlockPos cobbleA = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos cobbleB = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockPos stone = helper.absolutePos(new BlockPos(2, 1, 0));
        BlockPos structure = helper.absolutePos(new BlockPos(3, 1, 0));
        BlockPos quartz = helper.absolutePos(new BlockPos(4, 1, 0));
        helper.getLevel().setBlock(cobbleA, block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_ITEM
        ), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(cobbleB, block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_ITEM
        ), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(stone, block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.STONE_ITEM
        ), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(structure, block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.STRUCTURE
        ), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(quartz, block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.QUARTZ_ITEM
        ), net.minecraft.world.level.block.Block.UPDATE_ALL);

        BlockState cobbleAState = helper.getLevel().getBlockState(cobbleA);
        BlockState cobbleBState = helper.getLevel().getBlockState(cobbleB);
        BlockState stoneState = helper.getLevel().getBlockState(stone);
        BlockState structureState = helper.getLevel().getBlockState(structure);
        BlockState quartzState = helper.getLevel().getBlockState(quartz);
        helper.assertTrue(cobbleAState.getValue(buildcraft.transport.block.PipeHolderBlock.EAST),
            "matching cobblestone pipes did not connect");
        helper.assertTrue(cobbleBState.getValue(buildcraft.transport.block.PipeHolderBlock.WEST),
            "matching cobblestone pipe connection was not reciprocal");
        helper.assertTrue(!cobbleBState.getValue(buildcraft.transport.block.PipeHolderBlock.EAST)
            && !stoneState.getValue(buildcraft.transport.block.PipeHolderBlock.WEST),
            "stone and cobblestone pipes incorrectly connected");
        helper.assertTrue(!stoneState.getValue(buildcraft.transport.block.PipeHolderBlock.EAST)
            && !structureState.getValue(buildcraft.transport.block.PipeHolderBlock.WEST),
            "structure pipe incorrectly connected to item flow");
        helper.assertTrue(!structureState.getValue(buildcraft.transport.block.PipeHolderBlock.EAST)
            && !quartzState.getValue(buildcraft.transport.block.PipeHolderBlock.WEST),
            "structure pipe incorrectly connected to quartz item flow");
        helper.assertTrue(helper.getLevel().getBlockEntity(structure)
            instanceof buildcraft.transport.block.entity.PipeHolderBlockEntity,
            "pipe holder block entity missing");

        assertPipeLoot(helper, cobbleA, buildcraft.transport.BCTransportItems.PIPE_COBBLE_ITEM.get());
        assertPipeLoot(helper, stone, buildcraft.transport.BCTransportItems.PIPE_STONE_ITEM.get());
        assertPipeLoot(helper, structure, buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get());
        assertPipeLoot(helper, quartz, buildcraft.transport.BCTransportItems.PIPE_QUARTZ_ITEM.get());
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get(),
            Items.COBBLESTONE, Items.GRAVEL);
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_COBBLE_ITEM.get(),
            Items.COBBLESTONE, Items.GLASS);
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_STONE_ITEM.get(),
            Items.STONE, Items.GLASS);
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_QUARTZ_ITEM.get(),
            Items.QUARTZ_BLOCK, Items.GLASS);
        helper.succeed();
    }

    private static void assertPipeLoot(GameTestHelper helper, BlockPos pos, net.minecraft.world.item.Item item) {
        BlockState state = helper.getLevel().getBlockState(pos);
        java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
            state, helper.getLevel(), pos, helper.getLevel().getBlockEntity(pos)
        );
        helper.assertValueEqual(drops.size(), 1, "pipe returned wrong drop count");
        helper.assertTrue(drops.getFirst().is(item), "pipe returned wrong historical item identity");
    }

    private static void assertPipeRecipe(GameTestHelper helper, net.minecraft.world.item.Item expected,
        net.minecraft.world.item.Item shell, net.minecraft.world.item.Item middle) {
        net.minecraft.world.item.crafting.CraftingInput input =
            net.minecraft.world.item.crafting.CraftingInput.of(3, 1, java.util.List.of(
                new ItemStack(shell), new ItemStack(middle), new ItemStack(shell)
            ));
        ItemStack output = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
            net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, helper.getLevel()
        ).orElseThrow().value().assemble(input);
        helper.assertTrue(output.is(expected), "pipe recipe returned wrong item");
        helper.assertValueEqual(output.getCount(), 8, "pipe recipe returned wrong count");
    }

    private static void transportItemFlow(GameTestHelper helper) {
        buildcraft.transport.block.PipeHolderBlock block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState cobble = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_ITEM
        );
        BlockPos firstPos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos secondPos = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockPos chestPos = helper.absolutePos(new BlockPos(2, 1, 0));
        helper.getLevel().setBlock(firstPos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(secondPos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        buildcraft.transport.block.entity.PipeHolderBlockEntity first =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(firstPos);
        buildcraft.transport.block.entity.PipeHolderBlockEntity second =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(secondPos);
        helper.assertTrue(helper.getLevel().getBlockState(secondPos).getValue(
            buildcraft.transport.block.PipeHolderBlock.EAST),
            "item pipe did not expose its inventory connection arm");
        var input = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            firstPos, net.minecraft.core.Direction.WEST
        );
        helper.assertTrue(input != null, "item pipe input capability missing");
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(input.insert(
                net.neoforged.neoforge.transfer.item.ItemResource.of(Items.DIAMOND), 12, transaction
            ), 12, "item pipe rejected committed insertion");
            helper.assertValueEqual(input.extract(
                net.neoforged.neoforge.transfer.item.ItemResource.of(Items.DIAMOND), 1, transaction
            ), 0, "item pipe allowed external extraction");
            transaction.commit();
        }
        helper.assertTrue(helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            firstPos, null
        ) == null, "item pipe exposed an unsided capability");
        tickPipes(helper, 65, firstPos, secondPos);
        net.minecraft.world.Container chest = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(chestPos);
        helper.assertValueEqual(containerCount(chest, Items.DIAMOND), 12,
            "straight item pipes did not deliver into inventory");

        BlockPos branchPos = helper.absolutePos(new BlockPos(5, 1, 2));
        BlockPos northChestPos = branchPos.north();
        BlockPos southChestPos = branchPos.south();
        helper.getLevel().setBlock(branchPos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(northChestPos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(southChestPos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        var branchInput = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            branchPos, net.minecraft.core.Direction.WEST
        );
        insertPipeItem(branchInput, Items.GOLD_INGOT, 3);
        tickPipes(helper, 35, branchPos);
        insertPipeItem(branchInput, Items.IRON_INGOT, 4);
        tickPipes(helper, 35, branchPos);
        net.minecraft.world.Container northChest =
            (net.minecraft.world.Container) helper.getLevel().getBlockEntity(northChestPos);
        net.minecraft.world.Container southChest =
            (net.minecraft.world.Container) helper.getLevel().getBlockEntity(southChestPos);
        helper.assertValueEqual(containerCount(northChest, Items.GOLD_INGOT)
            + containerCount(southChest, Items.GOLD_INGOT), 3, "branch lost first item stack");
        helper.assertValueEqual(containerCount(northChest, Items.IRON_INGOT)
            + containerCount(southChest, Items.IRON_INGOT), 4, "branch lost second item stack");
        helper.assertTrue((containerCount(northChest, Items.GOLD_INGOT) > 0
            && containerCount(southChest, Items.IRON_INGOT) > 0)
            || (containerCount(southChest, Items.GOLD_INGOT) > 0
            && containerCount(northChest, Items.IRON_INGOT) > 0),
            "branch did not round-robin successive stacks");

        insertPipeItem(branchInput, Items.EMERALD, 2);
        tickPipes(helper, 1, branchPos);
        net.minecraft.nbt.CompoundTag saved = helper.getLevel().getBlockEntity(branchPos)
            .saveWithFullMetadata(helper.getLevel().registryAccess());
        buildcraft.transport.block.entity.PipeHolderBlockEntity loaded =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                    branchPos, helper.getLevel().getBlockState(branchPos), saved,
                    helper.getLevel().registryAccess()
                );
        helper.assertTrue(loaded != null && loaded.travellingCount() == 1,
            "travelling item state did not survive codec reload");
        helper.assertTrue(loaded.travellingItems().getFirst().stack().is(Items.EMERALD),
            "reloaded travelling item had wrong stack");
        insertPipeItem(input, Items.LAPIS_LAZULI, 5);
        net.minecraft.nbt.CompoundTag pendingSaved = first.saveWithFullMetadata(helper.getLevel().registryAccess());
        buildcraft.transport.block.entity.PipeHolderBlockEntity pendingLoaded =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                    firstPos, helper.getLevel().getBlockState(firstPos), pendingSaved,
                    helper.getLevel().registryAccess()
                );
        helper.assertTrue(pendingLoaded != null, "pending pipe input block entity failed to reload");
        helper.assertValueEqual(pendingLoaded.input(net.minecraft.core.Direction.WEST).getAmountAsInt(0), 5,
            "pending pipe capability input did not survive codec reload");
        helper.succeed();
    }

    private static void transportPipeVisualState(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockPos woodPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos daizuliPos = helper.absolutePos(new BlockPos(4, 2, 1));
        BlockPos stripesPos = helper.absolutePos(new BlockPos(7, 2, 1));
        helper.getLevel().setBlock(woodPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.WOOD_ITEM), Block.UPDATE_ALL);
        helper.getLevel().setBlock(woodPos.west(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(daizuliPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.DAIZULI_ITEM), Block.UPDATE_ALL);
        helper.getLevel().setBlock(daizuliPos.east(), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(stripesPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.STRIPES_ITEM), Block.UPDATE_ALL);

        var wood = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(woodPos);
        var daizuli = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(daizuliPos);
        var stripes = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(stripesPos);
        tickPipes(helper, 1, woodPos);
        daizuli.activatePipeDirection(Direction.EAST);
        daizuli.cycleDaizuliColor(false);
        stripes.activatePipeDirection(Direction.UP);
        helper.assertTrue(wood.extractionDirection() == Direction.WEST,
                "wood pipe did not expose its synchronized extraction face");
        helper.assertTrue(daizuli.routingDirection() == Direction.EAST,
                "Daizuli pipe did not expose its synchronized route face");
        helper.assertTrue(daizuli.pipeColor() == net.minecraft.world.item.DyeColor.ORANGE,
                "Daizuli pipe did not expose its synchronized colour");
        helper.assertTrue(stripes.stripesDirection() == Direction.UP,
                "Stripes pipe did not expose its synchronized working face");

        var restoredWood = reloadPipeEntity(helper, woodPos, wood);
        var restoredDaizuli = reloadPipeEntity(helper, daizuliPos, daizuli);
        var restoredStripes = reloadPipeEntity(helper, stripesPos, stripes);
        helper.assertTrue(restoredWood.extractionDirection() == Direction.WEST,
                "reloaded wood pipe lost its extraction face");
        helper.assertTrue(restoredDaizuli.routingDirection() == Direction.EAST,
                "reloaded Daizuli pipe lost its route face");
        helper.assertTrue(restoredDaizuli.pipeColor() == net.minecraft.world.item.DyeColor.ORANGE,
                "reloaded Daizuli pipe lost its colour");
        helper.assertTrue(restoredStripes.stripesDirection() == Direction.UP,
                "reloaded Stripes pipe lost its working face");
        helper.succeed();
    }

    private static buildcraft.transport.block.entity.PipeHolderBlockEntity reloadPipeEntity(
            GameTestHelper helper, BlockPos pos,
            buildcraft.transport.block.entity.PipeHolderBlockEntity holder) {
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                pos, holder.getBlockState(), holder.saveWithFullMetadata(helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());
        if (!(loaded instanceof buildcraft.transport.block.entity.PipeHolderBlockEntity pipe)) {
            throw new IllegalStateException("pipe block entity did not reload from its saved tag");
        }
        return pipe;
    }

    private static void transportPartialItemBounce(GameTestHelper helper) {
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState pipeState = block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_ITEM);
        BlockPos pipePos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos partialTargetPos = pipePos.east();
        BlockPos alternateTargetPos = pipePos.south();
        helper.getLevel().setBlock(pipePos, pipeState, Block.UPDATE_ALL);
        helper.getLevel().setBlock(partialTargetPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        var partialTarget = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(partialTargetPos);
        partialTarget.setItem(0, new ItemStack(Items.DIAMOND, 62));
        for (int slot = 1; slot < partialTarget.getContainerSize(); slot++) {
            partialTarget.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }

        var input = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
                pipePos, Direction.WEST);
        helper.assertTrue(input != null, "partial-bounce pipe input capability missing");
        insertPipeItem(input, Items.DIAMOND, 5);
        tickPipes(helper, 11, pipePos);
        helper.getLevel().setBlock(alternateTargetPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        tickPipes(helper, 16, pipePos);

        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        helper.assertValueEqual(containerCount(partialTarget, Items.DIAMOND), 64,
                "partial target did not accept its available two items");
        helper.assertValueEqual(holder.travellingCount(), 1,
                "partial target did not return the excess stack to pipe flow");
        var excess = holder.travellingItems().getFirst();
        helper.assertValueEqual(excess.stack().getCount(), 3,
                "partial target returned the wrong excess count");
        helper.assertTrue(excess.blocked().orElse(null) == Direction.EAST,
                "bounced stack did not remember the blocked target");

        var tag = holder.saveWithFullMetadata(helper.getLevel().registryAccess());
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                pipePos, holder.getBlockState(), tag, helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof buildcraft.transport.block.entity.PipeHolderBlockEntity,
                "partial-bounce pipe did not reload from its saved tag");
        var restored = (buildcraft.transport.block.entity.PipeHolderBlockEntity) loaded;
        helper.assertValueEqual(restored.travellingCount(), 1,
                "reloaded pipe lost the bounced stack");
        helper.assertValueEqual(restored.travellingItems().getFirst().stack().getCount(), 3,
                "reloaded bounced stack had the wrong count");
        helper.assertTrue(restored.travellingItems().getFirst().blocked().orElse(null) == Direction.EAST,
                "reloaded bounced stack lost its blocked target");

        tickPipes(helper, 70, pipePos);
        var alternateTarget = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(alternateTargetPos);
        helper.assertValueEqual(containerCount(alternateTarget, Items.DIAMOND), 3,
                "bounced excess did not select the available alternate target");
        helper.assertValueEqual(holder.travellingCount(), 0,
                "pipe retained the delivered bounced stack");
        helper.succeed();
    }

    private static void insertPipeItem(net.neoforged.neoforge.transfer.ResourceHandler<
        net.neoforged.neoforge.transfer.item.ItemResource> handler, net.minecraft.world.item.Item item, int amount) {
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            if (handler.insert(net.neoforged.neoforge.transfer.item.ItemResource.of(item), amount, transaction)
                != amount) throw new IllegalStateException("pipe test insertion failed");
            transaction.commit();
        }
    }

    private static void tickPipes(GameTestHelper helper, int ticks, BlockPos... positions) {
        for (int tick = 0; tick < ticks; tick++) {
            for (BlockPos pos : positions) {
                buildcraft.transport.block.entity.PipeHolderBlockEntity holder =
                    (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(pos);
                buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                    helper.getLevel(), pos, helper.getLevel().getBlockState(pos), holder
                );
            }
        }
    }

    private static int containerCount(net.minecraft.world.Container container, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(item)) count += container.getItem(slot).getCount();
        }
        return count;
    }

    private static void transportSpecialItemPipes(GameTestHelper helper) {
        buildcraft.transport.block.PipeHolderBlock block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState wood = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.WOOD_ITEM
        );
        BlockState cobble = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_ITEM
        );
        BlockPos sourcePos = helper.absolutePos(new BlockPos(0, 1, 0));
        BlockPos woodPos = helper.absolutePos(new BlockPos(1, 1, 0));
        BlockPos cobblePos = helper.absolutePos(new BlockPos(2, 1, 0));
        BlockPos targetPos = helper.absolutePos(new BlockPos(3, 1, 0));
        helper.getLevel().setBlock(sourcePos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(woodPos, wood, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(cobblePos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(targetPos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        net.minecraft.world.Container source = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(sourcePos);
        source.setItem(0, new ItemStack(Items.COAL, 10));
        tickPipes(helper, 1, woodPos);
        buildcraft.transport.block.entity.PipeHolderBlockEntity woodHolder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(woodPos);
        helper.assertValueEqual(woodHolder.extractionDirection(), net.minecraft.core.Direction.WEST,
            "wood pipe did not face its source inventory");
        buildcraft.api.mj.IMjReceiver woodReceiver = helper.getLevel().getCapability(
            MjAPI.CAP_RECEIVER, woodPos, net.minecraft.core.Direction.UP
        );
        helper.assertTrue(woodReceiver != null, "wood pipe MJ receiver capability missing");
        helper.assertValueEqual(woodReceiver.getPowerRequested(), 10 * MjAPI.MJ,
            "wood pipe requested wrong MJ for available items");
        helper.assertValueEqual(woodReceiver.receivePower(6 * MjAPI.MJ, true), 0L,
            "wood pipe simulated wrong MJ excess");
        helper.assertValueEqual(containerCount(source, Items.COAL), 10,
            "wood pipe mutated source during MJ simulation");
        helper.assertValueEqual(woodReceiver.receivePower(6 * MjAPI.MJ, false), 0L,
            "wood pipe committed wrong MJ excess");
        helper.assertValueEqual(containerCount(source, Items.COAL), 4,
            "wood pipe extracted wrong committed item count");
        tickPipes(helper, 65, woodPos, cobblePos);
        net.minecraft.world.Container target = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(targetPos);
        helper.assertValueEqual(containerCount(target, Items.COAL), 6,
            "wood pipe did not deliver extracted items away from source");

        BlockPos woodOtherPos = helper.absolutePos(new BlockPos(1, 1, 3));
        BlockPos woodPairPos = woodOtherPos.east();
        helper.getLevel().setBlock(woodOtherPos, wood, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(woodPairPos, wood, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.assertTrue(!helper.getLevel().getBlockState(woodOtherPos).getValue(
            buildcraft.transport.block.PipeHolderBlock.EAST), "wood pipes incorrectly connected to each other");

        BlockState gold = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.GOLD_ITEM
        );
        BlockPos goldPos = helper.absolutePos(new BlockPos(5, 1, 1));
        BlockPos goldTargetPos = goldPos.east();
        helper.getLevel().setBlock(goldPos, gold, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(goldTargetPos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        var goldInput = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            goldPos, net.minecraft.core.Direction.WEST
        );
        insertPipeItem(goldInput, Items.REDSTONE, 2);
        tickPipes(helper, 10, goldPos);
        buildcraft.transport.block.entity.PipeHolderBlockEntity goldHolder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(goldPos);
        helper.assertValueEqual(goldHolder.travellingCount(), 1, "gold pipe lost travelling stack");
        helper.assertTrue(Math.abs(goldHolder.travellingItems().getFirst().speed() - 0.12) < 0.0001,
            "gold pipe did not accelerate by historical delta");
        helper.assertTrue(goldHolder.travellingItems().getFirst().ticks() <= 5,
            "gold pipe acceleration did not shorten travel time");
        tickPipes(helper, 5, goldPos);
        net.minecraft.world.Container goldTarget =
            (net.minecraft.world.Container) helper.getLevel().getBlockEntity(goldTargetPos);
        helper.assertValueEqual(containerCount(goldTarget, Items.REDSTONE), 2,
            "gold pipe did not deliver accelerated stack");

        assertPipeLoot(helper, woodPos, buildcraft.transport.BCTransportItems.PIPE_WOOD_ITEM.get());
        assertPipeLoot(helper, goldPos, buildcraft.transport.BCTransportItems.PIPE_GOLD_ITEM.get());
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_WOOD_ITEM.get(),
            Items.OAK_PLANKS, Items.GLASS);
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_GOLD_ITEM.get(),
            Items.GOLD_INGOT, Items.GLASS);
        helper.succeed();
    }

    private static void transportRoutingItemPipes(GameTestHelper helper) {
        buildcraft.transport.block.PipeHolderBlock block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState iron = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.IRON_ITEM
        );
        BlockPos ironPos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos northTargetPos = ironPos.north();
        BlockPos eastTargetPos = ironPos.east();
        helper.getLevel().setBlock(ironPos, iron, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(northTargetPos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(eastTargetPos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        var ironInput = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            ironPos, net.minecraft.core.Direction.WEST
        );
        insertPipeItem(ironInput, Items.DIAMOND, 2);
        tickPipes(helper, 25, ironPos);
        buildcraft.transport.block.entity.PipeHolderBlockEntity ironHolder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(ironPos);
        helper.assertValueEqual(ironHolder.routingDirection(), net.minecraft.core.Direction.NORTH,
            "iron pipe did not select first connected output");
        net.minecraft.world.Container northTarget =
            (net.minecraft.world.Container) helper.getLevel().getBlockEntity(northTargetPos);
        net.minecraft.world.Container eastTarget =
            (net.minecraft.world.Container) helper.getLevel().getBlockEntity(eastTargetPos);
        helper.assertValueEqual(containerCount(northTarget, Items.DIAMOND), 2,
            "iron pipe did not constrain first stack to selected output");
        helper.assertValueEqual(containerCount(eastTarget, Items.DIAMOND), 0,
            "iron pipe leaked first stack to unselected output");
        for (int attempt = 0; attempt < 6
            && ironHolder.routingDirection() != net.minecraft.core.Direction.EAST; attempt++) {
            helper.assertTrue(ironHolder.rotatePipeDirection(), "iron pipe failed to rotate to another output");
        }
        helper.assertValueEqual(ironHolder.routingDirection(), net.minecraft.core.Direction.EAST,
            "iron pipe rotated to wrong output");
        insertPipeItem(ironInput, Items.EMERALD, 3);
        tickPipes(helper, 25, ironPos);
        helper.assertValueEqual(containerCount(eastTarget, Items.EMERALD), 3,
            "rotated iron pipe did not use new output");
        net.minecraft.nbt.CompoundTag ironSaved = ironHolder.saveWithFullMetadata(helper.getLevel().registryAccess());
        buildcraft.transport.block.entity.PipeHolderBlockEntity ironLoaded =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                    ironPos, helper.getLevel().getBlockState(ironPos), ironSaved,
                    helper.getLevel().registryAccess()
                );
        helper.assertTrue(ironLoaded != null, "iron pipe failed codec reload");
        helper.assertValueEqual(ironLoaded.routingDirection(), net.minecraft.core.Direction.EAST,
            "iron pipe lost selected direction on codec reload");

        BlockState clay = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.CLAY_ITEM
        );
        BlockState cobble = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_ITEM
        );
        BlockPos clayPos = helper.absolutePos(new BlockPos(5, 1, 2));
        BlockPos clayTargetPos = clayPos.north();
        BlockPos branchPipePos = clayPos.east();
        BlockPos branchTargetPos = branchPipePos.east();
        helper.getLevel().setBlock(clayPos, clay, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(clayTargetPos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(branchPipePos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(branchTargetPos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        var clayInput = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            clayPos, net.minecraft.core.Direction.WEST
        );
        insertPipeItem(clayInput, Items.CLAY_BALL, 5);
        tickPipes(helper, 35, clayPos, branchPipePos);
        net.minecraft.world.Container clayTarget =
            (net.minecraft.world.Container) helper.getLevel().getBlockEntity(clayTargetPos);
        net.minecraft.world.Container branchTarget =
            (net.minecraft.world.Container) helper.getLevel().getBlockEntity(branchTargetPos);
        helper.assertValueEqual(containerCount(clayTarget, Items.CLAY_BALL), 5,
            "clay pipe did not prioritize adjacent inventory");
        helper.assertValueEqual(containerCount(branchTarget, Items.CLAY_BALL), 0,
            "clay pipe routed stack through pipe instead of inventory");

        assertPipeLoot(helper, ironPos, buildcraft.transport.BCTransportItems.PIPE_IRON_ITEM.get());
        assertPipeLoot(helper, clayPos, buildcraft.transport.BCTransportItems.PIPE_CLAY_ITEM.get());
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_IRON_ITEM.get(),
            Items.IRON_INGOT, Items.GLASS);
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_CLAY_ITEM.get(),
            Items.CLAY, Items.GLASS);
        helper.succeed();
    }

    private static void transportTerminalItemPipes(GameTestHelper helper) {
        buildcraft.transport.block.PipeHolderBlock block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState sandstone = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.SANDSTONE_ITEM
        );
        BlockState cobble = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_ITEM
        );
        BlockPos chestPos = helper.absolutePos(new BlockPos(0, 1, 1));
        BlockPos sandstonePos = chestPos.east();
        BlockPos cobblePos = sandstonePos.east();
        helper.getLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(sandstonePos, sandstone, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(cobblePos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        BlockState sandstoneState = helper.getLevel().getBlockState(sandstonePos);
        helper.assertTrue(!sandstoneState.getValue(buildcraft.transport.block.PipeHolderBlock.WEST),
            "sandstone pipe incorrectly connected to inventory");
        helper.assertTrue(sandstoneState.getValue(buildcraft.transport.block.PipeHolderBlock.EAST),
            "sandstone pipe did not connect to item pipe");
        var sandstoneInput = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            sandstonePos, net.minecraft.core.Direction.WEST
        );
        insertPipeItem(sandstoneInput, Items.SAND, 1);
        tickPipes(helper, 10, sandstonePos);
        buildcraft.transport.block.entity.PipeHolderBlockEntity sandstoneHolder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(sandstonePos);
        helper.assertValueEqual(sandstoneHolder.travellingCount(), 1,
            "sandstone pipe lost travelling item");
        helper.assertTrue(Math.abs(sandstoneHolder.travellingItems().getFirst().speed() - 0.042) < 0.0001,
            "sandstone pipe did not use stone speed delta");

        BlockState voidPipe = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.VOID_ITEM
        );
        BlockPos voidPos = helper.absolutePos(new BlockPos(5, 1, 1));
        BlockPos voidTargetPos = voidPos.east();
        helper.getLevel().setBlock(voidPos, voidPipe, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(voidTargetPos, Blocks.CHEST.defaultBlockState(),
            net.minecraft.world.level.block.Block.UPDATE_ALL);
        var voidInput = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            voidPos, net.minecraft.core.Direction.WEST
        );
        insertPipeItem(voidInput, Items.ROTTEN_FLESH, 7);
        tickPipes(helper, 10, voidPos);
        buildcraft.transport.block.entity.PipeHolderBlockEntity voidHolder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(voidPos);
        net.minecraft.world.Container voidTarget =
            (net.minecraft.world.Container) helper.getLevel().getBlockEntity(voidTargetPos);
        helper.assertValueEqual(voidHolder.travellingCount(), 0, "void pipe retained discarded stack");
        helper.assertValueEqual(containerCount(voidTarget, Items.ROTTEN_FLESH), 0,
            "void pipe routed discarded stack into inventory");
        net.minecraft.world.phys.AABB search = new net.minecraft.world.phys.AABB(voidPos).inflate(2);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
            net.minecraft.world.entity.item.ItemEntity.class, search
        ).stream().noneMatch(entity -> entity.getItem().is(Items.ROTTEN_FLESH)),
            "void pipe dropped rather than discarded stack");

        assertPipeLoot(helper, sandstonePos, buildcraft.transport.BCTransportItems.PIPE_SANDSTONE_ITEM.get());
        assertPipeLoot(helper, voidPos, buildcraft.transport.BCTransportItems.PIPE_VOID_ITEM.get());
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_SANDSTONE_ITEM.get(),
            Items.SANDSTONE, Items.GLASS);
        net.minecraft.world.item.crafting.CraftingInput voidRecipeInput =
            net.minecraft.world.item.crafting.CraftingInput.of(3, 1, java.util.List.of(
                new ItemStack(Items.BLACK_DYE), new ItemStack(Items.GLASS), new ItemStack(Items.REDSTONE)
            ));
        ItemStack voidOutput = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
            net.minecraft.world.item.crafting.RecipeType.CRAFTING, voidRecipeInput, helper.getLevel()
        ).orElseThrow().value().assemble(voidRecipeInput);
        helper.assertTrue(voidOutput.is(buildcraft.transport.BCTransportItems.PIPE_VOID_ITEM.get()),
            "void pipe recipe returned wrong item");
        helper.assertValueEqual(voidOutput.getCount(), 8, "void pipe recipe returned wrong count");
        helper.succeed();
    }

    private static void siliconPipeWireSignals(GameTestHelper helper) {
        BlockPos sourcePos = helper.absolutePos(new BlockPos(2, 3, 3));
        BlockPos relayPos = helper.absolutePos(new BlockPos(3, 3, 3));
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        var sourceState = block.defaultBlockState()
                .setValue(buildcraft.transport.block.PipeHolderBlock.TYPE,
                        buildcraft.transport.PipeType.COBBLESTONE_ITEM)
                .setValue(buildcraft.transport.block.PipeHolderBlock.EAST, true);
        var relayState = block.defaultBlockState()
                .setValue(buildcraft.transport.block.PipeHolderBlock.TYPE,
                        buildcraft.transport.PipeType.COBBLESTONE_ITEM)
                .setValue(buildcraft.transport.block.PipeHolderBlock.WEST, true);
        helper.getLevel().setBlock(sourcePos, sourceState, 3);
        helper.getLevel().setBlock(relayPos, relayState, 3);
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(sourcePos);
        var relay = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(relayPos);
        for (buildcraft.transport.PipeWireColor color : java.util.List.of(
                buildcraft.transport.PipeWireColor.RED, buildcraft.transport.PipeWireColor.BLUE)) {
            helper.assertTrue(source.installWire(color), "source pipe wire could not be installed");
            helper.assertTrue(relay.installWire(color), "relay pipe wire could not be installed");
        }
        ItemStack sourceGate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        ItemStack relayGate = sourceGate.copy();
        helper.assertTrue(source.installAttachment(Direction.UP, sourceGate),
                "source wire Gate could not be installed");
        helper.assertTrue(relay.installAttachment(Direction.UP, relayGate),
                "relay wire Gate could not be installed");
        source.attachment(Direction.UP).set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.PIPE_SIGNAL_RED))));
        relay.attachment(Direction.UP).set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.PIPE_SIGNAL_RED_ACTIVE,
                                buildcraft.silicon.gate.GateAction.PIPE_SIGNAL_BLUE))));
        tickPipes(helper, 2, sourcePos, relayPos);
        helper.assertTrue(source.isWirePowered(buildcraft.transport.PipeWireColor.RED)
                        && relay.isWirePowered(buildcraft.transport.PipeWireColor.RED),
                "red signal did not propagate across the connected wire network");
        helper.assertTrue(source.isWirePowered(buildcraft.transport.PipeWireColor.BLUE)
                        && relay.isWirePowered(buildcraft.transport.PipeWireColor.BLUE),
                "red signal trigger did not drive the blue wire network");
        source.takeAttachment(Direction.UP);
        tickPipes(helper, 2, sourcePos, relayPos);
        helper.assertTrue(!source.isWirePowered(buildcraft.transport.PipeWireColor.RED)
                        && !relay.isWirePowered(buildcraft.transport.PipeWireColor.RED)
                        && !source.isWirePowered(buildcraft.transport.PipeWireColor.BLUE)
                        && !relay.isWirePowered(buildcraft.transport.PipeWireColor.BLUE),
                "wire networks remained powered after their Gate sources were removed");
        net.minecraft.world.item.crafting.CraftingInput wireRecipeInput =
                net.minecraft.world.item.crafting.CraftingInput.of(3, 1, java.util.List.of(
                        new ItemStack(Items.RED_DYE), new ItemStack(Items.REDSTONE),
                        new ItemStack(Items.IRON_INGOT)));
        ItemStack wireOutput = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, wireRecipeInput, helper.getLevel())
                .orElseThrow().value().assemble(wireRecipeInput);
        helper.assertTrue(wireOutput.is(buildcraft.transport.BCTransportItems.PIPE_WIRE_RED.get()),
                "pipe wire recipe returned wrong item");
        helper.assertValueEqual(wireOutput.getCount(), 8, "pipe wire recipe returned wrong count");
        helper.succeed();
    }

    private static void siliconPipeColorActions(GameTestHelper helper) {
        BlockPos lapisPos = helper.absolutePos(new BlockPos(2, 3, 3));
        BlockPos daizuliPos = helper.absolutePos(new BlockPos(4, 3, 3));
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        helper.getLevel().setBlock(lapisPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.LAPIS_ITEM), 3);
        helper.getLevel().setBlock(daizuliPos, block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.DAIZULI_ITEM), 3);
        var lapis = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(lapisPos);
        var daizuli = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(daizuliPos);
        ItemStack lapisGate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        ItemStack daizuliGate = lapisGate.copy();
        helper.assertTrue(lapis.installAttachment(Direction.UP, lapisGate),
                "lapis pipe color gate could not be installed");
        helper.assertTrue(daizuli.installAttachment(Direction.UP, daizuliGate),
                "daizuli pipe color gate could not be installed");
        lapisGate = lapis.attachment(Direction.UP);
        daizuliGate = daizuli.attachment(Direction.UP);
        lapisGate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.PIPE_COLOR_RED))));
        daizuliGate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.PIPE_COLOR_BLUE))));
        tickPipes(helper, 1, lapisPos, daizuliPos);
        helper.assertValueEqual(net.minecraft.world.item.DyeColor.RED, lapis.pipeColor(),
                "red Gate action did not recolor the Lapis pipe");
        helper.assertValueEqual(net.minecraft.world.item.DyeColor.BLUE, daizuli.pipeColor(),
                "blue Gate action did not recolor the Daizuli pipe");
        helper.succeed();
    }

    private static void siliconExtractionPresetActions(GameTestHelper helper) {
        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 3, 3));
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState().setValue(
                        buildcraft.transport.block.PipeHolderBlock.TYPE,
                        buildcraft.transport.PipeType.EMZULI_ITEM), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        holder.setEmzuliFilter(2, new ItemStack(Items.STONE));
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.UP, gate),
                "extraction preset action gate could not be installed");
        gate = holder.attachment(Direction.UP);
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.EXTRACTION_PRESET_TRIANGLE))));
        tickPipes(helper, 1, pipePos);
        helper.assertValueEqual(2, holder.activeEmzuliPreset(),
                "TRIANGLE extraction preset did not activate Emzuli slot 2");
        helper.succeed();
    }

    private static void siliconPowerLimitActions(GameTestHelper helper) {
        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 3, 3));
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState().setValue(
                        buildcraft.transport.block.PipeHolderBlock.TYPE,
                        buildcraft.transport.PipeType.IRON_POWER), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.UP, gate),
                "power limit action gate could not be installed");
        gate = holder.attachment(Direction.UP);

        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.POWER_LIMIT_3))));
        tickPipes(helper, 1, pipePos);
        helper.assertValueEqual(buildcraft.transport.PipeType.IRON_POWER.powerTransferPerTick() >> 3,
                holder.effectivePowerTransferPerTick(),
                "POWER_LIMIT_3 did not apply the expected limiter shift");

        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.POWER_LIMIT_6))));
        tickPipes(helper, 1, pipePos);
        helper.assertValueEqual(0L, holder.effectivePowerTransferPerTick(),
                "POWER_LIMIT_6 did not turn the limiter off");
        helper.succeed();
    }

    private static void siliconPipeDirectionAction(GameTestHelper helper) {
        BlockPos ironPos = helper.absolutePos(new BlockPos(3, 3, 3));
        BlockPos westPos = ironPos.west();
        BlockPos eastPos = ironPos.east();
        var block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState iron = block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.IRON_ITEM);
        BlockState cobble = block.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.COBBLESTONE_ITEM);
        helper.getLevel().setBlock(ironPos, iron, 3);
        helper.getLevel().setBlock(westPos, cobble, 3);
        helper.getLevel().setBlock(eastPos, cobble, 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(ironPos);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.UP, gate),
                "pipe direction action gate could not be installed");
        gate = holder.attachment(Direction.UP);

        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.PIPE_DIRECTION,
                                java.util.Optional.of(Direction.EAST)))));
        tickPipes(helper, 1, ironPos);
        helper.assertValueEqual(Direction.EAST, holder.routingDirection(),
                "PIPE_DIRECTION did not select the east iron-pipe output");

        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.PIPE_DIRECTION,
                                java.util.Optional.of(Direction.WEST)))));
        tickPipes(helper, 1, ironPos);
        helper.assertValueEqual(Direction.WEST, holder.routingDirection(),
                "PIPE_DIRECTION did not replace the iron-pipe output");
        helper.succeed();
    }

    private static void siliconPowerRequestedTrigger(GameTestHelper helper) {
        BlockPos gatePipePos = helper.absolutePos(new BlockPos(2, 3, 3));
        BlockPos networkPipePos = gatePipePos.east();
        BlockPos wellPos = networkPipePos.east();
        BlockState powerPipe = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE,
                buildcraft.transport.PipeType.STONE_POWER);
        helper.getLevel().setBlock(gatePipePos, powerPipe, 3);
        helper.getLevel().setBlock(networkPipePos, powerPipe, 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(gatePipePos);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.NORTH, gate),
                "power requested trigger gate could not be installed");
        gate = holder.attachment(Direction.NORTH);
        assertEngineStageTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_REQUESTED, false,
                "power pipe network without a receiver matched POWER_REQUESTED");

        helper.getLevel().setBlock(wellPos,
                buildcraft.factory.BCFactoryBlocks.MINING_WELL.get().defaultBlockState(), 3);
        assertEngineStageTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_REQUESTED, true,
                "downstream mining well demand did not match POWER_REQUESTED");
        helper.getLevel().removeBlock(wellPos, false);
        assertEngineStageTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_REQUESTED, false,
                "removed downstream receiver still matched POWER_REQUESTED");
        helper.succeed();
    }

    private static void siliconFluidsTraversingTrigger(GameTestHelper helper) {
        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 3, 3));
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState().setValue(
                        buildcraft.transport.block.PipeHolderBlock.TYPE,
                        buildcraft.transport.PipeType.COBBLESTONE_FLUID), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.NORTH, gate),
                "fluids traversing trigger gate could not be installed");
        gate = holder.attachment(Direction.NORTH);
        assertEngineStageTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUIDS_TRAVERSING, false,
                "empty fluid pipe matched FLUIDS_TRAVERSING");

        var handler = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                pipePos, Direction.WEST);
        helper.assertTrue(handler != null, "fluid pipe sided capability missing");
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(handler.insert(water, 200, transaction), 200,
                    "fluid pipe rejected trigger test fluid");
            transaction.commit();
        }
        assertEngineStageTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUIDS_TRAVERSING, true,
                "buffered fluid did not match FLUIDS_TRAVERSING");
        helper.succeed();
    }

    private static void siliconEngineStageTriggers(GameTestHelper helper) {
        double min = buildcraft.energy.block.entity.CombustionEngineBlockEntity.MIN_HEAT;
        double range = buildcraft.energy.block.entity.CombustionEngineBlockEntity.MAX_HEAT - min;
        var stages = new buildcraft.api.enums.EnumPowerStage[] {
                buildcraft.api.enums.EnumPowerStage.BLUE, buildcraft.api.enums.EnumPowerStage.GREEN,
                buildcraft.api.enums.EnumPowerStage.YELLOW, buildcraft.api.enums.EnumPowerStage.RED,
                buildcraft.api.enums.EnumPowerStage.OVERHEAT
        };
        var triggers = new buildcraft.silicon.gate.GateTrigger[] {
                buildcraft.silicon.gate.GateTrigger.ENGINE_BLUE,
                buildcraft.silicon.gate.GateTrigger.ENGINE_GREEN,
                buildcraft.silicon.gate.GateTrigger.ENGINE_YELLOW,
                buildcraft.silicon.gate.GateTrigger.ENGINE_RED,
                buildcraft.silicon.gate.GateTrigger.ENGINE_OVERHEAT
        };
        double[] boundaryHeat = { min, min + range * .25, min + range * .50, min + range * .75,
                min + range * .85 };
        for (int index = 0; index < stages.length; index++) {
            helper.assertValueEqual(stages[index],
                    buildcraft.energy.block.entity.CombustionEngineBlockEntity.stageForHeat(boundaryHeat[index]),
                    "combustion engine stage boundary was incorrect");
            for (int triggerIndex = 0; triggerIndex < triggers.length; triggerIndex++) {
                helper.assertValueEqual(index == triggerIndex, triggers[triggerIndex].matchesEngineStage(stages[index]),
                        "engine stage trigger mapped to the wrong stage");
            }
        }
        helper.assertValueEqual(buildcraft.api.enums.EnumPowerStage.OVERHEAT,
                buildcraft.energy.block.entity.CombustionEngineBlockEntity.stageForHeat(min + range * .85),
                "combustion engine overheat boundary was incorrect");

        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 3, 3));
        BlockPos enginePos = pipePos.north();
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(enginePos, buildcraft.core.BCCoreBlocks.ENGINE.get().defaultBlockState()
                .setValue(buildcraft.core.block.BlockEngine.ENGINE_TYPE,
                        buildcraft.api.enums.EnumEngineType.STONE)
                .setValue(buildcraft.core.block.BlockEngine.FACING, Direction.UP), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.NORTH, gate),
                "engine stage trigger gate could not be installed");
        gate = holder.attachment(Direction.NORTH);
        for (int index = 0; index < triggers.length; index++) {
            assertEngineStageTrigger(helper, holder, gate, triggers[index], index == 0,
                    "stirling engine reported the wrong Gate power stage");
        }
        helper.getLevel().removeBlock(enginePos, false);
        assertEngineStageTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.ENGINE_BLUE, false,
                "missing engine triggered ENGINE_BLUE");
        helper.succeed();
    }

    private static void assertEngineStageTrigger(GameTestHelper helper,
            buildcraft.transport.block.entity.PipeHolderBlockEntity holder, ItemStack gate,
            buildcraft.silicon.gate.GateTrigger trigger, boolean expected, String message) {
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(trigger,
                                buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT))));
        tickPipes(helper, 1, holder.getBlockPos());
        helper.assertValueEqual(expected, holder.gateRedstoneOutput(), message);
    }

    private static void siliconGateParameterTriggers(GameTestHelper helper) {
        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 3, 3));
        BlockPos targetPos = pipePos.north();
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(targetPos, Blocks.CHEST.defaultBlockState(), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(pipePos);
        var chest = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(targetPos);
        chest.setItem(0, new ItemStack(Items.IRON_INGOT));
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.NORTH, gate), "parameter trigger gate could not be installed");
        gate = holder.attachment(Direction.NORTH);
        assertParameterizedTrigger(helper, holder, gate, buildcraft.silicon.gate.GateTrigger.INVENTORY_CONTAINS,
                new ItemStack(Items.IRON_INGOT), true, "matching inventory parameter did not trigger");
        assertParameterizedTrigger(helper, holder, gate, buildcraft.silicon.gate.GateTrigger.INVENTORY_CONTAINS,
                new ItemStack(Items.GOLD_INGOT), false, "wrong inventory parameter triggered");

        helper.getLevel().removeBlock(targetPos, false);
        helper.getLevel().setBlock(targetPos, buildcraft.factory.BCFactoryBlocks.TANK.get().defaultBlockState(), 3);
        var fluid = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, targetPos, Direction.SOUTH);
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(500, fluid.insert(water, 500, transaction), "parameter trigger tank rejected water");
            transaction.commit();
        }
        assertParameterizedTrigger(helper, holder, gate, buildcraft.silicon.gate.GateTrigger.FLUID_CONTAINS,
                new ItemStack(Items.WATER_BUCKET), true, "matching fluid parameter did not trigger");
        assertParameterizedTrigger(helper, holder, gate, buildcraft.silicon.gate.GateTrigger.FLUID_CONTAINS,
                new ItemStack(Items.LAVA_BUCKET), false, "wrong fluid parameter triggered");
        assertParameterizedTrigger(helper, holder, gate, buildcraft.silicon.gate.GateTrigger.FLUID_SPACE,
                new ItemStack(Items.WATER_BUCKET), true, "matching fluid space parameter did not trigger");
        assertParameterizedTrigger(helper, holder, gate, buildcraft.silicon.gate.GateTrigger.FLUID_SPACE,
                new ItemStack(Items.LAVA_BUCKET), false, "incompatible fluid space parameter triggered");
        helper.succeed();
    }

    private static void buildersFillerGatePatterns(GameTestHelper helper) {
        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 3, 3));
        BlockPos fillerPos = pipePos.north();
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(fillerPos,
                buildcraft.builders.BCBuildersBlocks.FILLER.get().defaultBlockState(), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        var filler = (buildcraft.builders.block.entity.FillerBlockEntity)
                helper.getLevel().getBlockEntity(fillerPos);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.NORTH, gate),
                "filler pattern gate could not be installed");
        gate = holder.attachment(Direction.NORTH);
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.FILLER_PYRAMID,
                                java.util.Optional.empty(), java.util.List.of(), java.util.List.of(1, 8)))));
        tickPipes(helper, 1, pipePos);
        helper.assertValueEqual(buildcraft.builders.FillerPattern.PYRAMID, filler.pattern(),
                "pyramid gate action did not select its pattern");
        helper.assertValueEqual(Direction.DOWN, filler.verticalDirection(),
                "pyramid gate vertical parameter was ignored");
        helper.assertValueEqual(8, filler.pyramidCenter(),
                "pyramid gate centre parameter was ignored");

        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.FILLER_CIRCLE,
                                java.util.Optional.empty(), java.util.List.of(), java.util.List.of(2, 1, 3)))));
        tickPipes(helper, 1, pipePos);
        helper.assertValueEqual(buildcraft.builders.FillerPattern.CIRCLE, filler.pattern(),
                "2D gate action did not select its pattern");
        helper.assertValueEqual(Direction.Axis.Z, filler.shapeAxis(), "2D axis parameter was ignored");
        helper.assertValueEqual(buildcraft.builders.FillerFillMode.FILLED_OUTER, filler.fillMode(),
                "2D fill parameter was ignored");
        helper.assertValueEqual(3, filler.shapeRotation(), "2D rotation parameter was ignored");
        helper.assertValueEqual(buildcraft.builders.FillerPattern.values().length,
                (int) java.util.Arrays.stream(buildcraft.silicon.gate.GateAction.values())
                        .filter(action -> buildcraft.builders.BuildersGateActions.pattern(action) != null).count(),
                "not every active Filler pattern has a gate action");
        helper.succeed();
    }

    private static void roboticsGateListParameters(GameTestHelper helper) {
        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 3, 3));
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState(), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        helper.assertTrue(holder.installAttachment(Direction.NORTH,
                new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get())),
                "Robotics List test could not install its Station");

        ItemStack itemList = new ItemStack(BCCoreItems.LIST.get());
        ItemList.setData(itemList, new ListData("Gate items", java.util.List.of(
                new ListLineData(java.util.List.of(new ItemStack(Items.IRON_INGOT)),
                        false, buildcraft.api.lists.ListMatchMode.TYPE), ListLineData.empty())));
        ItemStack robotList = new ItemStack(BCCoreItems.LIST.get());
        ItemList.setData(robotList, new ListData("Gate robots", java.util.List.of(
                new ListLineData(java.util.List.of(buildcraft.robotics.item.RobotItem.create(
                        buildcraft.robotics.RobotBoardType.CARRIER, 0)),
                        true, buildcraft.api.lists.ListMatchMode.DIRECT), ListLineData.empty())));

        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.STATION_PROVIDE_ITEMS,
                                java.util.Optional.empty(), java.util.List.of(itemList)),
                        new buildcraft.silicon.gate.GateRule(buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.STATION_FORCE_ROBOT,
                                java.util.Optional.empty(), java.util.List.of(robotList)))));
        helper.assertTrue(holder.installAttachment(Direction.UP, gate),
                "Robotics List test could not install its Gate");
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), pipePos, Direction.NORTH);
        tickPipes(helper, 1, pipePos);
        var address = new buildcraft.robotics.RobotStationRegistry.Address(pipePos, Direction.NORTH);
        helper.assertTrue(buildcraft.robotics.RoboticsGateActions.providesItem(helper.getLevel(), address,
                buildcraft.robotics.RobotStationConfig.DEFAULT, new ItemStack(Items.GOLD_INGOT)),
                "type-matching List did not admit another ingot");
        helper.assertFalse(buildcraft.robotics.RoboticsGateActions.providesItem(helper.getLevel(), address,
                buildcraft.robotics.RobotStationConfig.DEFAULT, new ItemStack(Items.COBBLESTONE)),
                "type-matching List admitted an unrelated item");
        helper.assertTrue(buildcraft.robotics.RoboticsGateActions.permitsRobot(helper.getLevel(), address,
                buildcraft.robotics.RobotBoardType.CARRIER), "Robot List rejected its configured board");
        helper.assertFalse(buildcraft.robotics.RoboticsGateActions.permitsRobot(helper.getLevel(), address,
                buildcraft.robotics.RobotBoardType.PICKER), "Robot List admitted an unconfigured board");
        helper.succeed();
    }

    private static void transportCreativePipeEntries(GameTestHelper helper) {
        java.util.EnumSet<buildcraft.transport.PipeType> types =
                java.util.EnumSet.noneOf(buildcraft.transport.PipeType.class);
        for (buildcraft.transport.item.PipeItem item : buildcraft.transport.BCTransportItems.pipeItems()) {
            helper.assertTrue(types.add(item.pipeType()),
                    "creative pipe list contains duplicate " + item.pipeType().getSerializedName());
        }
        helper.assertValueEqual(java.util.EnumSet.allOf(buildcraft.transport.PipeType.class), types,
                "creative pipe list does not contain every registered PipeType");
        helper.succeed();
    }

    private static void siliconLensVariants(GameTestHelper helper) {
        var variants = buildcraft.silicon.BCSiliconItems.lensVariants();
        helper.assertValueEqual(34, variants.size(), "creative tab did not expose all historical Lens variants");
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (ItemStack stack : variants) {
            var color = java.util.Optional.ofNullable(stack.get(
                    buildcraft.silicon.BCSiliconDataComponents.LENS_COLOR.get()));
            boolean filter = stack.getOrDefault(
                    buildcraft.silicon.BCSiliconDataComponents.LENS_FILTER.get(), false);
            helper.assertTrue(keys.add(color.map(DyeColor::getName).orElse("clear") + ":" + filter),
                    "creative tab contains a duplicate Lens variant");
            var attachment = (buildcraft.transport.item.LensAttachment) stack.getItem();
            helper.assertValueEqual(color, attachment.lensColor(stack),
                    "Lens attachment lost its optional colour");
        }

        var clearLensInput = new buildcraft.silicon.recipe.AssemblyRecipeInput(
                java.util.List.of(new ItemStack(Blocks.GLASS)), null);
        ItemStack clearLens = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                buildcraft.silicon.BCSiliconRecipes.ASSEMBLY_TYPE.get(), clearLensInput, helper.getLevel())
                .orElseThrow().value().assemble(clearLensInput);
        helper.assertTrue(clearLens.is(buildcraft.silicon.BCSiliconItems.PLUG_LENS.get())
                        && clearLens.get(buildcraft.silicon.BCSiliconDataComponents.LENS_COLOR.get()) == null
                        && !clearLens.getOrDefault(buildcraft.silicon.BCSiliconDataComponents.LENS_FILTER.get(), false),
                "clear Lens assembly recipe returned the wrong variant");
        var clearFilterInput = new buildcraft.silicon.recipe.AssemblyRecipeInput(
                java.util.List.of(new ItemStack(Blocks.GLASS), new ItemStack(Blocks.IRON_BARS)), null);
        ItemStack clearFilter = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                buildcraft.silicon.BCSiliconRecipes.ASSEMBLY_TYPE.get(), clearFilterInput, helper.getLevel())
                .orElseThrow().value().assemble(clearFilterInput);
        helper.assertTrue(clearFilter.getOrDefault(
                        buildcraft.silicon.BCSiliconDataComponents.LENS_FILTER.get(), false)
                        && clearFilter.get(buildcraft.silicon.BCSiliconDataComponents.LENS_COLOR.get()) == null,
                "clear Filter assembly recipe returned the wrong variant");
        helper.succeed();
    }

    private static void assertParameterizedTrigger(GameTestHelper helper,
            buildcraft.transport.block.entity.PipeHolderBlockEntity holder, ItemStack gate,
            buildcraft.silicon.gate.GateTrigger trigger, ItemStack parameter, boolean expected, String message) {
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(new buildcraft.silicon.gate.GateRule(
                        trigger, buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT,
                        java.util.Optional.empty(), java.util.List.of(parameter)))));
        tickPipes(helper, 1, holder.getBlockPos());
        helper.assertValueEqual(expected, holder.gateRedstoneOutput(), message);
    }

    private static void siliconMachineTriggers(GameTestHelper helper) {
        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 3, 3));
        BlockPos wellPos = pipePos.north();
        BlockPos targetPos = wellPos.below();
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(wellPos,
                buildcraft.factory.BCFactoryBlocks.MINING_WELL.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(targetPos, Blocks.STONE.defaultBlockState(), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        var well = (buildcraft.factory.block.entity.MiningWellBlockEntity)
                helper.getLevel().getBlockEntity(wellPos);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.NORTH, gate),
                "machine trigger gate could not be installed");
        gate = holder.attachment(Direction.NORTH);

        buildcraft.factory.block.entity.MiningWellBlockEntity.tick(
                helper.getLevel(), wellPos, helper.getLevel().getBlockState(wellPos), well);
        helper.assertTrue(well.hasWork(), "mining well did not discover its stone target");
        assertMachineTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.MACHINE_ACTIVE, true,
                "working mining well did not trigger MACHINE_ACTIVE");
        assertMachineTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.MACHINE_INACTIVE, false,
                "working mining well triggered MACHINE_INACTIVE");

        helper.getLevel().setBlock(targetPos, Blocks.BEDROCK.defaultBlockState(), 3);
        buildcraft.factory.block.entity.MiningWellBlockEntity.tick(
                helper.getLevel(), wellPos, helper.getLevel().getBlockState(wellPos), well);
        helper.assertFalse(well.hasWork(), "mining well retained work across an unbreakable target");
        assertMachineTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.MACHINE_ACTIVE, false,
                "idle mining well triggered MACHINE_ACTIVE");
        assertMachineTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.MACHINE_INACTIVE, true,
                "idle mining well did not trigger MACHINE_INACTIVE");

        helper.getLevel().removeBlock(wellPos, false);
        assertMachineTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.MACHINE_ACTIVE, false,
                "missing machine triggered MACHINE_ACTIVE");
        assertMachineTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.MACHINE_INACTIVE, false,
                "missing machine triggered MACHINE_INACTIVE");
        helper.succeed();
    }

    private static void assertMachineTrigger(GameTestHelper helper,
            buildcraft.transport.block.entity.PipeHolderBlockEntity holder, ItemStack gate,
            buildcraft.silicon.gate.GateTrigger trigger, boolean expected, String message) {
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(trigger,
                                buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT))));
        tickPipes(helper, 1, holder.getBlockPos());
        helper.assertValueEqual(expected, holder.gateRedstoneOutput(), message);
    }

    private static void siliconPowerTriggers(GameTestHelper helper) {
        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos laserPos = pipePos.north();
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(laserPos,
                buildcraft.silicon.BCSiliconBlocks.LASER.get().defaultBlockState(), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.NORTH, gate),
                "power trigger gate could not be installed");
        gate = holder.attachment(Direction.NORTH);
        var receiver = helper.getLevel().getCapability(buildcraft.api.mj.MjAPI.CAP_RECEIVER,
                laserPos, Direction.SOUTH);
        var readable = helper.getLevel().getCapability(buildcraft.api.mj.MjAPI.CAP_READABLE,
                laserPos, Direction.SOUTH);
        helper.assertTrue(receiver != null && readable != null,
                "adjacent laser exposed no MJ receiver/readable capability");
        long capacity = readable.getCapacity();
        helper.assertTrue(capacity > 20, "laser battery capacity was too small for boundary test");

        assertPowerTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_LOW, true, "empty laser did not trigger POWER_LOW");
        assertPowerTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_HIGH, false, "empty laser triggered POWER_HIGH");
        long fivePercent = capacity / 20;
        receiver.receivePower(fivePercent, false);
        helper.assertValueEqual(fivePercent, readable.getStored(), "laser did not store exact five-percent charge");
        assertPowerTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_LOW, false,
                "laser at exactly five percent triggered POWER_LOW");
        assertPowerTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_HIGH, false,
                "laser at five percent triggered POWER_HIGH");

        long ninetyFivePercent = capacity * 95 / 100;
        receiver.receivePower(ninetyFivePercent - readable.getStored(), false);
        helper.assertValueEqual(ninetyFivePercent, readable.getStored(),
                "laser did not store exact ninety-five-percent charge");
        assertPowerTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_HIGH, false,
                "laser at exactly ninety-five percent triggered POWER_HIGH");
        receiver.receivePower(1, false);
        assertPowerTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_HIGH, true,
                "laser above ninety-five percent did not trigger POWER_HIGH");
        assertPowerTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_LOW, false,
                "charged laser triggered POWER_LOW");

        helper.getLevel().removeBlock(laserPos, false);
        assertPowerTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_LOW, false,
                "missing MJ target triggered POWER_LOW");
        assertPowerTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.POWER_HIGH, false,
                "missing MJ target triggered POWER_HIGH");
        helper.succeed();
    }

    private static void assertPowerTrigger(GameTestHelper helper,
            buildcraft.transport.block.entity.PipeHolderBlockEntity holder, ItemStack gate,
            buildcraft.silicon.gate.GateTrigger trigger, boolean expected, String message) {
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(trigger,
                                buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT))));
        tickPipes(helper, 1, holder.getBlockPos());
        helper.assertValueEqual(expected, holder.gateRedstoneOutput(), message);
    }

    private static void siliconFluidTriggers(GameTestHelper helper) {
        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos tankPos = pipePos.north();
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(tankPos,
                buildcraft.factory.BCFactoryBlocks.TANK.get().defaultBlockState(), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.NORTH, gate),
                "fluid trigger gate could not be installed");
        gate = holder.attachment(Direction.NORTH);
        var handler = helper.getLevel().getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                tankPos, Direction.SOUTH);
        helper.assertTrue(handler != null, "adjacent tank exposed no fluid capability");
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);

        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_EMPTY, true, "empty tank did not trigger FLUID_EMPTY");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_CONTAINS, false, "empty tank triggered FLUID_CONTAINS");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_SPACE, true, "empty tank did not trigger FLUID_SPACE");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_FULL, false, "empty tank triggered FLUID_FULL");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_BELOW_25, true,
                "empty tank did not trigger FLUID_BELOW_25");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_BELOW_50, true,
                "empty tank did not trigger FLUID_BELOW_50");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_BELOW_75, true,
                "empty tank did not trigger FLUID_BELOW_75");

        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(500, handler.insert(water, 500, transaction),
                    "tank rejected partial trigger-test fill");
            transaction.commit();
        }
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_EMPTY, false, "partial tank triggered FLUID_EMPTY");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_CONTAINS, true, "partial tank did not trigger FLUID_CONTAINS");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_SPACE, true, "partial tank did not trigger FLUID_SPACE");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_FULL, false, "partial tank triggered FLUID_FULL");

        int tankCapacity = Math.toIntExact(handler.getCapacityAsLong(0, water));
        int toQuarter = tankCapacity / 4 - Math.toIntExact(handler.getAmountAsLong(0));
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(toQuarter, handler.insert(water, toQuarter, transaction),
                    "tank rejected fill to 25 percent");
            transaction.commit();
        }
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_BELOW_25, false,
                "tank at exactly 25 percent triggered FLUID_BELOW_25");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_BELOW_50, true,
                "tank at 25 percent did not trigger FLUID_BELOW_50");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_BELOW_75, true,
                "tank at 25 percent did not trigger FLUID_BELOW_75");
        int toHalf = tankCapacity / 2 - Math.toIntExact(handler.getAmountAsLong(0));
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(toHalf, handler.insert(water, toHalf, transaction),
                    "tank rejected fill to 50 percent");
            transaction.commit();
        }
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_BELOW_50, false,
                "tank at exactly 50 percent triggered FLUID_BELOW_50");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_BELOW_75, true,
                "tank at 50 percent did not trigger FLUID_BELOW_75");
        int toThreeQuarters = tankCapacity * 3 / 4 - Math.toIntExact(handler.getAmountAsLong(0));
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(toThreeQuarters, handler.insert(water, toThreeQuarters, transaction),
                    "tank rejected fill to 75 percent");
            transaction.commit();
        }
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_BELOW_75, false,
                "tank at exactly 75 percent triggered FLUID_BELOW_75");

        int remaining = Math.toIntExact(handler.getCapacityAsLong(0, water) - handler.getAmountAsLong(0));
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(remaining, handler.insert(water, remaining, transaction),
                    "tank rejected full trigger-test fill");
            transaction.commit();
        }
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_EMPTY, false, "full tank triggered FLUID_EMPTY");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_CONTAINS, true, "full tank did not trigger FLUID_CONTAINS");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_SPACE, false, "full tank triggered FLUID_SPACE");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_FULL, true, "full tank did not trigger FLUID_FULL");

        helper.getLevel().removeBlock(tankPos, false);
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_EMPTY, false, "missing tank triggered FLUID_EMPTY");
        assertFluidTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.FLUID_FULL, false, "missing tank triggered FLUID_FULL");
        helper.succeed();
    }

    private static void assertFluidTrigger(GameTestHelper helper,
            buildcraft.transport.block.entity.PipeHolderBlockEntity holder, ItemStack gate,
            buildcraft.silicon.gate.GateTrigger trigger, boolean expected, String message) {
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(trigger,
                                buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT))));
        tickPipes(helper, 1, holder.getBlockPos());
        helper.assertValueEqual(expected, holder.gateRedstoneOutput(), message);
    }

    private static void siliconInventoryTriggers(GameTestHelper helper) {
        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 2, 3));
        BlockPos chestPos = pipePos.north();
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        helper.assertTrue(holder.installAttachment(Direction.NORTH, gate),
                "inventory trigger gate could not be installed");
        gate = holder.attachment(Direction.NORTH);
        var chest = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(chestPos);

        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_EMPTY, true, "empty inventory did not trigger EMPTY");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_CONTAINS, false, "empty inventory triggered CONTAINS");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_SPACE, true, "empty inventory did not trigger SPACE");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_FULL, false, "empty inventory triggered FULL");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_BELOW_25, true,
                "empty inventory did not trigger BELOW_25");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_BELOW_50, true,
                "empty inventory did not trigger BELOW_50");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_BELOW_75, true,
                "empty inventory did not trigger BELOW_75");

        chest.setItem(0, new ItemStack(Items.COAL));
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_EMPTY, false, "partial inventory triggered EMPTY");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_CONTAINS, true, "partial inventory did not trigger CONTAINS");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_SPACE, true, "partial inventory did not trigger SPACE");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_FULL, false, "partial inventory triggered FULL");

        setContainerItemCount(chest, Items.COAL, 432);
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_BELOW_25, false,
                "inventory at exactly 25 percent triggered BELOW_25");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_BELOW_50, true,
                "inventory at 25 percent did not trigger BELOW_50");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_BELOW_75, true,
                "inventory at 25 percent did not trigger BELOW_75");
        setContainerItemCount(chest, Items.COAL, 864);
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_BELOW_50, false,
                "inventory at exactly 50 percent triggered BELOW_50");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_BELOW_75, true,
                "inventory at 50 percent did not trigger BELOW_75");
        setContainerItemCount(chest, Items.COAL, 1296);
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_BELOW_75, false,
                "inventory at exactly 75 percent triggered BELOW_75");

        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.COAL, 64));
        }
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_EMPTY, false, "full inventory triggered EMPTY");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_CONTAINS, true, "full inventory did not trigger CONTAINS");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_SPACE, false, "full inventory triggered SPACE");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_FULL, true, "full inventory did not trigger FULL");

        helper.getLevel().removeBlock(chestPos, false);
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_EMPTY, false, "missing inventory triggered EMPTY");
        assertInventoryTrigger(helper, holder, gate,
                buildcraft.silicon.gate.GateTrigger.INVENTORY_FULL, false, "missing inventory triggered FULL");
        helper.succeed();
    }

    private static void assertInventoryTrigger(GameTestHelper helper,
            buildcraft.transport.block.entity.PipeHolderBlockEntity holder, ItemStack gate,
            buildcraft.silicon.gate.GateTrigger trigger, boolean expected, String message) {
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(trigger,
                                buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT))));
        tickPipes(helper, 1, holder.getBlockPos());
        helper.assertValueEqual(expected, holder.gateRedstoneOutput(), message);
    }

    private static void setContainerItemCount(net.minecraft.world.Container container,
            net.minecraft.world.item.Item item, int count) {
        container.clearContent();
        for (int slot = 0; slot < container.getContainerSize() && count > 0; slot++) {
            int amount = Math.min(64, count);
            container.setItem(slot, new ItemStack(item, amount));
            count -= amount;
        }
    }

    private static void siliconFacade(GameTestHelper helper) {
        var facadeVariants = buildcraft.silicon.BCSiliconItems.facadeVariants();
        int eligibleFacadeItems = 0;
        java.util.Set<net.minecraft.world.level.block.Block> eligibleFacadeBlocks = new java.util.HashSet<>();
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof net.minecraft.world.item.BlockItem blockItem && item != Items.AIR
                    && !blockItem.getBlock().defaultBlockState().isAir()) {
                eligibleFacadeItems++;
                eligibleFacadeBlocks.add(blockItem.getBlock());
            }
        }
        helper.assertValueEqual(eligibleFacadeItems * 2, facadeVariants.size(),
                "creative facade catalog did not include solid and hollow form of every BlockItem");
        java.util.Map<net.minecraft.world.level.block.Block, Integer> facadeForms = new java.util.HashMap<>();
        for (ItemStack variant : facadeVariants) {
            helper.assertTrue(variant.is(buildcraft.silicon.BCSiliconItems.PLUG_FACADE.get()),
                    "facade catalog contained a non-facade item");
            BlockState variantState = variant.get(buildcraft.silicon.BCSiliconDataComponents.FACADE_STATE.get());
            helper.assertTrue(variantState != null, "facade catalog variant lacked appearance state");
            int form = variant.getOrDefault(buildcraft.silicon.BCSiliconDataComponents.FACADE_HOLLOW.get(), false)
                    ? 2 : 1;
            facadeForms.merge(variantState.getBlock(), form, (left, right) -> left | right);
        }
        helper.assertValueEqual(eligibleFacadeBlocks.size(), facadeForms.size(),
                "facade catalog covered wrong set of blocks");
        for (var entry : facadeForms.entrySet()) {
            helper.assertValueEqual(3, entry.getValue(),
                    "facade catalog did not include both forms for " + entry.getKey());
        }

        var input = new buildcraft.silicon.recipe.AssemblyRecipeInput(java.util.List.of(
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get(), 3),
                new ItemStack(Blocks.OAK_PLANKS)), null);
        var recipe = buildcraft.silicon.recipe.FacadeRecipe.INSTANCE;
        helper.assertTrue(recipe.matches(input, helper.getLevel()),
                "facade recipe rejected three Structure Pipes and a block");
        ItemStack facade = recipe.assemble(input);
        helper.assertTrue(facade.is(buildcraft.silicon.BCSiliconItems.PLUG_FACADE.get()),
                "facade recipe produced the wrong item");
        helper.assertValueEqual(6, facade.getCount(), "facade recipe did not produce six covers");
        helper.assertValueEqual(64 * buildcraft.api.mj.MjAPI.MJ, recipe.requiredPower(),
                "facade recipe has the wrong Assembly Table power cost");
        helper.assertValueEqual(Blocks.OAK_PLANKS.defaultBlockState(),
                facade.get(buildcraft.silicon.BCSiliconDataComponents.FACADE_STATE.get()),
                "facade recipe did not capture the block state");

        BlockPos pipePos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlock(pipePos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState(), 3);
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pipePos);
        helper.assertTrue(holder.installAttachment(Direction.NORTH, facade),
                "crafted facade could not be installed");
        ItemStack removed = holder.takeAttachment(Direction.NORTH);
        helper.assertValueEqual(Blocks.OAK_PLANKS.defaultBlockState(),
                removed.get(buildcraft.silicon.BCSiliconDataComponents.FACADE_STATE.get()),
                "removed facade lost its block state");
        helper.assertTrue(holder.installAttachment(Direction.SOUTH, removed),
                "state-preserving facade could not be reinstalled");
        helper.assertTrue(holder.attachmentBlocksConnection(Direction.SOUTH),
                "solid facade did not block its pipe connection");

        var swapInput = net.minecraft.world.item.crafting.CraftingInput.of(
                1, 1, java.util.List.of(facade.copyWithCount(1)));
        ItemStack hollowFacade = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, swapInput, helper.getLevel())
                .orElseThrow(() -> new AssertionError("facade swap recipe was not loaded"))
                .value().assemble(swapInput);
        helper.assertTrue(hollowFacade.getOrDefault(
                buildcraft.silicon.BCSiliconDataComponents.FACADE_HOLLOW.get(), false),
                "facade swap recipe did not produce hollow facade");
        helper.assertValueEqual(Blocks.OAK_PLANKS.defaultBlockState(),
                hollowFacade.get(buildcraft.silicon.BCSiliconDataComponents.FACADE_STATE.get()),
                "facade swap recipe lost appearance state");
        helper.assertTrue(holder.installAttachment(Direction.EAST, hollowFacade),
                "hollow facade could not be installed");
        helper.assertFalse(holder.attachmentBlocksConnection(Direction.EAST),
                "hollow facade blocked its pipe connection");
        var reverseSwapInput = net.minecraft.world.item.crafting.CraftingInput.of(
                1, 1, java.util.List.of(hollowFacade));
        ItemStack solidFacade = buildcraft.silicon.recipe.FacadeSwapRecipe.INSTANCE.assemble(reverseSwapInput);
        helper.assertFalse(solidFacade.getOrDefault(
                buildcraft.silicon.BCSiliconDataComponents.FACADE_HOLLOW.get(), false),
                "second facade swap did not restore solid facade");

        var invalid = new buildcraft.silicon.recipe.AssemblyRecipeInput(java.util.List.of(
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get(), 3),
                new ItemStack(Items.COAL)), null);
        helper.assertFalse(recipe.matches(invalid, helper.getLevel()),
                "facade recipe accepted a non-block appearance item");
        BlockPos tablePos = helper.absolutePos(new BlockPos(6, 2, 3));
        helper.getLevel().setBlock(tablePos, buildcraft.silicon.BCSiliconBlocks.ASSEMBLY_TABLE.get()
                .defaultBlockState(), Block.UPDATE_ALL);
        var table = (buildcraft.silicon.block.entity.AssemblyTableBlockEntity)
                helper.getLevel().getBlockEntity(tablePos);
        table.inventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(
                buildcraft.transport.BCTransportItems.PIPE_STRUCTURE.get()), 3);
        table.inventory().set(1, net.neoforged.neoforge.transfer.item.ItemResource.of(Blocks.OAK_PLANKS.asItem()), 1);
        helper.assertValueEqual(64 * buildcraft.api.mj.MjAPI.MJ, table.getRequiredLaserPower(),
                "Assembly Table did not select the facade recipe");
        helper.assertValueEqual(0L, table.receiveLaserPower(table.getRequiredLaserPower()),
                "Assembly Table rejected facade recipe power");
        buildcraft.silicon.block.entity.AssemblyTableBlockEntity.tick(
                helper.getLevel(), tablePos, helper.getLevel().getBlockState(tablePos), table);
        int assembledFacades = 0;
        for (int slot = 0; slot < table.inventory().size(); slot++) {
            if (table.inventory().getResource(slot).value() == buildcraft.silicon.BCSiliconItems.PLUG_FACADE.get()) {
                assembledFacades += table.inventory().getAmountAsInt(slot);
            }
        }
        helper.assertValueEqual(6, assembledFacades, "Assembly Table did not produce six facades");
        helper.assertValueEqual(0L, table.storedLaserPower(), "facade assembly did not consume its power");
        helper.succeed();
    }

    private static void siliconLensRouting(GameTestHelper helper) {
        var pipeBlock = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState wood = pipeBlock.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.WOOD_ITEM);
        BlockState cobble = pipeBlock.defaultBlockState().setValue(
                buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_ITEM);
        BlockPos sourcePos = helper.absolutePos(new BlockPos(1, 2, 3));
        BlockPos woodPos = sourcePos.east();
        BlockPos centerPos = woodPos.east();
        BlockPos blueTargetPos = centerPos.east();
        BlockPos redTargetPos = centerPos.north();
        BlockPos openTargetPos = centerPos.south();
        helper.getLevel().setBlock(sourcePos, Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(woodPos, wood, 3);
        helper.getLevel().setBlock(centerPos, cobble, 3);
        helper.getLevel().setBlock(blueTargetPos, Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(redTargetPos, Blocks.CHEST.defaultBlockState(), 3);
        helper.getLevel().setBlock(openTargetPos, Blocks.CHEST.defaultBlockState(), 3);
        var source = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(sourcePos);
        var blueTarget = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(blueTargetPos);
        var redTarget = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(redTargetPos);
        var openTarget = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(openTargetPos);
        var woodHolder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(woodPos);
        var centerHolder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(centerPos);
        helper.assertTrue(woodHolder.installAttachment(Direction.WEST,
                        buildcraft.silicon.BCSiliconItems.lens(DyeColor.BLUE, false)),
                "blue lens could not be installed on the item input");
        helper.assertTrue(centerHolder.installAttachment(Direction.EAST,
                        buildcraft.silicon.BCSiliconItems.lens(DyeColor.BLUE, true)),
                "blue filter could not be installed on its branch");
        helper.assertTrue(centerHolder.installAttachment(Direction.NORTH,
                        buildcraft.silicon.BCSiliconItems.lens(DyeColor.RED, true)),
                "red filter could not be installed on its branch");
        source.setItem(0, new ItemStack(Items.COAL));
        tickPipes(helper, 1, woodPos, centerPos);
        helper.assertValueEqual(Direction.WEST, woodHolder.extractionDirection(),
                "lens test wood pipe did not face its source");
        woodHolder.mjReceiver().receivePower(buildcraft.api.mj.MjAPI.MJ, false);
        tickPipes(helper, 50, woodPos, centerPos);
        helper.assertValueEqual(1, containerCount(blueTarget, Items.COAL),
                "blue-painted item did not choose the matching blue filter");
        helper.assertValueEqual(0, containerCount(redTarget, Items.COAL),
                "blue-painted item crossed the mismatched red filter");

        ItemStack removedLens = woodHolder.takeAttachment(Direction.WEST);
        helper.assertValueEqual(DyeColor.BLUE,
                removedLens.get(buildcraft.silicon.BCSiliconDataComponents.LENS_COLOR.get()),
                "removed lens lost its color component");
        source.setItem(0, new ItemStack(Items.COAL));
        woodHolder.mjReceiver().receivePower(buildcraft.api.mj.MjAPI.MJ, false);
        tickPipes(helper, 50, woodPos, centerPos);
        helper.assertValueEqual(1, containerCount(openTarget, Items.COAL),
                "uncolored item did not prefer the unfiltered branch");
        helper.assertValueEqual(1, containerCount(blueTarget, Items.COAL),
                "uncolored item incorrectly preferred the blue filter");
        ItemStack removedFilter = centerHolder.takeAttachment(Direction.NORTH);
        helper.assertTrue(removedFilter.getOrDefault(
                        buildcraft.silicon.BCSiliconDataComponents.LENS_FILTER.get(), false),
                "removed filter lost its filter component");
        helper.assertValueEqual(DyeColor.RED,
                removedFilter.get(buildcraft.silicon.BCSiliconDataComponents.LENS_COLOR.get()),
                "removed filter lost its color component");
        helper.succeed();
    }

    private static void transportColoredItemPipes(GameTestHelper helper) {
        buildcraft.transport.block.PipeHolderBlock block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState obsidian = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.OBSIDIAN_ITEM
        );
        BlockState lapis = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.LAPIS_ITEM
        );
        BlockPos obsidianPos = helper.absolutePos(new BlockPos(0, 1, 1));
        BlockPos outputPos = obsidianPos.east();
        helper.getLevel().setBlock(obsidianPos, obsidian, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(outputPos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        buildcraft.transport.block.entity.PipeHolderBlockEntity obsidianHolder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(obsidianPos);
        helper.assertTrue(obsidianHolder != null, "obsidian pipe holder missing");
        net.minecraft.world.entity.item.ItemEntity colliding = new net.minecraft.world.entity.item.ItemEntity(
            helper.getLevel(), obsidianPos.getX() + 0.5, obsidianPos.getY() + 0.5, obsidianPos.getZ() + 0.5,
            new ItemStack(Items.OBSIDIAN, 2)
        );
        helper.getLevel().addFreshEntity(colliding);
        obsidianHolder.absorbCollidingItem(colliding);
        helper.assertTrue(colliding.isRemoved(), "obsidian pipe did not absorb colliding item");
        helper.assertValueEqual(obsidianHolder.travellingCount(), 1, "obsidian pipe did not enqueue colliding item");
        helper.assertTrue(!buildcraft.transport.PipeType.OBSIDIAN_ITEM.connectsTo(
            buildcraft.transport.PipeType.OBSIDIAN_ITEM), "obsidian pipes connected to each other");
        net.minecraft.world.entity.item.ItemEntity distant = new net.minecraft.world.entity.item.ItemEntity(
            helper.getLevel(), obsidianPos.getX() - 1.5, obsidianPos.getY() + 0.5, obsidianPos.getZ() + 0.5,
            new ItemStack(Items.DIAMOND)
        );
        helper.getLevel().addFreshEntity(distant);
        buildcraft.api.mj.IMjRedstoneReceiver obsidianReceiver = obsidianHolder.mjReceiver();
        helper.assertTrue(obsidianReceiver != null, "obsidian pipe MJ receiver missing");
        helper.assertValueEqual(obsidianReceiver.receivePower(4 * MjAPI.MJ, true), 3 * MjAPI.MJ,
            "obsidian pipe simulated wrong distance-two suction cost");
        helper.assertTrue(!distant.isRemoved(), "obsidian pipe simulation removed distant item");
        helper.assertValueEqual(obsidianReceiver.receivePower(4 * MjAPI.MJ, false), 3 * MjAPI.MJ,
            "obsidian pipe consumed wrong distance-two suction cost");
        helper.assertTrue(distant.isRemoved(), "obsidian pipe did not absorb distant powered item");

        BlockPos lapisPos = helper.absolutePos(new BlockPos(5, 1, 1));
        BlockPos lapisOutputPos = lapisPos.east();
        helper.getLevel().setBlock(lapisPos, lapis, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(lapisOutputPos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        buildcraft.transport.block.entity.PipeHolderBlockEntity lapisHolder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(lapisPos);
        helper.assertTrue(lapisHolder != null, "lapis pipe holder missing");
        helper.assertTrue(lapisHolder.pipeColor() == net.minecraft.world.item.DyeColor.WHITE,
            "lapis pipe default color was not white");
        helper.assertTrue(lapisHolder.cycleLapisColor(false), "lapis pipe color did not advance");
        net.minecraft.world.item.DyeColor advanced = lapisHolder.pipeColor();
        helper.assertTrue(advanced != net.minecraft.world.item.DyeColor.WHITE, "lapis pipe color remained white");
        helper.assertTrue(lapisHolder.cycleLapisColor(true), "lapis pipe color did not reverse");
        helper.assertTrue(lapisHolder.pipeColor() == net.minecraft.world.item.DyeColor.WHITE,
            "lapis pipe reverse color cycle did not restore white");
        var lapisInput = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            lapisPos, net.minecraft.core.Direction.WEST
        );
        helper.assertTrue(lapisInput != null, "lapis pipe input capability missing");
        insertPipeItem(lapisInput, Items.LAPIS_LAZULI, 1);
        tickPipes(helper, 10, lapisPos);
        helper.assertTrue(lapisHolder.travellingItems().getFirst().color().orElse(null)
            == net.minecraft.world.item.DyeColor.WHITE, "lapis pipe did not color item at center");

        assertPipeLoot(helper, obsidianPos, buildcraft.transport.BCTransportItems.PIPE_OBSIDIAN_ITEM.get());
        assertPipeLoot(helper, lapisPos, buildcraft.transport.BCTransportItems.PIPE_LAPIS_ITEM.get());
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_OBSIDIAN_ITEM.get(), Items.OBSIDIAN, Items.GLASS);
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_LAPIS_ITEM.get(), Items.LAPIS_BLOCK, Items.GLASS);
        helper.succeed();
    }

    private static void transportDaizuliItemPipe(GameTestHelper helper) {
        buildcraft.transport.block.PipeHolderBlock block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState lapis = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.LAPIS_ITEM
        );
        BlockState daizuli = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.DAIZULI_ITEM
        );
        BlockPos lapisPos = helper.absolutePos(new BlockPos(0, 1, 1));
        BlockPos daizuliPos = lapisPos.east();
        BlockPos normalOutputPos = daizuliPos.east();
        BlockPos coloredOutputPos = daizuliPos.south();
        helper.getLevel().setBlock(lapisPos, lapis, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(daizuliPos, daizuli, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(normalOutputPos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(coloredOutputPos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        buildcraft.transport.block.entity.PipeHolderBlockEntity lapisHolder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(lapisPos);
        buildcraft.transport.block.entity.PipeHolderBlockEntity daizuliHolder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(daizuliPos);
        helper.assertTrue(lapisHolder != null && daizuliHolder != null, "colored pipe holders missing");
        helper.assertTrue(daizuliHolder.rotatePipeDirection(), "daizuli pipe direction did not rotate");
        helper.assertTrue(daizuliHolder.routingDirection() == net.minecraft.core.Direction.SOUTH,
            "daizuli pipe did not select first valid colored output");
        var lapisInput = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            lapisPos, net.minecraft.core.Direction.WEST
        );
        helper.assertTrue(lapisInput != null, "lapis input capability missing");
        insertPipeItem(lapisInput, Items.WHITE_WOOL, 2);
        tickPipes(helper, 45, lapisPos, daizuliPos);
        net.minecraft.world.Container normalOutput = (net.minecraft.world.Container)
            helper.getLevel().getBlockEntity(normalOutputPos);
        net.minecraft.world.Container coloredOutput = (net.minecraft.world.Container)
            helper.getLevel().getBlockEntity(coloredOutputPos);
        helper.assertValueEqual(containerCount(coloredOutput, Items.WHITE_WOOL), 2,
            "matching Daizuli color did not use selected output");
        helper.assertValueEqual(containerCount(normalOutput, Items.WHITE_WOOL), 0,
            "matching Daizuli color escaped through normal output");

        helper.assertTrue(lapisHolder.cycleLapisColor(false), "lapis source color did not change");
        insertPipeItem(lapisInput, Items.ORANGE_WOOL, 3);
        tickPipes(helper, 45, lapisPos, daizuliPos);
        helper.assertValueEqual(containerCount(normalOutput, Items.ORANGE_WOOL), 3,
            "nonmatching Daizuli color did not avoid selected output");
        helper.assertValueEqual(containerCount(coloredOutput, Items.ORANGE_WOOL), 0,
            "nonmatching Daizuli color used selected output");
        helper.assertTrue(daizuliHolder.cycleDaizuliColor(true), "Daizuli color did not cycle");
        helper.assertTrue(daizuliHolder.pipeColor() != net.minecraft.world.item.DyeColor.WHITE,
            "Daizuli color remained white after cycle");
        net.minecraft.nbt.CompoundTag daizuliSaved =
            daizuliHolder.saveWithFullMetadata(helper.getLevel().registryAccess());
        buildcraft.transport.block.entity.PipeHolderBlockEntity daizuliLoaded =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                    daizuliPos, helper.getLevel().getBlockState(daizuliPos), daizuliSaved,
                    helper.getLevel().registryAccess()
                );
        helper.assertTrue(daizuliLoaded != null, "Daizuli holder failed codec reload");
        helper.assertTrue(daizuliLoaded.routingDirection() == net.minecraft.core.Direction.SOUTH,
            "Daizuli selected output did not survive codec reload");
        helper.assertTrue(daizuliLoaded.pipeColor() == daizuliHolder.pipeColor(),
            "Daizuli selected color did not survive codec reload");
        assertPipeLoot(helper, daizuliPos, buildcraft.transport.BCTransportItems.PIPE_DAIZULI_ITEM.get());
        net.minecraft.world.item.crafting.CraftingInput recipeInput =
            net.minecraft.world.item.crafting.CraftingInput.of(3, 1, java.util.List.of(
                new ItemStack(Items.LAPIS_BLOCK), new ItemStack(Items.GLASS), new ItemStack(Items.DIAMOND)
            ));
        ItemStack recipeOutput = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
            net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel()
        ).orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(recipeOutput.is(buildcraft.transport.BCTransportItems.PIPE_DAIZULI_ITEM.get()),
            "Daizuli recipe returned wrong item");
        helper.assertValueEqual(recipeOutput.getCount(), 8, "Daizuli recipe returned wrong count");
        helper.succeed();
    }

    private static void transportDiamondWoodItemPipe(GameTestHelper helper) {
        buildcraft.transport.block.PipeHolderBlock block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState diamondWood = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.DIAMOND_WOOD_ITEM
        );
        BlockState cobble = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_ITEM
        );
        BlockPos sourcePos = helper.absolutePos(new BlockPos(0, 1, 1));
        BlockPos diamondPos = sourcePos.east();
        BlockPos cobblePos = diamondPos.east();
        BlockPos targetPos = cobblePos.east();
        helper.getLevel().setBlock(sourcePos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(diamondPos, diamondWood, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(cobblePos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(targetPos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        net.minecraft.world.Container source = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(sourcePos);
        net.minecraft.world.Container target = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(targetPos);
        source.setItem(0, new ItemStack(Items.GOLD_INGOT, 4));
        buildcraft.transport.block.entity.PipeHolderBlockEntity holder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(diamondPos);
        helper.assertTrue(holder != null, "diamond wooden holder missing");
        tickPipes(helper, 1, diamondPos);
        holder.setDiamondFilter(0, new ItemStack(Items.GOLD_INGOT));
        buildcraft.api.mj.IMjRedstoneReceiver receiver = holder.mjReceiver();
        helper.assertTrue(receiver != null, "diamond wooden MJ receiver missing");
        helper.assertValueEqual(receiver.receivePower(4 * MjAPI.MJ, false), 3 * MjAPI.MJ,
            "diamond wooden whitelist did not extract exactly one item");
        tickPipes(helper, 65, diamondPos, cobblePos);
        helper.assertValueEqual(containerCount(target, Items.GOLD_INGOT), 1,
            "diamond wooden whitelist extracted wrong item");
        helper.assertValueEqual(containerCount(target, Items.IRON_INGOT), 0,
            "diamond wooden whitelist leaked unmatched item");

        source.setItem(1, new ItemStack(Items.IRON_INGOT, 4));
        holder.setDiamondFilterMode(
            buildcraft.transport.block.entity.PipeHolderBlockEntity.DiamondFilterMode.BLACK_LIST
        );
        helper.assertValueEqual(receiver.receivePower(2 * MjAPI.MJ, false), MjAPI.MJ,
            "diamond wooden blacklist did not extract exactly one item");
        tickPipes(helper, 65, diamondPos, cobblePos);
        helper.assertValueEqual(containerCount(target, Items.IRON_INGOT), 1,
            "diamond wooden blacklist did not extract nonmatching item");

        holder.setDiamondFilter(1, new ItemStack(Items.IRON_INGOT));
        holder.setDiamondFilterMode(
            buildcraft.transport.block.entity.PipeHolderBlockEntity.DiamondFilterMode.ROUND_ROBIN
        );
        helper.assertValueEqual(holder.diamondFilterCursor(), 0, "diamond wooden round-robin cursor started wrong");
        receiver.receivePower(MjAPI.MJ, false);
        helper.assertValueEqual(holder.diamondFilterCursor(), 1, "diamond wooden round-robin did not advance");
        receiver.receivePower(MjAPI.MJ, false);
        helper.assertValueEqual(holder.diamondFilterCursor(), 0, "diamond wooden round-robin did not wrap");
        tickPipes(helper, 65, diamondPos, cobblePos);
        helper.assertValueEqual(containerCount(target, Items.GOLD_INGOT), 2,
            "diamond wooden round-robin missed gold preset");
        helper.assertValueEqual(containerCount(target, Items.IRON_INGOT), 2,
            "diamond wooden round-robin missed iron preset");
        helper.assertTrue(!buildcraft.transport.PipeType.WOOD_ITEM.connectsTo(
            buildcraft.transport.PipeType.DIAMOND_WOOD_ITEM), "wood extraction pipes connected together");

        net.minecraft.world.entity.player.Player menuPlayer =
            helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        buildcraft.transport.menu.DiamondWoodMenu menu = new buildcraft.transport.menu.DiamondWoodMenu(
            0, menuPlayer.getInventory(), diamondPos
        );
        helper.assertTrue(menu.clickMenuButton(menuPlayer, 0), "diamond wooden menu rejected mode button");
        helper.assertTrue(holder.diamondFilterMode()
            == buildcraft.transport.block.entity.PipeHolderBlockEntity.DiamondFilterMode.WHITE_LIST,
            "diamond wooden menu did not cycle filter mode");
        net.minecraft.nbt.CompoundTag saved = holder.saveWithFullMetadata(helper.getLevel().registryAccess());
        buildcraft.transport.block.entity.PipeHolderBlockEntity loaded =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                    diamondPos, helper.getLevel().getBlockState(diamondPos), saved, helper.getLevel().registryAccess()
                );
        helper.assertTrue(loaded != null && loaded.diamondFilters().get(0).is(Items.GOLD_INGOT)
            && loaded.diamondFilters().get(1).is(Items.IRON_INGOT), "diamond wooden filters failed codec reload");
        assertPipeLoot(helper, diamondPos, buildcraft.transport.BCTransportItems.PIPE_DIAMOND_WOOD_ITEM.get());
        net.minecraft.world.item.crafting.CraftingInput recipeInput =
            net.minecraft.world.item.crafting.CraftingInput.of(3, 1, java.util.List.of(
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.GLASS), new ItemStack(Items.DIAMOND)
            ));
        ItemStack recipeOutput = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
            net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel()
        ).orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(recipeOutput.is(buildcraft.transport.BCTransportItems.PIPE_DIAMOND_WOOD_ITEM.get()),
            "diamond wooden recipe returned wrong item");
        helper.assertValueEqual(recipeOutput.getCount(), 8, "diamond wooden recipe returned wrong count");
        helper.succeed();
    }

    private static void transportEmzuliItemPipe(GameTestHelper helper) {
        buildcraft.transport.block.PipeHolderBlock block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState emzuli = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.EMZULI_ITEM
        );
        BlockState cobble = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_ITEM
        );
        BlockPos sourcePos = helper.absolutePos(new BlockPos(0, 1, 1));
        BlockPos emzuliPos = sourcePos.east();
        BlockPos cobblePos = emzuliPos.east();
        BlockPos targetPos = cobblePos.east();
        helper.getLevel().setBlock(sourcePos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(emzuliPos, emzuli, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(cobblePos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(targetPos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        net.minecraft.world.Container source = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(sourcePos);
        net.minecraft.world.Container target = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(targetPos);
        source.setItem(0, new ItemStack(Items.GOLD_INGOT, 4));
        source.setItem(1, new ItemStack(Items.IRON_INGOT, 4));
        buildcraft.transport.block.entity.PipeHolderBlockEntity holder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(emzuliPos);
        helper.assertTrue(holder != null, "Emzuli holder missing");
        tickPipes(helper, 1, emzuliPos);
        holder.setEmzuliFilter(0, new ItemStack(Items.GOLD_INGOT));
        holder.setEmzuliFilter(1, new ItemStack(Items.IRON_INGOT));
        holder.cycleEmzuliColor(0, false);
        helper.assertTrue(holder.emzuliColor(0).orElse(null) == net.minecraft.world.item.DyeColor.WHITE,
            "Emzuli preset did not select white paint");
        helper.assertTrue(holder.activateEmzuliPreset(0) && holder.activateEmzuliPreset(1),
            "Emzuli gate activation contract rejected preset");
        helper.assertValueEqual(holder.emzuliCurrent(), 0, "Emzuli did not select first active preset");
        buildcraft.api.mj.IMjRedstoneReceiver receiver = holder.mjReceiver();
        helper.assertTrue(receiver != null, "Emzuli MJ receiver missing");
        helper.assertValueEqual(receiver.receivePower(2 * MjAPI.MJ, false), 0L,
            "Emzuli first preset consumed wrong MJ");
        helper.assertValueEqual(holder.travellingItems().getFirst().stack().getCount(), 2,
            "Emzuli first preset extracted wrong count");
        helper.assertTrue(holder.travellingItems().getFirst().color().orElse(null)
            == net.minecraft.world.item.DyeColor.WHITE, "Emzuli preset did not paint extracted stack");
        helper.assertValueEqual(holder.emzuliCurrent(), 1, "Emzuli did not advance to next active preset");
        holder.activateEmzuliPreset(0);
        holder.activateEmzuliPreset(1);
        helper.assertValueEqual(receiver.receivePower(3 * MjAPI.MJ, false), 0L,
            "Emzuli second preset consumed wrong MJ");
        helper.assertValueEqual(holder.travellingItems().get(1).stack().getCount(), 3,
            "Emzuli second preset extracted wrong count");
        helper.assertTrue(holder.travellingItems().get(1).color().isEmpty(),
            "Emzuli unpainted preset unexpectedly painted stack");
        helper.assertValueEqual(holder.emzuliCurrent(), 0, "Emzuli preset round-robin did not wrap");
        tickPipes(helper, 65, emzuliPos, cobblePos);
        helper.assertValueEqual(containerCount(target, Items.GOLD_INGOT), 2, "Emzuli missed gold extraction");
        helper.assertValueEqual(containerCount(target, Items.IRON_INGOT), 3, "Emzuli missed iron extraction");
        helper.assertTrue(!holder.emzuliActive(0) && !holder.emzuliActive(1),
            "Emzuli preset activation did not expire after TTL");
        helper.assertTrue(!buildcraft.transport.PipeType.EMZULI_ITEM.connectsTo(
            buildcraft.transport.PipeType.DIAMOND_WOOD_ITEM), "Emzuli connected to wooden extraction pipe");

        net.minecraft.world.entity.player.Player menuPlayer =
            helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        buildcraft.transport.menu.EmzuliMenu menu = new buildcraft.transport.menu.EmzuliMenu(
            0, menuPlayer.getInventory(), emzuliPos
        );
        helper.assertTrue(menu.clickMenuButton(menuPlayer, 1), "Emzuli menu rejected paint button");
        helper.assertTrue(holder.emzuliColor(1).orElse(null) == net.minecraft.world.item.DyeColor.WHITE,
            "Emzuli menu did not cycle preset paint");
        holder.activateEmzuliPreset(1);
        net.minecraft.nbt.CompoundTag saved = holder.saveWithFullMetadata(helper.getLevel().registryAccess());
        buildcraft.transport.block.entity.PipeHolderBlockEntity loaded =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                    emzuliPos, helper.getLevel().getBlockState(emzuliPos), saved, helper.getLevel().registryAccess()
                );
        helper.assertTrue(loaded != null && loaded.emzuliFilters().get(0).is(Items.GOLD_INGOT)
            && loaded.emzuliFilters().get(1).is(Items.IRON_INGOT), "Emzuli filters failed codec reload");
        helper.assertTrue(loaded.emzuliColor(0).orElse(null) == net.minecraft.world.item.DyeColor.WHITE
            && loaded.emzuliActive(1), "Emzuli paint or activation failed codec reload");
        assertPipeLoot(helper, emzuliPos, buildcraft.transport.BCTransportItems.PIPE_EMZULI_ITEM.get());
        net.minecraft.world.item.crafting.CraftingInput recipeInput =
            net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_DIAMOND_WOOD_ITEM.get()),
                new ItemStack(Items.LAPIS_BLOCK)
            ));
        ItemStack recipeOutput = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
            net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel()
        ).orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(recipeOutput.is(buildcraft.transport.BCTransportItems.PIPE_EMZULI_ITEM.get()),
            "Emzuli upgrade recipe returned wrong item");
        helper.assertValueEqual(recipeOutput.getCount(), 1, "Emzuli upgrade recipe returned wrong count");
        helper.succeed();
    }

    private static void transportDiamondItemPipe(GameTestHelper helper) {
        buildcraft.transport.block.PipeHolderBlock block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState diamond = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.DIAMOND_ITEM
        );
        BlockPos pipePos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos eastPos = pipePos.east();
        BlockPos southPos = pipePos.south();
        BlockPos northPos = pipePos.north();
        helper.getLevel().setBlock(pipePos, diamond, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(eastPos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(southPos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(northPos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        buildcraft.transport.block.entity.PipeHolderBlockEntity holder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(pipePos);
        helper.assertTrue(holder != null, "Diamond pipe holder missing");
        holder.setDiamondRouteFilter(net.minecraft.core.Direction.EAST.ordinal() * 9,
            new ItemStack(Items.GOLD_INGOT, 2));
        holder.setDiamondRouteFilter(net.minecraft.core.Direction.SOUTH.ordinal() * 9,
            new ItemStack(Items.GOLD_INGOT, 1));
        holder.setDiamondRouteFilter(net.minecraft.core.Direction.NORTH.ordinal() * 9,
            new ItemStack(Items.IRON_INGOT, 1));
        var input = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            pipePos, net.minecraft.core.Direction.WEST
        );
        helper.assertTrue(input != null, "Diamond pipe input capability missing");
        insertPipeItem(input, Items.GOLD_INGOT, 6);
        tickPipes(helper, 25, pipePos);
        net.minecraft.world.Container east = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(eastPos);
        net.minecraft.world.Container south = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(southPos);
        net.minecraft.world.Container north = (net.minecraft.world.Container) helper.getLevel().getBlockEntity(northPos);
        helper.assertValueEqual(containerCount(east, Items.GOLD_INGOT), 4,
            "Diamond pipe did not honor east weight two");
        helper.assertValueEqual(containerCount(south, Items.GOLD_INGOT), 2,
            "Diamond pipe did not honor south weight one");
        helper.assertValueEqual(containerCount(north, Items.GOLD_INGOT), 0,
            "Diamond pipe leaked gold into nonmatching configured side");
        insertPipeItem(input, Items.IRON_INGOT, 2);
        tickPipes(helper, 25, pipePos);
        helper.assertValueEqual(containerCount(north, Items.IRON_INGOT), 2,
            "Diamond pipe did not prioritize matching north side");

        insertPipeItem(input, Items.EMERALD, 1);
        tickPipes(helper, 12, pipePos);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(
            net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pipePos).inflate(1.5),
            entity -> entity.getItem().is(Items.EMERALD)
        ).isEmpty(), "Diamond pipe did not drop item when every output filter rejected it");
        holder.setDiamondRouteFilter(net.minecraft.core.Direction.EAST.ordinal() * 9, ItemStack.EMPTY);
        insertPipeItem(input, Items.DIAMOND, 3);
        tickPipes(helper, 25, pipePos);
        helper.assertValueEqual(containerCount(east, Items.DIAMOND), 3,
            "Diamond pipe did not use empty-filter fallback output");

        net.minecraft.nbt.CompoundTag saved = holder.saveWithFullMetadata(helper.getLevel().registryAccess());
        buildcraft.transport.block.entity.PipeHolderBlockEntity loaded =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                    pipePos, helper.getLevel().getBlockState(pipePos), saved, helper.getLevel().registryAccess()
                );
        helper.assertTrue(loaded != null && loaded.diamondRouteFilters()
            .get(net.minecraft.core.Direction.SOUTH.ordinal() * 9).getCount() == 1,
            "Diamond route filters failed codec reload");
        net.minecraft.world.entity.player.Player menuPlayer =
            helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        menuPlayer.setPos(pipePos.getX() + 0.5, pipePos.getY() + 0.5, pipePos.getZ() + 0.5);
        buildcraft.transport.menu.DiamondRouteMenu menu = new buildcraft.transport.menu.DiamondRouteMenu(
            0, menuPlayer.getInventory(), pipePos
        );
        helper.assertTrue(menu.stillValid(menuPlayer) && menu.slots.size() == 90,
            "Diamond route menu did not expose 54 filters plus player inventory");
        assertPipeLoot(helper, pipePos, buildcraft.transport.BCTransportItems.PIPE_DIAMOND_ITEM.get());
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_DIAMOND_ITEM.get(),
            Items.DIAMOND, Items.GLASS);
        helper.succeed();
    }

    private static void transportStripesItemPipe(GameTestHelper helper) {
        buildcraft.transport.block.PipeHolderBlock block = buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get();
        BlockState stripes = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.STRIPES_ITEM
        );
        BlockState cobble = block.defaultBlockState().setValue(
            buildcraft.transport.block.PipeHolderBlock.TYPE, buildcraft.transport.PipeType.COBBLESTONE_ITEM
        );
        BlockPos targetChestPos = helper.absolutePos(new BlockPos(0, 1, 1));
        BlockPos cobblePos = targetChestPos.east();
        BlockPos stripesPos = cobblePos.east();
        BlockPos workPos = stripesPos.east();
        helper.getLevel().setBlock(targetChestPos, Blocks.CHEST.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(cobblePos, cobble, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(stripesPos, stripes, net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.getLevel().setBlock(workPos, Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        buildcraft.transport.block.entity.PipeHolderBlockEntity holder =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity) helper.getLevel().getBlockEntity(stripesPos);
        helper.assertTrue(holder != null, "Stripes pipe holder missing");
        tickPipes(helper, 1, stripesPos);
        helper.assertTrue(holder.stripesDirection() == net.minecraft.core.Direction.EAST,
            "Stripes pipe did not select opposite of sole connection");
        buildcraft.api.mj.IMjRedstoneReceiver receiver = holder.mjReceiver();
        helper.assertTrue(receiver != null, "Stripes MJ receiver missing");
        helper.assertValueEqual(receiver.getPowerRequested(), 256 * MjAPI.MJ,
            "Stripes battery requested wrong capacity");
        helper.assertValueEqual(receiver.receivePower(100 * MjAPI.MJ, true), 0L,
            "Stripes battery simulation rejected valid power");
        helper.assertValueEqual(holder.stripesPower(), 0L, "Stripes battery simulation mutated state");
        receiver.receivePower(100 * MjAPI.MJ, false);
        tickPipes(helper, 3, stripesPos);
        helper.assertValueEqual(holder.stripesProgress(), 30 * MjAPI.MJ,
            "Stripes breaker did not consume 10 MJ per tick");
        net.minecraft.nbt.CompoundTag progressSaved = holder.saveWithFullMetadata(helper.getLevel().registryAccess());
        buildcraft.transport.block.entity.PipeHolderBlockEntity progressLoaded =
            (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                    stripesPos, helper.getLevel().getBlockState(stripesPos), progressSaved,
                    helper.getLevel().registryAccess()
                );
        helper.assertTrue(progressLoaded != null && progressLoaded.stripesProgress() == 30 * MjAPI.MJ
            && progressLoaded.stripesPower() == 70 * MjAPI.MJ,
            "Stripes battery or progress failed codec reload");
        tickPipes(helper, 100, stripesPos, cobblePos);
        helper.assertTrue(helper.getLevel().getBlockState(workPos).isAir(),
            "Stripes pipe did not break powered stone");
        net.minecraft.world.Container targetChest = (net.minecraft.world.Container)
            helper.getLevel().getBlockEntity(targetChestPos);
        helper.assertValueEqual(containerCount(targetChest, Items.COBBLESTONE), 1,
            "Stripes pipe did not reinsert block drop toward connected pipe");

        var stripesInput = helper.getLevel().getCapability(
            net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            stripesPos, net.minecraft.core.Direction.WEST
        );
        helper.assertTrue(stripesInput != null, "Stripes placement input capability missing");
        insertPipeItem(stripesInput, Items.DIRT, 1);
        tickPipes(helper, 15, stripesPos);
        helper.assertTrue(helper.getLevel().getBlockState(workPos).is(Blocks.DIRT),
            "Stripes pipe did not use incoming block item on open side");
        helper.getLevel().removeBlock(workPos, false);
        insertPipeItem(stripesInput, Items.ARROW, 1);
        tickPipes(helper, 12, stripesPos);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(
            net.minecraft.world.entity.projectile.arrow.AbstractArrow.class,
            new net.minecraft.world.phys.AABB(workPos).inflate(20)
        ).isEmpty(), "Stripes pipe did not invoke dispenser behavior for arrow");
        helper.assertTrue(!buildcraft.transport.PipeType.STRIPES_ITEM.connectsTo(
            buildcraft.transport.PipeType.STRIPES_ITEM), "Stripes pipes connected directly");
        assertPipeLoot(helper, stripesPos, buildcraft.transport.BCTransportItems.PIPE_STRIPES_ITEM.get());
        assertPipeRecipe(helper, buildcraft.transport.BCTransportItems.PIPE_STRIPES_ITEM.get(),
            BCCoreItems.GEAR_GOLD.get(), Items.GLASS);
        helper.succeed();
    }

    private static void mjFoundation(GameTestHelper helper) {
        MjBattery battery = new MjBattery(10);
        MjRedstoneBatteryReceiver receiver = new MjRedstoneBatteryReceiver(battery);
        MjCapabilityHelper capabilities = new MjCapabilityHelper(receiver);
        helper.assertTrue(capabilities.connector() == receiver, "MJ connector role");
        helper.assertTrue(capabilities.receiver() == receiver, "MJ receiver role");
        helper.assertTrue(capabilities.redstoneReceiver() == receiver, "MJ redstone receiver role");
        helper.assertTrue(capabilities.readable() == receiver, "MJ readable role");
        helper.assertTrue(capabilities.passiveProvider() == null, "MJ passive-provider role leak");

        helper.assertValueEqual(receiver.receivePower(6, true), 0L, "simulated MJ excess");
        helper.assertValueEqual(battery.getStored(), 0L, "simulated MJ changed storage");
        helper.assertValueEqual(receiver.receivePower(6, false), 0L, "committed MJ excess");
        helper.assertValueEqual(battery.getStored(), 6L, "committed MJ storage");
        helper.assertValueEqual(receiver.getPowerRequested(), 4L, "requested MJ");

        receiver.receivePower(6, false);
        helper.assertValueEqual(battery.getStored(), 12L, "historical MJ overfill");
        helper.assertValueEqual(receiver.receivePower(3, false), 3L, "full battery rejected MJ");
        helper.assertValueEqual(battery.extractPower(5, 7, true), 7L, "simulated MJ extraction");
        helper.assertValueEqual(battery.getStored(), 12L, "simulated extraction changed MJ");
        helper.assertValueEqual(battery.extractPower(5, 7), 7L, "committed MJ extraction");
        helper.assertValueEqual(battery.extractPower(6, 7), 0L, "minimum MJ extraction");

        MjBattery decoded = new MjBattery(10);
        decoded.deserializeNBT(battery.serializeNBT());
        helper.assertValueEqual(decoded.getStored(), 5L, "MJ NBT round trip");
        decoded.addPower(20, false);
        decoded.tick(helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
        helper.assertValueEqual(decoded.getStored(), 24L, "MJ overload decay");
        helper.assertValueEqual(MjAPI.formatMj(1_500_000), "1.5", "MJ display formatting");
        helper.assertTrue(MjAPI.CAP_CONNECTOR != null && MjAPI.CAP_RECEIVER != null
            && MjAPI.CAP_REDSTONE_RECEIVER != null && MjAPI.CAP_READABLE != null
            && MjAPI.CAP_PASSIVE_PROVIDER != null, "MJ capabilities were not created");
        helper.succeed();
    }

    private static void mjEnergyConversion(GameTestHelper helper) {
        MjRfConversion conversion = MjRfConversion.createDefault();
        helper.assertValueEqual(conversion.mjPerRf, 100_000L, "default MJ conversion");
        helper.assertTrue(conversion.usingDefaultValue, "default MJ conversion marker");
        helper.assertTrue(MjRfConversion.createRaw(99).usingDefaultValue, "invalid MJ conversion accepted");

        MjBattery battery = new MjBattery(MjAPI.MJ);
        MjRedstoneBatteryReceiver receiver = new MjRedstoneBatteryReceiver(battery);
        MjEnergyAdapter energy = new MjEnergyAdapter(receiver, receiver, conversion);
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(energy.insert(4, transaction), 4, "aborted energy insertion");
            helper.assertValueEqual(energy.getAmountAsLong(), 4L, "pending energy amount");
            helper.assertValueEqual(battery.getStored(), 0L, "energy inserted before commit");
        }
        helper.assertValueEqual(energy.getAmountAsLong(), 0L, "aborted pending energy");

        try (net.neoforged.neoforge.transfer.transaction.Transaction root =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(energy.insert(2, root), 2, "root energy insertion");
            try (net.neoforged.neoforge.transfer.transaction.Transaction nested =
                net.neoforged.neoforge.transfer.transaction.Transaction.open(root)) {
                helper.assertValueEqual(energy.insert(3, nested), 3, "nested energy insertion");
                nested.commit();
            }
            helper.assertValueEqual(battery.getStored(), 0L, "nested energy committed before root");
        }
        helper.assertValueEqual(energy.getAmountAsLong(), 0L, "aborted nested energy");

        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(energy.insert(4, transaction), 4, "committed energy insertion");
            transaction.commit();
        }
        helper.assertValueEqual(battery.getStored(), 400_000L, "converted MJ amount");
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
            net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            helper.assertValueEqual(energy.insert(10, transaction), 6, "capacity-limited energy insertion");
            helper.assertValueEqual(energy.extract(10, transaction), 0, "unsupported energy extraction");
            transaction.commit();
        }
        helper.assertValueEqual(battery.getStored(), MjAPI.MJ, "full converted MJ amount");

        IMjToRfStatus previous = MjAPI.getRfStatus();
        try {
            MjAPI.setRfStatus(new IMjToRfStatus() {
                @Override
                public MjRfConversion getConversion() {
                    return conversion;
                }

                @Override
                public boolean isAutoconvertEnabled() {
                    return true;
                }
            });
            helper.assertTrue(new MjCapabilityHelper(receiver).energy() != null,
                "enabled MJ helper did not expose NeoForge energy");
        } finally {
            MjAPI.setRfStatus(previous);
        }
        helper.succeed();
    }

    private static void redstoneEngine(GameTestHelper helper) {
        BlockState state = BCCoreBlocks.ENGINE.get().defaultBlockState()
            .setValue(BlockEngine.ENGINE_TYPE, EnumEngineType.WOOD)
            .setValue(BlockEngine.FACING, net.minecraft.core.Direction.UP);
        BlockPos enginePos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(enginePos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        RedstoneEngineBlockEntity engine = (RedstoneEngineBlockEntity) helper.getLevel().getBlockEntity(enginePos);
        helper.assertTrue(engine != null, "redstone engine block entity missing");
        helper.assertTrue(helper.getLevel().getCapability(
            MjAPI.CAP_CONNECTOR, enginePos, net.minecraft.core.Direction.UP
        ) != null, "registered engine connector capability missing");
        helper.assertTrue(helper.getLevel().getCapability(
            MjAPI.CAP_CONNECTOR, enginePos, net.minecraft.core.Direction.DOWN
        ) == null, "registered engine connector leaked to another side");
        helper.assertTrue(engine.connector(net.minecraft.core.Direction.UP) != null,
            "engine output connector missing");
        helper.assertTrue(engine.connector(net.minecraft.core.Direction.DOWN) == null,
            "engine exposed connector on input side");
        IMjReceiver normalReceiver = new IMjReceiver() {
            @Override public boolean canConnect(IMjConnector other) { return true; }
            @Override public long getPowerRequested() { return MjAPI.MJ; }
            @Override public long receivePower(long amount, boolean simulate) { return 0; }
        };
        helper.assertTrue(!engine.connector(net.minecraft.core.Direction.UP).canConnect(normalReceiver),
            "redstone engine connected to a normal MJ receiver");

        TestMjReceiver receiver = new TestMjReceiver();
        for (int tick = 0; tick < 60; tick++) engine.tickCycle(true, receiver, tick);
        helper.assertValueEqual(receiver.received, MjAPI.MJ, "redstone engine pulse output");
        helper.assertTrue(engine.pumping(), "powered redstone engine was not pumping");
        helper.assertValueEqual(engine.currentOutput(), MjAPI.MJ / 20, "redstone engine nominal output");
        engine.tickCycle(false, receiver, 61);
        helper.assertValueEqual(engine.storedPower(), 0L, "unpowered redstone engine retained power");
        helper.succeed();
    }

    private static void creativeEngine(GameTestHelper helper) {
        BlockState state = BCCoreBlocks.ENGINE.get().defaultBlockState()
            .setValue(BlockEngine.ENGINE_TYPE, EnumEngineType.CREATIVE)
            .setValue(BlockEngine.FACING, net.minecraft.core.Direction.UP);
        BlockPos enginePos = helper.absolutePos(new BlockPos(0, 1, 0));
        helper.getLevel().setBlock(enginePos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        CreativeEngineBlockEntity engine =
            (CreativeEngineBlockEntity) helper.getLevel().getBlockEntity(enginePos);
        helper.assertTrue(engine != null, "creative engine block entity missing");
        IMjConnector connector = helper.getLevel().getCapability(
            MjAPI.CAP_CONNECTOR, enginePos, net.minecraft.core.Direction.UP
        );
        helper.assertTrue(connector != null, "creative engine connector capability missing");
        helper.assertTrue(helper.getLevel().getCapability(
            MjAPI.CAP_CONNECTOR, enginePos, net.minecraft.core.Direction.DOWN
        ) == null, "creative engine connector leaked to another side");

        long[] received = { 0 };
        IMjReceiver receiver = new IMjReceiver() {
            @Override public boolean canConnect(IMjConnector other) { return true; }
            @Override public long getPowerRequested() { return 1_000 * MjAPI.MJ; }
            @Override public long receivePower(long amount, boolean simulate) {
                if (!simulate) received[0] += amount;
                return 0;
            }
        };
        helper.assertTrue(connector.canConnect(receiver),
            "creative engine rejected a normal MJ receiver");
        for (int tick = 0; tick < 3; tick++) engine.tickCycle(true, receiver);
        helper.assertValueEqual(received[0], 3 * MjAPI.MJ, "creative engine base output");
        helper.assertTrue(engine.pumping(), "powered creative engine was not pumping");

        for (int index = 1; index < CreativeEngineBlockEntity.OUTPUTS.length; index++) {
            helper.assertValueEqual(engine.cycleOutput(), index, "creative engine output index");
            helper.assertValueEqual(engine.currentOutput(),
                CreativeEngineBlockEntity.OUTPUTS[index] * MjAPI.MJ,
                "creative engine selected output");
        }
        helper.assertValueEqual(engine.cycleOutput(), 0, "creative engine output wrap");
        engine.tickCycle(false, receiver);
        helper.assertValueEqual(engine.storedPower(), 0L, "unpowered creative engine retained power");
        helper.succeed();
    }

    private static final class TestMjReceiver implements IMjRedstoneReceiver {
        private long received;

        @Override
        public boolean canConnect(IMjConnector other) {
            return true;
        }

        @Override
        public long getPowerRequested() {
            return 10 * MjAPI.MJ;
        }

        @Override
        public long receivePower(long microJoules, boolean simulate) {
            if (!simulate) received += microJoules;
            return 0;
        }
    }

    private static UseOnContext useContext(GameTestHelper helper, net.minecraft.world.entity.player.Player player,
        ItemStack stack, BlockPos relativePos) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos), net.minecraft.core.Direction.UP, absolutePos, false);
        return new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
    }

    private static void roboticsGateStatements(GameTestHelper helper) {
        BlockPos relative = new BlockPos(2, 2, 2);
        BlockPos pos = helper.absolutePos(relative);
        helper.setBlock(relative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        var holder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(holder != null, "Robotics Gate test pipe has no block entity");
        helper.assertTrue(holder.installAttachment(Direction.NORTH,
                new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get())),
                "Robotics Gate test rejected Robot Station");

        ItemStack gate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        gate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.ROBOT_RESERVED,
                                buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT))));
        helper.assertTrue(holder.installAttachment(Direction.UP, gate),
                "Robotics Gate test rejected Gate");
        buildcraft.robotics.item.RobotStationItem stationItem =
                buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get();
        stationItem.tickAttachment(holder, Direction.NORTH, holder.attachment(Direction.NORTH));
        var station = buildcraft.robotics.RobotStationRegistry.get(helper.getLevel(),
                new buildcraft.robotics.RobotStationRegistry.Address(pos, Direction.NORTH)).orElseThrow();
        java.util.UUID robotId = java.util.UUID.randomUUID();
        helper.assertTrue(station.reserve(robotId), "Robotics Gate test could not reserve station");
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), pos, helper.getLevel().getBlockState(pos), holder);
        helper.assertTrue(holder.gateRedstoneOutput(), "Robot Reserved trigger stayed inactive");

        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setUUID(robotId);
        robot.setBoard(buildcraft.robotics.RobotBoardType.CARRIER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(robot.dock(station), "Robotics Gate test Robot did not dock");
        helper.getLevel().addFreshEntity(robot);
        for (buildcraft.silicon.gate.GateTrigger trigger : java.util.List.of(
                buildcraft.silicon.gate.GateTrigger.ROBOT_LINKED,
                buildcraft.silicon.gate.GateTrigger.ROBOT_RESERVED,
                buildcraft.silicon.gate.GateTrigger.ROBOT_IN_STATION,
                buildcraft.silicon.gate.GateTrigger.ROBOT_SLEEPING)) {
            ItemStack updated = holder.attachment(Direction.UP).copy();
            updated.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                    new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                            new buildcraft.silicon.gate.GateRule(trigger,
                                    buildcraft.silicon.gate.GateAction.REDSTONE_OUTPUT))));
            holder.setAttachment(Direction.UP, updated);
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), pos, Direction.NORTH);
            buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                    helper.getLevel(), pos, helper.getLevel().getBlockState(pos), holder);
            helper.assertTrue(holder.gateRedstoneOutput(), trigger.getSerializedName() + " stayed inactive");
        }

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu = new buildcraft.silicon.menu.GateMenu(57, player.getInventory(), pos, Direction.UP);
        helper.assertTrue(menu.clickMenuButton(player, 16), "Gate editor rejected action cycle");
        helper.assertValueEqual(buildcraft.silicon.gate.GateAction.PULSAR_CONSTANT,
                holder.attachment(Direction.UP)
                        .get(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get())
                        .rules().getFirst().action(), "Gate editor did not persist action cycle");
        menu.setCarried(new ItemStack(Items.COBBLESTONE, 32));
        helper.assertTrue(menu.clickMenuButton(player, 24), "Gate editor rejected first parameter");
        ItemStack parameter = holder.attachment(Direction.UP)
                .get(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get())
                .rules().getFirst().parameters().getFirst();
        helper.assertTrue(parameter.is(Items.COBBLESTONE) && parameter.getCount() == 32,
                "Gate editor did not preserve the exact item parameter count");

        ItemStack actionGate = holder.attachment(Direction.UP).copy();
        actionGate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.STATION_PROVIDE_ITEMS,
                                java.util.Optional.empty(), java.util.List.of(parameter)))));
        holder.setAttachment(Direction.UP, actionGate);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), pos, Direction.NORTH);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), pos, helper.getLevel().getBlockState(pos), holder);
        var disabled = buildcraft.robotics.RobotStationConfig.DEFAULT;
        var address = new buildcraft.robotics.RobotStationRegistry.Address(pos, Direction.NORTH);
        helper.assertTrue(buildcraft.robotics.RoboticsGateActions.providesItem(
                helper.getLevel(), address, disabled, new ItemStack(Items.COBBLESTONE)),
                "Provide Items action did not enable its filtered item");
        helper.assertFalse(buildcraft.robotics.RoboticsGateActions.providesItem(
                helper.getLevel(), address, disabled, new ItemStack(Items.DIRT)),
                "Provide Items action ignored its item parameter");
        helper.assertFalse(buildcraft.robotics.RoboticsGateActions.providesFluid(
                helper.getLevel(), address, disabled,
                net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                        net.minecraft.world.level.material.Fluids.WATER)),
                "Provide Items action incorrectly enabled fluid output");
        ItemStack unrestrictedGate = holder.attachment(Direction.UP).copy();
        unrestrictedGate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.STATION_PROVIDE_ITEMS,
                                java.util.Optional.empty(), java.util.List.of(new ItemStack(Items.DIRT))),
                        new buildcraft.silicon.gate.GateRule(buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.STATION_PROVIDE_ITEMS))));
        holder.setAttachment(Direction.UP, unrestrictedGate);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), pos, helper.getLevel().getBlockState(pos), holder);
        helper.assertTrue(buildcraft.robotics.RoboticsGateActions.providesItem(
                helper.getLevel(), address, disabled, new ItemStack(Items.DIAMOND)),
                "Unfiltered Provide Items rule did not override a parallel filtered rule");
        robot.setTaskState(buildcraft.robotics.RobotTaskState.LEAVING);
        ItemStack returnGate = holder.attachment(Direction.UP).copy();
        returnGate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.ROBOT_GOTO_STATION))));
        holder.setAttachment(Direction.UP, returnGate);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(
                helper.getLevel(), pos, helper.getLevel().getBlockState(pos), holder);
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.RETURNING, robot.taskState(),
                "Go To Station action did not recall linked Robot");

        BlockPos fluidRelative = new BlockPos(5, 2, 2);
        BlockPos fluidPos = helper.absolutePos(fluidRelative);
        helper.setBlock(fluidRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        var fluidHolder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(fluidPos);
        helper.assertTrue(fluidHolder != null && fluidHolder.installAttachment(Direction.NORTH,
                new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get())),
                "Fluid Gate test rejected Robot Station");
        ItemStack fluidGate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        fluidGate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(
                                buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.STATION_PROVIDE_FLUIDS,
                                java.util.Optional.empty(),
                                java.util.List.of(new ItemStack(Items.WATER_BUCKET))))));
        helper.assertTrue(fluidHolder.installAttachment(Direction.UP, fluidGate),
                "Fluid Gate test rejected Gate");
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), fluidPos, Direction.NORTH);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(helper.getLevel(), fluidPos,
                helper.getLevel().getBlockState(fluidPos), fluidHolder);
        var fluidAddress = new buildcraft.robotics.RobotStationRegistry.Address(fluidPos, Direction.NORTH);
        helper.assertTrue(buildcraft.robotics.RoboticsGateActions.providesFluid(
                helper.getLevel(), fluidAddress, disabled,
                net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                        net.minecraft.world.level.material.Fluids.WATER)),
                "Provide Fluids action did not enable its filtered fluid");
        helper.assertFalse(buildcraft.robotics.RoboticsGateActions.providesFluid(
                helper.getLevel(), fluidAddress, disabled,
                net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                        net.minecraft.world.level.material.Fluids.LAVA)),
                "Provide Fluids action ignored its bucket parameter");
        helper.assertFalse(buildcraft.robotics.RoboticsGateActions.providesItem(
                helper.getLevel(), fluidAddress, disabled, new ItemStack(Items.WATER_BUCKET)),
                "Provide Fluids action incorrectly enabled item output");

        BlockPos robotActionRelative = new BlockPos(7, 2, 2);
        BlockPos robotActionPos = helper.absolutePos(robotActionRelative);
        helper.setBlock(robotActionRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        var robotActionHolder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(robotActionPos);
        helper.assertTrue(robotActionHolder != null && robotActionHolder.installAttachment(Direction.NORTH,
                new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get())),
                "Robot action test rejected Robot Station");
        var actionZone = new buildcraft.robotics.zone.ZonePlan();
        actionZone.set(robotActionPos.getX() + 3, robotActionPos.getZ() + 4, true);
        ItemStack map = new ItemStack(buildcraft.core.BCCoreItems.MAP_LOCATION.get());
        buildcraft.robotics.zone.ZoneMapLocation.set(map, actionZone, "Gate zone");
        ItemStack forcedRobot = buildcraft.robotics.item.RobotItem.create(
                buildcraft.robotics.RobotBoardType.CARRIER, 0);
        ItemStack robotActionGate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.GOLD,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        robotActionGate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.ROBOT_WORK_AREA,
                                java.util.Optional.empty(), java.util.List.of(map)),
                        new buildcraft.silicon.gate.GateRule(buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.ROBOT_FILTER,
                                java.util.Optional.empty(), java.util.List.of(new ItemStack(Items.COBBLESTONE))),
                        new buildcraft.silicon.gate.GateRule(buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.ROBOT_FILTER_TOOL,
                                java.util.Optional.empty(), java.util.List.of(new ItemStack(Items.IRON_PICKAXE))),
                        new buildcraft.silicon.gate.GateRule(buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.STATION_FORCE_ROBOT,
                                java.util.Optional.empty(), java.util.List.of(forcedRobot)))));
        helper.assertTrue(robotActionHolder.installAttachment(Direction.UP, robotActionGate),
                "Robot action test rejected Gate");
        buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), robotActionPos, Direction.NORTH);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(helper.getLevel(), robotActionPos,
                helper.getLevel().getBlockState(robotActionPos), robotActionHolder);
        var robotActionAddress = new buildcraft.robotics.RobotStationRegistry.Address(
                robotActionPos, Direction.NORTH);
        helper.assertValueEqual(actionZone, buildcraft.robotics.RoboticsGateActions.workZone(
                helper.getLevel(), robotActionAddress), "Work Area action lost its Zone Map");
        helper.assertTrue(buildcraft.robotics.RoboticsGateActions.matchesWorkItem(
                helper.getLevel(), robotActionAddress, new ItemStack(Items.COBBLESTONE)),
                "Robot Filter rejected configured item");
        helper.assertFalse(buildcraft.robotics.RoboticsGateActions.matchesWorkItem(
                helper.getLevel(), robotActionAddress, new ItemStack(Items.DIRT)),
                "Robot Filter accepted unconfigured item");
        helper.assertTrue(buildcraft.robotics.RoboticsGateActions.matchesTool(
                helper.getLevel(), robotActionAddress, new ItemStack(Items.IRON_PICKAXE)),
                "Tool Filter rejected configured tool");
        helper.assertFalse(buildcraft.robotics.RoboticsGateActions.matchesTool(
                helper.getLevel(), robotActionAddress, new ItemStack(Items.DIAMOND_PICKAXE)),
                "Tool Filter accepted unconfigured tool");
        helper.assertTrue(buildcraft.robotics.RoboticsGateActions.permitsRobot(helper.getLevel(),
                robotActionAddress, buildcraft.robotics.RobotBoardType.CARRIER),
                "Force Robot rejected configured board");
        helper.assertFalse(buildcraft.robotics.RoboticsGateActions.permitsRobot(helper.getLevel(),
                robotActionAddress, buildcraft.robotics.RobotBoardType.PICKER),
                "Force Robot accepted unconfigured board");

        BlockPos requestRelative = new BlockPos(7, 2, 5);
        BlockPos requestPos = helper.absolutePos(requestRelative);
        helper.getLevel().setBlock(requestPos,
                buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get().defaultBlockState().setValue(
                        buildcraft.transport.block.PipeHolderBlock.TYPE,
                        buildcraft.transport.PipeType.COBBLESTONE_ITEM), 3);
        var requestHolder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(requestPos);
        helper.assertTrue(requestHolder != null && requestHolder.installAttachment(Direction.NORTH,
                new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get())),
                "Request Items test rejected Robot Station");
        ItemStack requestGate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        requestGate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.STATION_REQUEST_ITEMS,
                                java.util.Optional.empty(),
                                java.util.List.of(new ItemStack(Items.COBBLESTONE, 12))))));
        helper.assertTrue(requestHolder.installAttachment(Direction.UP, requestGate),
                "Request Items test rejected Gate");
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), requestPos, Direction.NORTH);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(helper.getLevel(), requestPos,
                helper.getLevel().getBlockState(requestPos), requestHolder);
        java.util.UUID requestRobot = java.util.UUID.randomUUID();
        var requestReservation = buildcraft.robotics.RequesterRegistry.reserveClosest(helper.getLevel(),
                Vec3.atCenterOf(requestPos), requestRobot, 2).orElseThrow();
        helper.assertTrue(requestReservation.station().isPresent()
                        && requestReservation.request().getCount() == 12,
                "Request Items action did not expose its exact-count Station request");
        helper.assertTrue(buildcraft.robotics.RequesterRegistry.reserveClosest(helper.getLevel(),
                Vec3.atCenterOf(requestPos), java.util.UUID.randomUUID(), 2).isEmpty(),
                "Request Items action accepted two reservations for one slot");
        helper.assertTrue(buildcraft.robotics.RequesterRegistry.offer(helper.getLevel(), requestReservation,
                new ItemStack(Items.COBBLESTONE, 12)).isEmpty(),
                "Request Items delivery was not injected into the Station pipe");
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(helper.getLevel(), requestPos,
                helper.getLevel().getBlockState(requestPos), requestHolder);
        helper.assertTrue(requestHolder.hasTravellingItems(),
                "Request Items delivery did not create a travelling pipe item");
        buildcraft.robotics.RequesterRegistry.release(helper.getLevel(), requestReservation);

        BlockPos machineRelative = new BlockPos(4, 2, 5);
        BlockPos machinePos = helper.absolutePos(machineRelative);
        BlockPos providerRelative = machineRelative.relative(Direction.NORTH);
        helper.setBlock(machineRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(providerRelative, buildcraft.robotics.BCRoboticsBlocks.REQUESTER.get());
        var machineHolder = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(machinePos);
        var provider = (buildcraft.robotics.block.entity.RequesterBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(providerRelative));
        provider.setRequest(0, new ItemStack(Items.DIRT, 9));
        helper.assertTrue(machineHolder.installAttachment(Direction.NORTH,
                new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get())),
                "Machine Request test rejected Robot Station");
        ItemStack machineGate = buildcraft.silicon.BCSiliconItems.gate(
                buildcraft.silicon.gate.GateMaterial.IRON,
                buildcraft.silicon.gate.GateLogic.AND,
                buildcraft.silicon.gate.GateModifier.NO_MODIFIER);
        machineGate.set(buildcraft.silicon.BCSiliconDataComponents.GATE_PROGRAM.get(),
                new buildcraft.silicon.gate.GateProgram(java.util.List.of(
                        new buildcraft.silicon.gate.GateRule(buildcraft.silicon.gate.GateTrigger.TRUE,
                                buildcraft.silicon.gate.GateAction.STATION_MACHINE_REQUEST_ITEMS))));
        helper.assertTrue(machineHolder.installAttachment(Direction.UP, machineGate),
                "Machine Request test rejected Gate");
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), machinePos, Direction.NORTH);
        buildcraft.transport.block.entity.PipeHolderBlockEntity.tick(helper.getLevel(), machinePos,
                helper.getLevel().getBlockState(machinePos), machineHolder);
        java.util.UUID machineRobot = java.util.UUID.randomUUID();
        var machineReservation = buildcraft.robotics.RequesterRegistry.reserveClosest(helper.getLevel(),
                Vec3.atCenterOf(machinePos), machineRobot, 2, machinePos::equals).orElseThrow();
        helper.assertTrue(machineReservation.request().is(Items.DIRT)
                        && machineReservation.request().getCount() == 9,
                "Machine Request did not forward the adjacent provider request");
        helper.assertTrue(buildcraft.robotics.RequesterRegistry.offer(helper.getLevel(), machineReservation,
                new ItemStack(Items.DIRT, 9)).isEmpty() && provider.fulfilled(0),
                "Machine Request did not deliver into the adjacent provider");
        buildcraft.robotics.RequesterRegistry.release(helper.getLevel(), machineReservation);
        helper.succeed();
    }

    private static void roboticsRobotGoggles(GameTestHelper helper) {
        ItemStack goggles = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_GOGGLES.get());
        helper.assertValueEqual(1, goggles.getMaxStackSize(), "Robot Goggles stack size");
        helper.assertFalse(goggles.isDamageableItem(), "Robot Goggles unexpectedly have durability");
        var equippable = goggles.get(DataComponents.EQUIPPABLE);
        helper.assertTrue(equippable != null, "Robot Goggles are not equippable");
        helper.assertValueEqual(net.minecraft.world.entity.EquipmentSlot.HEAD, equippable.slot(),
                "Robot Goggles equipment slot");
        var modifiers = goggles.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
        helper.assertTrue(modifiers.modifiers().isEmpty(), "Robot Goggles unexpectedly provide armour");
        helper.succeed();
    }

    private static void roboticsRobotStation(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 2, 1);
        helper.setBlock(relative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        buildcraft.transport.block.entity.PipeHolderBlockEntity holder =
                (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                        helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        helper.assertTrue(holder != null, "robot station test pipe did not create its block entity");

        ItemStack stationStack = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        helper.assertTrue(holder.installAttachment(Direction.UP, stationStack),
                "robot station was rejected by the pipe attachment slot");
        buildcraft.robotics.item.RobotStationItem stationItem =
                buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get();
        stationItem.tickAttachment(holder, Direction.UP, holder.attachment(Direction.UP));

        buildcraft.robotics.RobotStationRegistry.Address address =
                new buildcraft.robotics.RobotStationRegistry.Address(holder.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.Station station =
                buildcraft.robotics.RobotStationRegistry.get(helper.getLevel(), address).orElseThrow();
        helper.assertValueEqual(buildcraft.robotics.RobotStationState.AVAILABLE, station.state(),
                "new robot station was not available");
        helper.assertTrue(buildcraft.robotics.RobotStationRegistry.closestAvailable(
                        helper.getLevel(), station.dockingPosition(), 1.0).orElseThrow() == station,
                "available robot station was not discoverable");

        java.util.UUID robot = java.util.UUID.randomUUID();
        helper.assertTrue(station.reserve(robot), "available robot station could not be reserved");
        helper.assertFalse(station.reserve(java.util.UUID.randomUUID()),
                "reserved robot station accepted a second robot");
        helper.assertTrue(station.link(robot), "reserved robot station could not link its robot");
        stationItem.tickAttachment(holder, Direction.UP, holder.attachment(Direction.UP));
        buildcraft.robotics.RobotStationData data = holder.attachment(Direction.UP).getOrDefault(
                buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION.get(),
                buildcraft.robotics.RobotStationData.AVAILABLE);
        helper.assertValueEqual(buildcraft.robotics.RobotStationState.LINKED, data.state(),
                "linked station state was not synchronized to its attachment stack");
        helper.assertValueEqual(robot, data.robot().orElseThrow(),
                "linked robot identity was not synchronized");
        station.release(robot);
        helper.assertValueEqual(buildcraft.robotics.RobotStationState.AVAILABLE, station.state(),
                "released station did not become available");
        ItemStack robotStack = buildcraft.robotics.item.RobotItem.create(
                buildcraft.robotics.RobotBoardType.CARRIER, 5_000_000L);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertTrue(buildcraft.robotics.BCRoboticsItems.ROBOT.get()
                        .useOn(useContext(helper, player, robotStack, relative)).consumesAction(),
                "integrated robot item could not be placed on an available station");
        helper.assertTrue(robotStack.isEmpty(), "placed robot item was not consumed");
        var robots = helper.getLevel().getEntitiesOfClass(
                buildcraft.robotics.entity.RobotEntity.class,
                new net.minecraft.world.phys.AABB(holder.getBlockPos()).inflate(3));
        helper.assertValueEqual(1, robots.size(), "Robot Station did not spawn exactly one robot");
        buildcraft.robotics.entity.RobotEntity placedRobot = robots.getFirst();
        helper.assertValueEqual(buildcraft.robotics.RobotBoardType.CARRIER, placedRobot.board(),
                "spawned robot lost its integrated board");
        helper.assertValueEqual(5_000_000L, placedRobot.energy(),
                "spawned robot lost its stored energy");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, placedRobot.taskState(),
                "spawned robot did not dock");
        buildcraft.api.mj.IMjReceiver chargingReceiver =
                buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get()
                        .mjReceiver(holder, Direction.UP, holder.attachment(Direction.UP));
        helper.assertValueEqual(buildcraft.robotics.RobotItemData.MAX_ENERGY - 5_000_000L,
                chargingReceiver.getPowerRequested(), "station exposed the wrong robot power request");
        helper.assertValueEqual(0L, chargingReceiver.receivePower(1_000_000L, false),
                "station rejected power for its linked robot");
        helper.assertValueEqual(6_000_000L, placedRobot.energy(),
                "station did not charge its linked robot");
        placedRobot.setItem(0, new ItemStack(Items.COBBLESTONE, 12));
        helper.assertValueEqual(12, placedRobot.getItem(0).getCount(),
                "robot four-slot inventory rejected its contents");
        placedRobot.leaveStation();
        for (int i = 0; i < 15; i++) {
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), holder.getBlockPos(), Direction.UP);
            placedRobot.tick();
        }
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.IDLE, placedRobot.taskState(),
                "robot did not complete its station departure");
        placedRobot.returnToStation();
        for (int i = 0; i < 20 && placedRobot.taskState() != buildcraft.robotics.RobotTaskState.DOCKED; i++) {
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), holder.getBlockPos(), Direction.UP);
            placedRobot.tick();
        }
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, placedRobot.taskState(),
                "robot did not return and re-dock");
        player.setShiftKeyDown(true);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(buildcraft.core.BCCoreItems.WRENCH.get()));
        helper.assertTrue(placedRobot.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                        placedRobot.position()).consumesAction(),
                "wrench did not dismantle the robot");
        helper.assertTrue(placedRobot.isRemoved(), "dismantled robot entity remained in the world");
        helper.assertTrue(player.getInventory().hasAnyMatching(
                        stack -> stack.is(buildcraft.robotics.BCRoboticsItems.ROBOT.get())),
                "dismantled robot did not return its configured item");
        helper.assertValueEqual(buildcraft.robotics.RobotStationState.AVAILABLE, station.state(),
                "dismantling did not release the Robot Station");
        ItemStack goldChipset = buildcraft.silicon.BCSiliconItems.chipset(
                buildcraft.silicon.ChipsetType.GOLD);
        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY,
                new ItemStack(Items.IRON_INGOT), goldChipset, new ItemStack(Items.IRON_INGOT),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        ItemStack crafted = helper.getLevel().getServer().getRecipeManager().getRecipeFor(
                        net.minecraft.world.item.crafting.RecipeType.CRAFTING, recipeInput, helper.getLevel())
                .orElseThrow().value().assemble(recipeInput);
        helper.assertTrue(crafted.is(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get()),
                "Robot Station recipe output");
        helper.succeed();
    }

    private static void roboticsDeliveryRobot(GameTestHelper helper) {
        BlockPos pipeRelative = new BlockPos(1, 2, 1);
        BlockPos chestRelative = pipeRelative.above();
        BlockPos requesterRelative = new BlockPos(6, 2, 1);
        helper.setBlock(pipeRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(chestRelative, Blocks.CHEST);
        helper.setBlock(requesterRelative, buildcraft.robotics.BCRoboticsBlocks.REQUESTER.get());
        buildcraft.transport.block.entity.PipeHolderBlockEntity holder =
                (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                        helper.getLevel().getBlockEntity(helper.absolutePos(pipeRelative));
        ItemStack deliverySourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        deliverySourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of()));
        helper.assertTrue(holder.installAttachment(Direction.UP, deliverySourceStation),
                "delivery test rejected Robot Station attachment");
        net.minecraft.world.Container chest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(chestRelative));
        chest.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        buildcraft.robotics.block.entity.RequesterBlockEntity requester =
                (buildcraft.robotics.block.entity.RequesterBlockEntity)
                        helper.getLevel().getBlockEntity(helper.absolutePos(requesterRelative));
        requester.setRequest(0, new ItemStack(Items.COBBLESTONE, 16));

        java.util.UUID auditOwner = java.util.UUID.randomUUID();
        buildcraft.robotics.RequesterRegistry.Reservation auditReservation =
                buildcraft.robotics.RequesterRegistry.reserveClosest(helper.getLevel(),
                        Vec3.atCenterOf(holder.getBlockPos()), auditOwner, 128,
                        helper.absolutePos(requesterRelative)::equals).orElseThrow();
        helper.assertTrue(buildcraft.robotics.RequesterRegistry.reserveClosest(helper.getLevel(),
                        Vec3.atCenterOf(holder.getBlockPos()), java.util.UUID.randomUUID(), 128,
                        helper.absolutePos(requesterRelative)::equals).isEmpty(),
                "Requester accepted two Delivery reservations for one slot");
        buildcraft.robotics.RequesterRegistry.release(helper.getLevel(), auditReservation);

        buildcraft.robotics.RobotStationRegistry.Station station =
                buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), holder.getBlockPos(), Direction.UP);
        buildcraft.robotics.entity.RobotEntity robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.DELIVERY);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(station.reserve(robot.getUUID()) && robot.dock(station),
                "Delivery robot could not claim its home station");
        helper.getLevel().addFreshEntity(robot);

        for (int tick = 0; tick < 300 && (!requester.fulfilled(0)
                || robot.deliveryPhase() != buildcraft.robotics.DeliveryPhase.NONE); tick++) {
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), holder.getBlockPos(), Direction.UP);
            robot.tick();
        }
        helper.assertTrue(requester.fulfilled(0), "Delivery robot did not fulfill the Requester slot");
        helper.assertValueEqual(16, requester.stored(0).getCount(),
                "Delivery robot offered the wrong item quantity");
        helper.assertValueEqual(16, chest.getItem(0).getCount(),
                "Delivery robot extracted the wrong source quantity");
        helper.assertTrue(robot.isEmpty(), "Delivery robot retained cargo after a successful offer");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Delivery robot did not return home");
        helper.assertValueEqual(buildcraft.robotics.DeliveryPhase.NONE, robot.deliveryPhase(),
                "Delivery scheduler did not finish its task");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Delivery flight consumed no battery energy");
        helper.succeed();
    }

    private static void roboticsCarrierRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 2, 1);
        BlockPos providerRelative = new BlockPos(4, 2, 1);
        BlockPos receiverRelative = new BlockPos(2, 2, 1);
        helper.setBlock(homeRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(providerRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(receiverRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(providerRelative.above(), Blocks.CHEST);
        helper.setBlock(receiverRelative.above(), Blocks.CHEST);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var provider = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(providerRelative));
        var receiver = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative));
        helper.assertTrue(home.installAttachment(Direction.UP,
                new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get())),
                "Carrier home station installation failed");
        ItemStack providerStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        providerStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of(new ItemStack(Items.COBBLESTONE))));
        helper.assertTrue(provider.installAttachment(Direction.UP, providerStation),
                "Carrier provider station installation failed");
        ItemStack receiverStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        receiverStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.RECEIVE,
                        java.util.List.of(new ItemStack(Items.COBBLESTONE))));
        helper.assertTrue(receiver.installAttachment(Direction.UP, receiverStation),
                "Carrier receiver station installation failed");

        net.minecraft.world.Container providerChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(providerRelative.above()));
        net.minecraft.world.Container receiverChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative.above()));
        providerChest.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        providerChest.setItem(1, new ItemStack(Items.DIRT, 7));

        var homeStation = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), provider.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), receiver.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.CARRIER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeStation.reserve(robot.getUUID()) && robot.dock(homeStation),
                "Carrier could not dock at home");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 500 && (receiverChest.getItem(0).getCount() != 32
                || robot.carrierPhase() != buildcraft.robotics.CarrierPhase.NONE); tick++) {
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), home.getBlockPos(), Direction.UP);
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), provider.getBlockPos(), Direction.UP);
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), receiver.getBlockPos(), Direction.UP);
            robot.tick();
        }
        helper.assertTrue(providerChest.getItem(0).isEmpty(),
                "Carrier did not extract provider cargo");
        helper.assertValueEqual(7, providerChest.getItem(1).getCount(),
                "Carrier ignored provider item filter");
        helper.assertTrue(receiverChest.getItem(0).is(Items.COBBLESTONE)
                        && receiverChest.getItem(0).getCount() == 32,
                "Carrier did not unload into receiver station");
        helper.assertTrue(robot.isEmpty(), "Carrier retained successfully unloaded cargo");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Carrier did not return home");
        helper.assertValueEqual(buildcraft.robotics.CarrierPhase.NONE, robot.carrierPhase(),
                "Carrier scheduler did not finish");

        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        helper.assertTrue(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get()
                        .useAttachment(home, Direction.UP, home.attachment(Direction.UP), player).consumesAction(),
                "empty-hand station configuration did not consume the interaction");
        var cycled = home.attachment(Direction.UP).getOrDefault(
                buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                buildcraft.robotics.RobotStationConfig.DEFAULT);
        helper.assertValueEqual(buildcraft.robotics.RobotStationMode.BOTH, cycled.mode(),
                "station mode did not cycle from disabled to both");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(Items.DIAMOND));
        buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get()
                .useAttachment(home, Direction.UP, home.attachment(Direction.UP), player);
        var filtered = home.attachment(Direction.UP).getOrDefault(
                buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                buildcraft.robotics.RobotStationConfig.DEFAULT);
        helper.assertTrue(filtered.matches(new ItemStack(Items.DIAMOND)) && filtered.filters().size() == 1,
                "held-item station filter was not added");
        helper.succeed();
    }

    private static void roboticsPickerRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 2, 1);
        BlockPos receiverRelative = new BlockPos(8, 2, 1);
        helper.setBlock(homeRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(receiverRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(receiverRelative.above(), Blocks.CHEST);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var receiver = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative));
        var pickerWorkZone = new buildcraft.robotics.zone.ZonePlan();
        pickerWorkZone.set(helper.absolutePos(new BlockPos(1, 3, 1)).getX(),
                helper.absolutePos(new BlockPos(1, 3, 1)).getZ(), true);
        var pickerLoadZone = new buildcraft.robotics.zone.ZonePlan();
        pickerLoadZone.set(receiver.getBlockPos().getX(), receiver.getBlockPos().getZ(), true);
        ItemStack homeStationStack = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStationStack.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.BOTH,
                        java.util.List.of(new ItemStack(Items.COBBLESTONE)), java.util.List.of(),
                        pickerWorkZone, pickerLoadZone));
        helper.assertTrue(home.installAttachment(Direction.UP, homeStationStack),
                "Picker home station installation failed");
        ItemStack receiverStationStack = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        receiverStationStack.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.RECEIVE,
                        java.util.List.of(new ItemStack(Items.COBBLESTONE))));
        helper.assertTrue(receiver.installAttachment(Direction.UP, receiverStationStack),
                "Picker receiver station installation failed");
        net.minecraft.world.Container receiverChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative.above()));

        Vec3 dropPosition = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 3, 1)));
        var cobblestoneDrop = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),
                dropPosition.x, dropPosition.y, dropPosition.z, new ItemStack(Items.COBBLESTONE, 20));
        var dirtDrop = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),
                dropPosition.x, dropPosition.y, dropPosition.z + 1, new ItemStack(Items.DIRT, 9));
        Vec3 excludedPosition = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 3, 2)));
        var outsideZoneDrop = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),
                excludedPosition.x, excludedPosition.y, excludedPosition.z,
                new ItemStack(Items.COBBLESTONE, 7));
        helper.getLevel().addFreshEntity(cobblestoneDrop);
        helper.getLevel().addFreshEntity(dirtDrop);
        helper.getLevel().addFreshEntity(outsideZoneDrop);
        java.util.UUID auditRobot = java.util.UUID.randomUUID();
        var auditTarget = buildcraft.robotics.DroppedItemRegistry.reserveClosest(helper.getLevel(),
                dropPosition, 250, auditRobot, item -> item.getItem().is(Items.COBBLESTONE)).orElseThrow();
        helper.assertFalse(buildcraft.robotics.DroppedItemRegistry.reclaim(helper.getLevel(),
                        auditTarget.getUUID(), java.util.UUID.randomUUID()),
                "two Picker robots reserved the same dropped item");
        buildcraft.robotics.DroppedItemRegistry.release(
                helper.getLevel(), auditTarget.getUUID(), auditRobot);

        var homeStation = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), receiver.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.PICKER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeStation.reserve(robot.getUUID()) && robot.dock(homeStation),
                "Picker could not dock at home");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 500 && (receiverChest.getItem(0).getCount() != 20
                || robot.pickerPhase() != buildcraft.robotics.PickerPhase.NONE); tick++) {
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), home.getBlockPos(), Direction.UP);
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), receiver.getBlockPos(), Direction.UP);
            robot.tick();
        }
        helper.assertTrue(cobblestoneDrop.isRemoved(), "Picker did not consume the selected drop");
        helper.assertTrue(dirtDrop.isAlive() && dirtDrop.getItem().getCount() == 9,
                "Picker ignored its home station item filter");
        helper.assertTrue(outsideZoneDrop.isAlive() && outsideZoneDrop.getItem().getCount() == 7,
                "Picker ignored its configured work zone");
        helper.assertTrue(receiverChest.getItem(0).is(Items.COBBLESTONE)
                        && receiverChest.getItem(0).getCount() == 20,
            "Picker did not unload collected items");
        helper.assertTrue(robot.isEmpty(), "Picker retained unloaded items");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Picker did not return home");
        helper.assertValueEqual(buildcraft.robotics.PickerPhase.NONE, robot.pickerPhase(),
                "Picker scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Picker flight consumed no battery energy");
        helper.succeed();
    }

    private static void roboticsFluidCarrierRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 2, 1);
        BlockPos waterProviderRelative = new BlockPos(3, 2, 1);
        BlockPos lavaProviderRelative = new BlockPos(1, 2, 3);
        BlockPos receiverRelative = new BlockPos(3, 2, 3);
        for (BlockPos relative : java.util.List.of(homeRelative, waterProviderRelative,
                lavaProviderRelative, receiverRelative)) {
            helper.setBlock(relative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        }
        helper.setBlock(waterProviderRelative.above(), buildcraft.factory.BCFactoryBlocks.TANK.get());
        helper.setBlock(lavaProviderRelative.above(), buildcraft.factory.BCFactoryBlocks.TANK.get());
        helper.setBlock(receiverRelative.above(), buildcraft.factory.BCFactoryBlocks.TANK.get());
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var waterProvider = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(waterProviderRelative));
        var lavaProvider = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(lavaProviderRelative));
        var receiver = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative));
        home.installAttachment(Direction.UP,
                new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get()));
        net.neoforged.neoforge.transfer.fluid.FluidResource water =
                net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                        net.minecraft.world.level.material.Fluids.WATER);
        net.neoforged.neoforge.transfer.fluid.FluidResource lava =
                net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                        net.minecraft.world.level.material.Fluids.LAVA);
        ItemStack waterProviderStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        waterProviderStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of(), java.util.List.of(water)));
        waterProvider.installAttachment(Direction.UP, waterProviderStation);
        ItemStack lavaProviderStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        lavaProviderStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of(), java.util.List.of(water)));
        lavaProvider.installAttachment(Direction.UP, lavaProviderStation);
        ItemStack receiverStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        receiverStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.RECEIVE,
                        java.util.List.of(), java.util.List.of(water)));
        receiver.installAttachment(Direction.UP, receiverStation);

        var waterTank = ((buildcraft.factory.block.entity.TankBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(waterProviderRelative.above()))).localStorage();
        var lavaTank = ((buildcraft.factory.block.entity.TankBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(lavaProviderRelative.above()))).localStorage();
        var receiverTank = ((buildcraft.factory.block.entity.TankBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(receiverRelative.above()))).localStorage();
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction =
                     net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            waterTank.insert(water, 6_000, transaction);
            lavaTank.insert(lava, 1_000, transaction);
            transaction.commit();
        }
        var homeStation = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        for (var pipe : java.util.List.of(waterProvider, lavaProvider, receiver)) {
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), pipe.getBlockPos(), Direction.UP);
        }
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.FLUID_CARRIER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeStation.reserve(robot.getUUID()) && robot.dock(homeStation),
                "Fluid Carrier could not dock at home");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 600 && (receiverTank.getAmountAsInt(0) != 4_000
                || robot.fluidCarrierPhase() != buildcraft.robotics.FluidCarrierPhase.NONE); tick++) {
            for (var pipe : java.util.List.of(home, waterProvider, lavaProvider, receiver)) {
                buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), pipe.getBlockPos(), Direction.UP);
            }
            robot.tick();
        }
        helper.assertValueEqual(2_000, waterTank.getAmountAsInt(0),
                "Fluid Carrier ignored its 4,000 mB tank capacity");
        helper.assertValueEqual(1_000, lavaTank.getAmountAsInt(0),
                "Fluid Carrier ignored station fluid filter");
        helper.assertTrue(receiverTank.getResource(0).equals(water)
                        && receiverTank.getAmountAsInt(0) == 4_000,
                "Fluid Carrier unloaded the wrong fluid or amount");
        helper.assertValueEqual(0, robot.fluidTank().getAmountAsInt(0),
                "Fluid Carrier retained unloaded fluid");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Fluid Carrier did not return home");
        helper.assertValueEqual(buildcraft.robotics.FluidCarrierPhase.NONE, robot.fluidCarrierPhase(),
                "Fluid Carrier scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Fluid Carrier flight consumed no battery energy");
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(Items.WATER_BUCKET));
        buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get()
                .useAttachment(home, Direction.UP, home.attachment(Direction.UP), player);
        var editedConfig = home.attachment(Direction.UP).getOrDefault(
                buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                buildcraft.robotics.RobotStationConfig.DEFAULT);
        helper.assertTrue(editedConfig.fluidFilters().contains(water),
                "filled-container interaction did not add station fluid filter");
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        workZone.set(home.getBlockPos().getX() + 2, home.getBlockPos().getZ() + 3, true);
        ItemStack zoneMap = new ItemStack(buildcraft.core.BCCoreItems.MAP_LOCATION.get());
        buildcraft.robotics.zone.ZoneMapLocation.set(zoneMap, workZone, "Robot work area");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, zoneMap);
        buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get()
                .useAttachment(home, Direction.UP, home.attachment(Direction.UP), player);
        player.setShiftKeyDown(true);
        buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get()
                .useAttachment(home, Direction.UP, home.attachment(Direction.UP), player);
        player.setShiftKeyDown(false);
        editedConfig = home.attachment(Direction.UP).getOrDefault(
                buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                buildcraft.robotics.RobotStationConfig.DEFAULT);
        helper.assertTrue(workZone.equals(editedConfig.workZone())
                        && workZone.equals(editedConfig.loadUnloadZone()),
                "Zone Map interaction did not assign work and load/unload areas");
        helper.succeed();
    }

    private static void roboticsLumberjackRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 12, 1);
        BlockPos sourceRelative = new BlockPos(2, 12, 1);
        BlockPos receiverRelative = new BlockPos(3, 12, 1);
        BlockPos targetRelative = new BlockPos(4, 12, 1);
        BlockPos excludedRelative = new BlockPos(4, 12, 2);
        for (BlockPos relative : java.util.List.of(homeRelative, sourceRelative, receiverRelative)) {
            helper.setBlock(relative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        }
        helper.setBlock(sourceRelative.above(), Blocks.CHEST);
        helper.setBlock(receiverRelative.above(), Blocks.CHEST);
        helper.setBlock(targetRelative, Blocks.OAK_LOG);
        helper.setBlock(excludedRelative, Blocks.OAK_LOG);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative));
        var receiver = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative));
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        BlockPos target = helper.absolutePos(targetRelative);
        workZone.set(target.getX(), target.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(source.getBlockPos().getX(), source.getBlockPos().getZ(), true);
        loadZone.set(receiver.getBlockPos().getX(), receiver.getBlockPos().getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, loadZone));
        helper.assertTrue(home.installAttachment(Direction.UP, homeStation),
                "Lumberjack home station installation failed");
        ItemStack sourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        sourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of()));
        source.installAttachment(Direction.UP, sourceStation);
        ItemStack receiverStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        receiverStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.RECEIVE,
                        java.util.List.of()));
        receiver.installAttachment(Direction.UP, receiverStation);
        net.minecraft.world.Container sourceChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative.above()));
        net.minecraft.world.Container receiverChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative.above()));
        ItemStack nearlyBrokenAxe = new ItemStack(Items.WOODEN_AXE);
        nearlyBrokenAxe.setDamageValue(nearlyBrokenAxe.getMaxDamage() - 2);
        sourceChest.setItem(0, nearlyBrokenAxe);
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), receiver.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.LUMBERJACK);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Lumberjack failed to dock at its home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 1_500 && (!helper.getBlockState(targetRelative).isAir()
                || receiverChest.getItem(0).isEmpty()
                || robot.lumberjackPhase() != buildcraft.robotics.LumberjackPhase.NONE); tick++) {
            for (var pipe : java.util.List.of(home, source, receiver)) {
                buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), pipe.getBlockPos(), Direction.UP);
            }
            robot.tick();
        }
        helper.assertTrue(helper.getBlockState(targetRelative).isAir(),
                "Lumberjack did not harvest the zoned log: phase=" + robot.lumberjackPhase()
                        + ", task=" + robot.taskState() + ", tool=" + robot.lumberjackTool()
                        + ", source=" + sourceChest.getItem(0)
                        + ", receiver=" + receiverChest.getItem(0));
        helper.assertTrue(helper.getBlockState(excludedRelative).is(Blocks.OAK_LOG),
                "Lumberjack harvested a log outside its work zone");
        helper.assertTrue(sourceChest.getItem(0).isEmpty(),
                "Lumberjack did not extract exactly one axe");
        helper.assertTrue(receiverChest.getItem(0).is(Items.WOODEN_AXE)
                        && receiverChest.getItem(0).getDamageValue()
                        == receiverChest.getItem(0).getMaxDamage() - 1,
                "Lumberjack did not apply durability and unload its worn axe");
        helper.assertTrue(robot.lumberjackTool().isEmpty(),
                "Lumberjack retained its unloaded axe");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Lumberjack did not return home");
        helper.assertValueEqual(buildcraft.robotics.LumberjackPhase.NONE, robot.lumberjackPhase(),
                "Lumberjack scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY - 1_000_000,
                "Lumberjack work consumed insufficient battery energy");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(target).inflate(2),
                item -> item.getItem().is(Items.OAK_LOG)).isEmpty(),
                "Lumberjack did not preserve harvested block drops");
        helper.succeed();
    }

    private static void roboticsHarvesterRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 24, 1);
        BlockPos matureRelative = new BlockPos(3, 24, 1);
        BlockPos immatureRelative = new BlockPos(4, 24, 1);
        BlockPos excludedRelative = new BlockPos(3, 24, 2);
        helper.setBlock(homeRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        for (BlockPos crop : java.util.List.of(matureRelative, immatureRelative, excludedRelative)) {
            helper.setBlock(crop.below(), Blocks.FARMLAND);
        }
        net.minecraft.world.level.block.CropBlock wheat =
                (net.minecraft.world.level.block.CropBlock) Blocks.WHEAT;
        helper.setBlock(matureRelative, wheat.getStateForAge(wheat.getMaxAge()));
        helper.setBlock(immatureRelative, wheat.getStateForAge(3));
        helper.setBlock(excludedRelative, wheat.getStateForAge(wheat.getMaxAge()));
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        BlockPos mature = helper.absolutePos(matureRelative);
        BlockPos immature = helper.absolutePos(immatureRelative);
        workZone.set(mature.getX(), mature.getZ(), true);
        workZone.set(immature.getX(), immature.getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, null));
        helper.assertTrue(home.installAttachment(Direction.UP, homeStation),
                "Harvester home station installation failed");
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.HARVESTER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Harvester failed to dock at its home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 1_000 && (!helper.getBlockState(matureRelative).isAir()
                || robot.harvesterPhase() != buildcraft.robotics.HarvesterPhase.NONE); tick++) {
            buildcraft.robotics.RobotStationRegistry.touch(
                    helper.getLevel(), home.getBlockPos(), Direction.UP);
            robot.tick();
        }
        helper.assertTrue(helper.getBlockState(matureRelative).isAir(),
                "Harvester did not harvest the mature zoned crop: phase=" + robot.harvesterPhase()
                        + ", task=" + robot.taskState());
        helper.assertTrue(helper.getBlockState(immatureRelative).is(Blocks.WHEAT)
                        && wheat.getAge(helper.getBlockState(immatureRelative)) == 3,
                "Harvester harvested an immature crop");
        helper.assertTrue(helper.getBlockState(excludedRelative).is(Blocks.WHEAT)
                        && wheat.isMaxAge(helper.getBlockState(excludedRelative)),
                "Harvester harvested a mature crop outside its work zone");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Harvester did not return home");
        helper.assertValueEqual(buildcraft.robotics.HarvesterPhase.NONE, robot.harvesterPhase(),
                "Harvester scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Harvester work consumed no battery energy");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(mature).inflate(3),
                item -> item.getItem().is(Items.WHEAT) || item.getItem().is(Items.WHEAT_SEEDS)).isEmpty(),
                "Harvester did not preserve crop drops");
        helper.succeed();
    }

    private static void roboticsMinerRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 36, 1);
        BlockPos sourceRelative = new BlockPos(2, 36, 1);
        BlockPos receiverRelative = new BlockPos(3, 36, 1);
        BlockPos ironRelative = new BlockPos(4, 36, 1);
        BlockPos diamondRelative = new BlockPos(5, 36, 1);
        BlockPos excludedRelative = new BlockPos(4, 36, 2);
        for (BlockPos relative : java.util.List.of(homeRelative, sourceRelative, receiverRelative)) {
            helper.setBlock(relative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        }
        helper.setBlock(sourceRelative.above(), Blocks.CHEST);
        helper.setBlock(receiverRelative.above(), Blocks.CHEST);
        helper.setBlock(ironRelative, Blocks.IRON_ORE);
        helper.setBlock(diamondRelative, Blocks.DIAMOND_ORE);
        helper.setBlock(excludedRelative, Blocks.IRON_ORE);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative));
        var receiver = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative));
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        BlockPos iron = helper.absolutePos(ironRelative);
        BlockPos diamond = helper.absolutePos(diamondRelative);
        workZone.set(iron.getX(), iron.getZ(), true);
        workZone.set(diamond.getX(), diamond.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(source.getBlockPos().getX(), source.getBlockPos().getZ(), true);
        loadZone.set(receiver.getBlockPos().getX(), receiver.getBlockPos().getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, loadZone));
        home.installAttachment(Direction.UP, homeStation);
        ItemStack sourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        sourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of()));
        source.installAttachment(Direction.UP, sourceStation);
        ItemStack receiverStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        receiverStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.RECEIVE,
                        java.util.List.of()));
        receiver.installAttachment(Direction.UP, receiverStation);
        net.minecraft.world.Container sourceChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative.above()));
        net.minecraft.world.Container receiverChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative.above()));
        ItemStack nearlyBrokenPickaxe = new ItemStack(Items.STONE_PICKAXE);
        nearlyBrokenPickaxe.setDamageValue(nearlyBrokenPickaxe.getMaxDamage() - 2);
        sourceChest.setItem(0, nearlyBrokenPickaxe);
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), receiver.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.MINER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Miner failed to dock at its home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 1_500 && (!helper.getBlockState(ironRelative).isAir()
                || receiverChest.getItem(0).isEmpty()
                || robot.minerPhase() != buildcraft.robotics.MinerPhase.NONE); tick++) {
            for (var pipe : java.util.List.of(home, source, receiver)) {
                buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), pipe.getBlockPos(), Direction.UP);
            }
            robot.tick();
        }
        helper.assertTrue(helper.getBlockState(ironRelative).isAir(),
                "Miner did not mine the reachable zoned ore: phase=" + robot.minerPhase()
                        + ", task=" + robot.taskState() + ", tool=" + robot.minerTool());
        helper.assertTrue(helper.getBlockState(diamondRelative).is(Blocks.DIAMOND_ORE),
                "Miner ignored its pickaxe harvest tier");
        helper.assertTrue(helper.getBlockState(excludedRelative).is(Blocks.IRON_ORE),
                "Miner mined ore outside its work zone");
        helper.assertTrue(sourceChest.getItem(0).isEmpty(), "Miner did not extract exactly one pickaxe");
        helper.assertTrue(receiverChest.getItem(0).is(Items.STONE_PICKAXE)
                        && receiverChest.getItem(0).getDamageValue()
                        == receiverChest.getItem(0).getMaxDamage() - 1,
                "Miner did not apply durability and unload its worn pickaxe");
        helper.assertTrue(robot.minerTool().isEmpty(), "Miner retained its unloaded pickaxe");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Miner did not return home");
        helper.assertValueEqual(buildcraft.robotics.MinerPhase.NONE, robot.minerPhase(),
                "Miner scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY - 1_000_000,
                "Miner work consumed insufficient battery energy");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(iron).inflate(2),
                item -> item.getItem().is(Items.RAW_IRON)).isEmpty(),
                "Miner did not preserve ore drops");
        helper.succeed();
    }

    private static void roboticsPlanterRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 48, 1);
        BlockPos sourceRelative = new BlockPos(2, 48, 1);
        BlockPos groundRelative = new BlockPos(3, 48, 1);
        BlockPos excludedRelative = new BlockPos(3, 48, 2);
        helper.setBlock(homeRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(sourceRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(sourceRelative.above(), Blocks.CHEST);
        helper.setBlock(groundRelative, Blocks.FARMLAND);
        helper.setBlock(excludedRelative, Blocks.FARMLAND);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative));
        BlockPos ground = helper.absolutePos(groundRelative);
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        workZone.set(ground.getX(), ground.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(source.getBlockPos().getX(), source.getBlockPos().getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, loadZone));
        home.installAttachment(Direction.UP, homeStation);
        ItemStack sourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        sourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of(new ItemStack(Items.WHEAT_SEEDS))));
        source.installAttachment(Direction.UP, sourceStation);
        net.minecraft.world.Container sourceChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative.above()));
        sourceChest.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 2));
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.PLANTER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Planter failed to dock at its home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 1_000 && (!helper.getBlockState(groundRelative.above()).is(Blocks.WHEAT)
                || robot.planterPhase() != buildcraft.robotics.PlanterPhase.NONE); tick++) {
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), home.getBlockPos(), Direction.UP);
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
            robot.tick();
        }
        helper.assertTrue(helper.getBlockState(groundRelative.above()).is(Blocks.WHEAT),
                "Planter did not plant on valid zoned soil: phase=" + robot.planterPhase()
                        + ", task=" + robot.taskState() + ", seed=" + robot.planterSeed());
        helper.assertTrue(helper.getBlockState(excludedRelative.above()).isAir(),
                "Planter planted outside its work zone");
        helper.assertTrue(sourceChest.getItem(0).is(Items.WHEAT_SEEDS)
                        && sourceChest.getItem(0).getCount() == 1,
                "Planter did not extract exactly one seed");
        helper.assertTrue(robot.planterSeed().isEmpty(), "Planter retained its consumed seed");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Planter did not return home");
        helper.assertValueEqual(buildcraft.robotics.PlanterPhase.NONE, robot.planterPhase(),
                "Planter scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Planter work consumed no battery energy");
        helper.succeed();
    }

    private static void roboticsFarmerRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 60, 1);
        BlockPos sourceRelative = new BlockPos(2, 60, 1);
        BlockPos targetRelative = new BlockPos(3, 60, 1);
        BlockPos excludedRelative = new BlockPos(3, 60, 2);
        helper.setBlock(homeRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(sourceRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(sourceRelative.above(), Blocks.CHEST);
        helper.setBlock(targetRelative, Blocks.DIRT);
        helper.setBlock(excludedRelative, Blocks.DIRT);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative));
        BlockPos target = helper.absolutePos(targetRelative);
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        workZone.set(target.getX(), target.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(source.getBlockPos().getX(), source.getBlockPos().getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, loadZone));
        home.installAttachment(Direction.UP, homeStation);
        ItemStack sourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        sourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of()));
        source.installAttachment(Direction.UP, sourceStation);
        net.minecraft.world.Container sourceChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative.above()));
        ItemStack finalUseHoe = new ItemStack(Items.WOODEN_HOE);
        finalUseHoe.setDamageValue(finalUseHoe.getMaxDamage() - 1);
        sourceChest.setItem(0, finalUseHoe);
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.FARMER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Farmer failed to dock at its home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 1_000 && (!helper.getBlockState(targetRelative).is(Blocks.FARMLAND)
                || robot.farmerPhase() != buildcraft.robotics.FarmerPhase.NONE); tick++) {
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), home.getBlockPos(), Direction.UP);
            buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
            robot.tick();
        }
        helper.assertTrue(helper.getBlockState(targetRelative).is(Blocks.FARMLAND),
                "Farmer did not till zoned dirt: phase=" + robot.farmerPhase()
                        + ", task=" + robot.taskState() + ", tool=" + robot.farmerTool());
        helper.assertTrue(helper.getBlockState(excludedRelative).is(Blocks.DIRT),
                "Farmer tilled dirt outside its work zone");
        helper.assertTrue(sourceChest.getItem(0).isEmpty(), "Farmer did not extract exactly one hoe");
        helper.assertTrue(robot.farmerTool().isEmpty(), "Farmer retained a broken hoe");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Farmer did not return home");
        helper.assertValueEqual(buildcraft.robotics.FarmerPhase.NONE, robot.farmerPhase(),
                "Farmer scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Farmer work consumed no battery energy");
        helper.succeed();
    }

    private static void roboticsLeafCutterRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 84, 1);
        BlockPos sourceRelative = new BlockPos(2, 84, 1);
        BlockPos receiverRelative = new BlockPos(3, 84, 1);
        BlockPos targetRelative = new BlockPos(4, 84, 1);
        BlockPos excludedRelative = new BlockPos(4, 84, 2);
        for (BlockPos relative : java.util.List.of(homeRelative, sourceRelative, receiverRelative)) {
            helper.setBlock(relative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        }
        helper.setBlock(sourceRelative.above(), Blocks.CHEST);
        helper.setBlock(receiverRelative.above(), Blocks.CHEST);
        var persistentLeaves = Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        helper.setBlock(targetRelative, persistentLeaves);
        helper.setBlock(excludedRelative, persistentLeaves);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative));
        var receiver = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative));
        BlockPos target = helper.absolutePos(targetRelative);
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        workZone.set(target.getX(), target.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(source.getBlockPos().getX(), source.getBlockPos().getZ(), true);
        loadZone.set(receiver.getBlockPos().getX(), receiver.getBlockPos().getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, loadZone));
        home.installAttachment(Direction.UP, homeStation);
        ItemStack sourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        sourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of()));
        source.installAttachment(Direction.UP, sourceStation);
        ItemStack receiverStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        receiverStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.RECEIVE,
                        java.util.List.of()));
        receiver.installAttachment(Direction.UP, receiverStation);
        net.minecraft.world.Container sourceChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative.above()));
        net.minecraft.world.Container receiverChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative.above()));
        ItemStack nearlyBrokenShears = new ItemStack(Items.SHEARS);
        nearlyBrokenShears.setDamageValue(nearlyBrokenShears.getMaxDamage() - 2);
        sourceChest.setItem(0, nearlyBrokenShears);
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), receiver.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.LEAF_CUTTER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Leaf Cutter failed to dock at its home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 1_500 && (!helper.getBlockState(targetRelative).isAir()
                || receiverChest.getItem(0).isEmpty()
                || robot.leafCutterPhase() != buildcraft.robotics.LeafCutterPhase.NONE); tick++) {
            for (var pipe : java.util.List.of(home, source, receiver)) {
                buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), pipe.getBlockPos(), Direction.UP);
            }
            robot.tick();
        }
        helper.assertTrue(helper.getBlockState(targetRelative).isAir(),
                "Leaf Cutter did not cut zoned leaves: phase=" + robot.leafCutterPhase()
                        + ", task=" + robot.taskState() + ", tool=" + robot.leafCutterTool());
        helper.assertTrue(helper.getBlockState(excludedRelative).is(Blocks.OAK_LEAVES),
                "Leaf Cutter cut leaves outside its work zone");
        helper.assertTrue(sourceChest.getItem(0).isEmpty(), "Leaf Cutter did not extract one Shears");
        helper.assertTrue(receiverChest.getItem(0).is(Items.SHEARS)
                        && receiverChest.getItem(0).getDamageValue()
                        == receiverChest.getItem(0).getMaxDamage() - 1,
                "Leaf Cutter did not damage and unload its worn Shears");
        helper.assertTrue(robot.leafCutterTool().isEmpty(), "Leaf Cutter retained unloaded Shears");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Leaf Cutter did not return home");
        helper.assertValueEqual(buildcraft.robotics.LeafCutterPhase.NONE, robot.leafCutterPhase(),
                "Leaf Cutter scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Leaf Cutter work consumed no battery energy");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(target).inflate(2),
                item -> item.getItem().is(Items.OAK_LEAVES)).isEmpty(),
                "Leaf Cutter did not preserve Shears leaf drops");
        helper.succeed();
    }

    private static void roboticsShovelmanRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 96, 1);
        BlockPos sourceRelative = new BlockPos(2, 96, 1);
        BlockPos receiverRelative = new BlockPos(3, 96, 1);
        BlockPos targetRelative = new BlockPos(4, 96, 1);
        BlockPos excludedRelative = new BlockPos(4, 96, 2);
        for (BlockPos relative : java.util.List.of(homeRelative, sourceRelative, receiverRelative)) {
            helper.setBlock(relative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        }
        helper.setBlock(sourceRelative.above(), Blocks.CHEST);
        helper.setBlock(receiverRelative.above(), Blocks.CHEST);
        helper.setBlock(targetRelative, Blocks.DIRT);
        helper.setBlock(excludedRelative, Blocks.DIRT);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative));
        var receiver = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative));
        BlockPos target = helper.absolutePos(targetRelative);
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        workZone.set(target.getX(), target.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(source.getBlockPos().getX(), source.getBlockPos().getZ(), true);
        loadZone.set(receiver.getBlockPos().getX(), receiver.getBlockPos().getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, loadZone));
        home.installAttachment(Direction.UP, homeStation);
        ItemStack sourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        sourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of()));
        source.installAttachment(Direction.UP, sourceStation);
        ItemStack receiverStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        receiverStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.RECEIVE,
                        java.util.List.of()));
        receiver.installAttachment(Direction.UP, receiverStation);
        var sourceChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative.above()));
        var receiverChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative.above()));
        ItemStack nearlyBrokenShovel = new ItemStack(Items.IRON_SHOVEL);
        nearlyBrokenShovel.setDamageValue(nearlyBrokenShovel.getMaxDamage() - 2);
        sourceChest.setItem(0, nearlyBrokenShovel);
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), receiver.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.SHOVELMAN);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Shovelman failed to dock at home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 1600 && (!helper.getBlockState(targetRelative).isAir()
                || receiverChest.getItem(0).isEmpty()
                || robot.shovelmanPhase() != buildcraft.robotics.ShovelmanPhase.NONE); tick++) {
            for (var pipe : java.util.List.of(home, source, receiver)) {
                buildcraft.robotics.RobotStationRegistry.touch(
                        helper.getLevel(), pipe.getBlockPos(), Direction.UP);
            }
            robot.tick();
        }
        helper.assertTrue(helper.getBlockState(targetRelative).isAir(),
                "Shovelman did not dig zoned dirt: phase=" + robot.shovelmanPhase()
                        + ", state=" + robot.taskState() + ", tool=" + robot.shovelmanTool());
        helper.assertTrue(helper.getBlockState(excludedRelative).is(Blocks.DIRT),
                "Shovelman dug dirt outside its work zone");
        helper.assertTrue(sourceChest.getItem(0).isEmpty(), "Shovelman did not extract one shovel");
        helper.assertTrue(receiverChest.getItem(0).is(Items.IRON_SHOVEL)
                        && receiverChest.getItem(0).getDamageValue()
                        == receiverChest.getItem(0).getMaxDamage() - 1,
                "Shovelman did not damage and unload its worn shovel");
        helper.assertTrue(robot.shovelmanTool().isEmpty(), "Shovelman retained unloaded shovel");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Shovelman did not return home");
        helper.assertValueEqual(buildcraft.robotics.ShovelmanPhase.NONE, robot.shovelmanPhase(),
                "Shovelman scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Shovelman work consumed no battery energy");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(target).inflate(2),
                        item -> item.getItem().is(Items.DIRT)).isEmpty(),
                "Shovelman did not preserve shovel drops");
        helper.succeed();
    }

    private static void roboticsButcherRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 108, 1);
        BlockPos sourceRelative = new BlockPos(2, 108, 1);
        BlockPos receiverRelative = new BlockPos(3, 108, 1);
        BlockPos targetRelative = new BlockPos(5, 108, 1);
        BlockPos excludedRelative = new BlockPos(5, 108, 3);
        for (BlockPos relative : java.util.List.of(homeRelative, sourceRelative, receiverRelative)) {
            helper.setBlock(relative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        }
        helper.setBlock(sourceRelative.above(), Blocks.CHEST);
        helper.setBlock(receiverRelative.above(), Blocks.CHEST);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative));
        var receiver = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative));
        BlockPos target = helper.absolutePos(targetRelative);
        BlockPos excluded = helper.absolutePos(excludedRelative);
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        workZone.set(target.getX(), target.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(source.getBlockPos().getX(), source.getBlockPos().getZ(), true);
        loadZone.set(receiver.getBlockPos().getX(), receiver.getBlockPos().getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, loadZone));
        home.installAttachment(Direction.UP, homeStation);
        ItemStack sourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        sourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of()));
        source.installAttachment(Direction.UP, sourceStation);
        ItemStack receiverStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        receiverStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.RECEIVE,
                        java.util.List.of()));
        receiver.installAttachment(Direction.UP, receiverStation);
        var sourceChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative.above()));
        var receiverChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative.above()));
        ItemStack nearlyBrokenSword = new ItemStack(Items.IRON_SWORD);
        nearlyBrokenSword.setDamageValue(nearlyBrokenSword.getMaxDamage() - 2);
        sourceChest.setItem(0, nearlyBrokenSword);
        var targetSheep = net.minecraft.world.entity.EntityType.SHEEP.create(
                helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var excludedSheep = net.minecraft.world.entity.EntityType.SHEEP.create(
                helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(targetSheep != null && excludedSheep != null, "Could not create Butcher test animals");
        targetSheep.setNoAi(true);
        excludedSheep.setNoAi(true);
        targetSheep.setHealth(1.0F);
        targetSheep.setPos(net.minecraft.world.phys.Vec3.atCenterOf(target));
        excludedSheep.setPos(net.minecraft.world.phys.Vec3.atCenterOf(excluded));
        helper.getLevel().addFreshEntity(targetSheep);
        helper.getLevel().addFreshEntity(excludedSheep);
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), receiver.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.BUTCHER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Butcher failed to dock at home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 1600 && (targetSheep.isAlive()
                || receiverChest.getItem(0).isEmpty()
                || robot.butcherPhase() != buildcraft.robotics.ButcherPhase.NONE); tick++) {
            for (var pipe : java.util.List.of(home, source, receiver)) {
                buildcraft.robotics.RobotStationRegistry.touch(
                        helper.getLevel(), pipe.getBlockPos(), Direction.UP);
            }
            robot.tick();
        }
        helper.assertTrue(!targetSheep.isAlive(),
                "Butcher did not kill zoned animal: phase=" + robot.butcherPhase()
                        + ", state=" + robot.taskState() + ", tool=" + robot.butcherTool());
        helper.assertTrue(excludedSheep.isAlive(), "Butcher attacked animal outside its work zone");
        helper.assertTrue(sourceChest.getItem(0).isEmpty(), "Butcher did not extract one sword");
        helper.assertTrue(receiverChest.getItem(0).is(Items.IRON_SWORD)
                        && receiverChest.getItem(0).getDamageValue()
                        == receiverChest.getItem(0).getMaxDamage() - 1,
                "Butcher did not damage and unload its worn sword");
        helper.assertTrue(robot.butcherTool().isEmpty(), "Butcher retained unloaded sword");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Butcher did not return home");
        helper.assertValueEqual(buildcraft.robotics.ButcherPhase.NONE, robot.butcherPhase(),
                "Butcher scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Butcher work consumed no battery energy");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(target).inflate(3),
                        item -> item.getItem().is(Items.WHITE_WOOL)
                                || item.getItem().is(Items.MUTTON)).isEmpty(),
                "Butcher did not preserve animal drops");
        helper.succeed();
    }

    private static void roboticsPumpRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 120, 1);
        BlockPos receiverRelative = new BlockPos(2, 120, 1);
        BlockPos lavaRelative = new BlockPos(3, 120, 1);
        BlockPos sourceRelative = new BlockPos(5, 120, 1);
        BlockPos excludedRelative = new BlockPos(5, 120, 3);
        helper.setBlock(homeRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(receiverRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(receiverRelative.above(), buildcraft.factory.BCFactoryBlocks.TANK.get());
        helper.setBlock(lavaRelative, Blocks.LAVA);
        helper.setBlock(sourceRelative, Blocks.WATER);
        helper.setBlock(excludedRelative, Blocks.WATER);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var receiver = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative));
        BlockPos lava = helper.absolutePos(lavaRelative);
        BlockPos source = helper.absolutePos(sourceRelative);
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        workZone.set(lava.getX(), lava.getZ(), true);
        workZone.set(source.getX(), source.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(receiver.getBlockPos().getX(), receiver.getBlockPos().getZ(), true);
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(
                net.minecraft.world.level.material.Fluids.WATER);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(water), workZone, loadZone));
        home.installAttachment(Direction.UP, homeStation);
        ItemStack receiverStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        receiverStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.RECEIVE,
                        java.util.List.of(), java.util.List.of(water)));
        receiver.installAttachment(Direction.UP, receiverStation);
        var receiverTank = ((buildcraft.factory.block.entity.TankBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(receiverRelative.above()))).localStorage();
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), receiver.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.PUMP);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Pump failed to dock at home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 1800 && (receiverTank.getAmountAsInt(0) != 1_000
                || robot.pumpPhase() != buildcraft.robotics.PumpPhase.NONE); tick++) {
            buildcraft.robotics.RobotStationRegistry.touch(
                    helper.getLevel(), home.getBlockPos(), Direction.UP);
            buildcraft.robotics.RobotStationRegistry.touch(
                    helper.getLevel(), receiver.getBlockPos(), Direction.UP);
            robot.tick();
        }
        helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(sourceRelative)).isEmpty(),
                "Pump did not drain zoned Water source: phase=" + robot.pumpPhase()
                        + ", state=" + robot.taskState());
        helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(lavaRelative)).isSource(),
                "Pump ignored linked-station Water filter");
        helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(excludedRelative)).isSource(),
                "Pump drained source outside its work zone");
        helper.assertTrue(receiverTank.getResource(0).equals(water)
                        && receiverTank.getAmountAsInt(0) == 1_000,
                "Pump did not unload exactly one source bucket");
        helper.assertValueEqual(0, robot.fluidTank().getAmountAsInt(0),
                "Pump retained unloaded fluid");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Pump did not return home");
        helper.assertValueEqual(buildcraft.robotics.PumpPhase.NONE, robot.pumpPhase(),
                "Pump scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Pump work consumed no battery energy");
        helper.succeed();
    }

    private static void roboticsKnightRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 132, 1);
        BlockPos sourceRelative = new BlockPos(2, 132, 1);
        BlockPos receiverRelative = new BlockPos(3, 132, 1);
        BlockPos targetRelative = new BlockPos(5, 132, 1);
        BlockPos passiveRelative = new BlockPos(5, 132, 2);
        BlockPos excludedRelative = new BlockPos(5, 132, 3);
        for (BlockPos relative : java.util.List.of(homeRelative, sourceRelative, receiverRelative)) {
            helper.setBlock(relative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        }
        helper.setBlock(sourceRelative.above(), Blocks.CHEST);
        helper.setBlock(receiverRelative.above(), Blocks.CHEST);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative));
        var receiver = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative));
        BlockPos target = helper.absolutePos(targetRelative);
        BlockPos passive = helper.absolutePos(passiveRelative);
        BlockPos excluded = helper.absolutePos(excludedRelative);
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        workZone.set(target.getX(), target.getZ(), true);
        workZone.set(passive.getX(), passive.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(source.getBlockPos().getX(), source.getBlockPos().getZ(), true);
        loadZone.set(receiver.getBlockPos().getX(), receiver.getBlockPos().getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, loadZone));
        home.installAttachment(Direction.UP, homeStation);
        ItemStack sourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        sourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of()));
        source.installAttachment(Direction.UP, sourceStation);
        ItemStack receiverStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        receiverStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.RECEIVE,
                        java.util.List.of()));
        receiver.installAttachment(Direction.UP, receiverStation);
        var sourceChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative.above()));
        var receiverChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(receiverRelative.above()));
        ItemStack nearlyBrokenSword = new ItemStack(Items.IRON_SWORD);
        nearlyBrokenSword.setDamageValue(nearlyBrokenSword.getMaxDamage() - 2);
        sourceChest.setItem(0, nearlyBrokenSword);
        var targetZombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(
                helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var excludedZombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(
                helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var passiveWolf = net.minecraft.world.entity.EntityType.WOLF.create(
                helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(targetZombie != null && excludedZombie != null && passiveWolf != null,
                "Could not create Knight test entities");
        targetZombie.setNoAi(true);
        excludedZombie.setNoAi(true);
        passiveWolf.setNoAi(true);
        targetZombie.setHealth(1.0F);
        targetZombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new ItemStack(Items.GOLD_INGOT));
        targetZombie.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 2.0F);
        targetZombie.setPos(net.minecraft.world.phys.Vec3.atCenterOf(target));
        passiveWolf.setPos(net.minecraft.world.phys.Vec3.atCenterOf(passive));
        excludedZombie.setPos(net.minecraft.world.phys.Vec3.atCenterOf(excluded));
        helper.getLevel().addFreshEntity(targetZombie);
        helper.getLevel().addFreshEntity(passiveWolf);
        helper.getLevel().addFreshEntity(excludedZombie);
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), receiver.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.KNIGHT);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Knight failed to dock at home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 1600 && (targetZombie.isAlive()
                || receiverChest.getItem(0).isEmpty()
                || robot.knightPhase() != buildcraft.robotics.KnightPhase.NONE); tick++) {
            for (var pipe : java.util.List.of(home, source, receiver)) {
                buildcraft.robotics.RobotStationRegistry.touch(
                        helper.getLevel(), pipe.getBlockPos(), Direction.UP);
            }
            robot.tick();
        }
        helper.assertTrue(!targetZombie.isAlive(),
                "Knight did not kill zoned hostile: phase=" + robot.knightPhase()
                        + ", state=" + robot.taskState() + ", tool=" + robot.knightTool());
        helper.assertTrue(passiveWolf.isAlive(), "Knight attacked a non-angry Wolf");
        helper.assertTrue(excludedZombie.isAlive(), "Knight attacked hostile outside its work zone");
        helper.assertTrue(sourceChest.getItem(0).isEmpty(), "Knight did not extract one Sword");
        helper.assertTrue(receiverChest.getItem(0).is(Items.IRON_SWORD)
                        && receiverChest.getItem(0).getDamageValue()
                        == receiverChest.getItem(0).getMaxDamage() - 1,
                "Knight did not damage and unload its worn Sword");
        helper.assertTrue(robot.knightTool().isEmpty(), "Knight retained unloaded Sword");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Knight did not return home");
        helper.assertValueEqual(buildcraft.robotics.KnightPhase.NONE, robot.knightPhase(),
                "Knight scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Knight work consumed no battery energy");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(target).inflate(3),
                        item -> item.getItem().is(Items.GOLD_INGOT)).isEmpty(),
                "Knight did not preserve hostile drops");
        helper.succeed();
    }

    private static void roboticsBomberRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 156, 1);
        BlockPos sourceRelative = new BlockPos(2, 156, 1);
        BlockPos targetRelative = new BlockPos(6, 156, 1);
        BlockPos excludedRelative = new BlockPos(6, 156, 3);
        helper.setBlock(homeRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(sourceRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(sourceRelative.above(), Blocks.CHEST);
        helper.setBlock(targetRelative, Blocks.STONE);
        helper.setBlock(excludedRelative, Blocks.STONE);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative));
        BlockPos target = helper.absolutePos(targetRelative);
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        workZone.set(target.getX(), target.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(source.getBlockPos().getX(), source.getBlockPos().getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, loadZone));
        home.installAttachment(Direction.UP, homeStation);
        ItemStack sourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        sourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of(new ItemStack(Items.TNT))));
        source.installAttachment(Direction.UP, sourceStation);
        var sourceChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative.above()));
        sourceChest.setItem(0, new ItemStack(Items.TNT));
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.BOMBER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Bomber failed to dock at home station");
        helper.getLevel().addFreshEntity(robot);
        java.util.List<net.minecraft.world.entity.item.PrimedTnt> primed = java.util.List.of();
        for (int tick = 0; tick < 1800 && (primed.isEmpty()
                || robot.bomberPhase() != buildcraft.robotics.BomberPhase.NONE); tick++) {
            buildcraft.robotics.RobotStationRegistry.touch(
                    helper.getLevel(), home.getBlockPos(), Direction.UP);
            buildcraft.robotics.RobotStationRegistry.touch(
                    helper.getLevel(), source.getBlockPos(), Direction.UP);
            robot.tick();
            primed = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,
                    new net.minecraft.world.phys.AABB(target).inflate(30));
        }
        helper.assertTrue(!primed.isEmpty(),
                "Bomber did not prime TNT above zoned ground: phase=" + robot.bomberPhase()
                        + ", state=" + robot.taskState());
        var tnt = primed.getFirst();
        helper.assertValueEqual(37, tnt.getFuse(), "Bomber used the wrong historical TNT fuse");
        helper.assertTrue(Math.pow(tnt.getX() - (target.getX() + 0.5), 2)
                        + Math.pow(tnt.getZ() - (target.getZ() + 0.5), 2) < 4
                        && tnt.getY() > target.getY() + 15,
                "Bomber did not drop TNT from 20 blocks above its zoned target");
        helper.assertTrue(sourceChest.getItem(0).isEmpty(), "Bomber did not load TNT from Provide station");
        helper.assertTrue(robot.isEmpty(), "Bomber retained its dropped TNT");
        helper.assertTrue(helper.getBlockState(excludedRelative).is(Blocks.STONE),
                "Bomber modified ground outside its work zone before detonation");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Bomber did not return home after exhausting TNT");
        helper.assertValueEqual(buildcraft.robotics.BomberPhase.NONE, robot.bomberPhase(),
                "Bomber scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Bomber work consumed no battery energy");
        helper.succeed();
    }

    private static void roboticsStripesRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 168, 1);
        BlockPos sourceRelative = new BlockPos(2, 168, 1);
        BlockPos targetRelative = new BlockPos(5, 169, 1);
        BlockPos deployedRelative = targetRelative.north();
        BlockPos excludedRelative = new BlockPos(5, 169, 3);
        helper.setBlock(homeRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(sourceRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(sourceRelative.above(), Blocks.CHEST);
        helper.setBlock(deployedRelative.below(), Blocks.STONE);
        helper.setBlock(excludedRelative.below(), Blocks.STONE);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative));
        BlockPos target = helper.absolutePos(targetRelative);
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        workZone.set(target.getX(), target.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(source.getBlockPos().getX(), source.getBlockPos().getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, loadZone));
        home.installAttachment(Direction.UP, homeStation);
        ItemStack sourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        sourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of(new ItemStack(Items.COBBLESTONE))));
        source.installAttachment(Direction.UP, sourceStation);
        var sourceChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative.above()));
        sourceChest.setItem(0, new ItemStack(Items.COBBLESTONE));
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.STRIPES);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Stripes failed to dock at home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 1600 && (!helper.getBlockState(deployedRelative).is(Blocks.COBBLESTONE)
                || robot.stripesPhase() != buildcraft.robotics.StripesPhase.NONE); tick++) {
            buildcraft.robotics.RobotStationRegistry.touch(
                    helper.getLevel(), home.getBlockPos(), Direction.UP);
            buildcraft.robotics.RobotStationRegistry.touch(
                    helper.getLevel(), source.getBlockPos(), Direction.UP);
            robot.tick();
        }
        helper.assertTrue(helper.getBlockState(deployedRelative).is(Blocks.COBBLESTONE),
                "Stripes did not use BlockItem in zoned air: phase=" + robot.stripesPhase()
                        + ", state=" + robot.taskState() + ", item=" + robot.stripesItem()
                        + ", target=" + robot.stripesBlockTarget()
                        + ", attempts=" + robot.stripesSearchAttempts()
                        + ", position=" + robot.position()
                        + ", activation=" + helper.getBlockState(targetRelative)
                        + ", deployment=" + helper.getBlockState(deployedRelative)
                        + ", support=" + helper.getBlockState(deployedRelative.below()));
        helper.assertTrue(helper.getBlockState(excludedRelative).isAir(),
                "Stripes deployed item outside its work zone");
        helper.assertTrue(sourceChest.getItem(0).isEmpty(),
                "Stripes did not extract exactly one arbitrary item");
        helper.assertTrue(robot.stripesItem().isEmpty(), "Stripes retained consumed deployment item");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Stripes did not return home");
        helper.assertValueEqual(buildcraft.robotics.StripesPhase.NONE, robot.stripesPhase(),
                "Stripes scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Stripes work consumed no battery energy");
        helper.succeed();
    }

    private static void roboticsBuilderRobot(GameTestHelper helper) {
        BlockPos homeRelative = new BlockPos(1, 180, 1);
        BlockPos sourceRelative = new BlockPos(2, 180, 1);
        BlockPos markerRelative = new BlockPos(5, 180, 1);
        BlockPos placeRelative = markerRelative.east();
        BlockPos clearRelative = placeRelative.east();
        helper.setBlock(homeRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(sourceRelative, buildcraft.transport.BCTransportBlocks.PIPE_HOLDER.get());
        helper.setBlock(sourceRelative.above(), Blocks.CHEST);
        helper.setBlock(markerRelative.below(), Blocks.STONE);
        helper.setBlock(markerRelative, buildcraft.builders.BCBuildersBlocks.CONSTRUCTION_MARKER.get());
        helper.setBlock(placeRelative.below(), Blocks.STONE);
        helper.setBlock(clearRelative, Blocks.DIRT);
        var home = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(homeRelative));
        var source = (buildcraft.transport.block.entity.PipeHolderBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative));
        var marker = (buildcraft.builders.block.entity.ConstructionMarkerBlockEntity)
                helper.getLevel().getBlockEntity(helper.absolutePos(markerRelative));
        BlockPos markerPos = helper.absolutePos(markerRelative);
        var workZone = new buildcraft.robotics.zone.ZonePlan();
        workZone.set(markerPos.getX(), markerPos.getZ(), true);
        var loadZone = new buildcraft.robotics.zone.ZonePlan();
        loadZone.set(source.getBlockPos().getX(), source.getBlockPos().getZ(), true);
        ItemStack homeStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        homeStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.DISABLED,
                        java.util.List.of(), java.util.List.of(), workZone, loadZone));
        home.installAttachment(Direction.UP, homeStation);
        ItemStack sourceStation = new ItemStack(buildcraft.robotics.BCRoboticsItems.ROBOT_STATION.get());
        sourceStation.set(buildcraft.robotics.BCRoboticsDataComponents.ROBOT_STATION_CONFIG.get(),
                new buildcraft.robotics.RobotStationConfig(buildcraft.robotics.RobotStationMode.PROVIDE,
                        java.util.List.of(new ItemStack(Items.COBBLESTONE))));
        source.installAttachment(Direction.UP, sourceStation);
        var sourceChest = (net.minecraft.world.Container)
                helper.getLevel().getBlockEntity(helper.absolutePos(sourceRelative.above()));
        sourceChest.setItem(0, new ItemStack(Items.COBBLESTONE));
        var snapshot = new buildcraft.builders.snapshot.SnapshotData(
                buildcraft.builders.snapshot.SnapshotKind.BLUEPRINT,
                new BlockPos(2, 1, 1), Direction.WEST, new BlockPos(1, 0, 0),
                java.util.List.of(Blocks.COBBLESTONE.defaultBlockState(), Blocks.AIR.defaultBlockState()),
                java.util.List.of(0, 1), "Robot Builder", true, true, false, false);
        ItemStack blueprint = new ItemStack(buildcraft.builders.BCBuildersItems.BLUEPRINT.get());
        blueprint.set(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get(), snapshot);
        helper.assertTrue(marker.setBlueprint(blueprint), "Builder robot marker rejected Blueprint");
        buildcraft.builders.ConstructionMarkerRegistry.add(helper.getLevel(), marker);
        var homeRegistry = buildcraft.robotics.RobotStationRegistry.touch(
                helper.getLevel(), home.getBlockPos(), Direction.UP);
        buildcraft.robotics.RobotStationRegistry.touch(helper.getLevel(), source.getBlockPos(), Direction.UP);
        var robot = new buildcraft.robotics.entity.RobotEntity(
                buildcraft.robotics.BCRoboticsEntities.ROBOT.get(), helper.getLevel());
        robot.setBoard(buildcraft.robotics.RobotBoardType.BUILDER);
        robot.setEnergy(buildcraft.robotics.RobotItemData.MAX_ENERGY);
        helper.assertTrue(homeRegistry.reserve(robot.getUUID()) && robot.dock(homeRegistry),
                "Builder failed to dock at home station");
        helper.getLevel().addFreshEntity(robot);
        for (int tick = 0; tick < 2000 && (!helper.getBlockState(placeRelative).is(Blocks.COBBLESTONE)
                || !helper.getBlockState(clearRelative).isAir()
                || robot.builderPhase() != buildcraft.robotics.BuilderPhase.NONE); tick++) {
            buildcraft.robotics.RobotStationRegistry.touch(
                    helper.getLevel(), home.getBlockPos(), Direction.UP);
            buildcraft.robotics.RobotStationRegistry.touch(
                    helper.getLevel(), source.getBlockPos(), Direction.UP);
            robot.tick();
        }
        helper.assertTrue(helper.getBlockState(placeRelative).is(Blocks.COBBLESTONE),
                "Builder did not place Blueprint material: phase=" + robot.builderPhase()
                        + ", state=" + robot.taskState() + ", target=" + robot.builderBlockTarget());
        helper.assertTrue(helper.getBlockState(clearRelative).isAir(),
                "Builder did not excavate Blueprint air slot");
        helper.assertTrue(sourceChest.getItem(0).isEmpty(),
                "Builder did not transactionally fetch exact material");
        helper.assertTrue(robot.isEmpty(), "Builder retained consumed material");
        helper.assertValueEqual(buildcraft.robotics.RobotTaskState.DOCKED, robot.taskState(),
                "Builder did not return home after completing marker");
        helper.assertValueEqual(buildcraft.robotics.BuilderPhase.NONE, robot.builderPhase(),
                "Builder scheduler did not finish");
        helper.assertTrue(robot.energy() < buildcraft.robotics.RobotItemData.MAX_ENERGY,
                "Builder construction consumed no battery energy");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(
                        net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(helper.absolutePos(clearRelative)).inflate(3),
                        item -> item.getItem().is(Items.DIRT)).isEmpty(),
                "Builder did not preserve excavation drops");
        helper.succeed();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, path);
    }
}
