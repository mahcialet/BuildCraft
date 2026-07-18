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
        registerTest(event, environment, "fragile_fluid_shard", BCCoreGameTests::fragileFluidShard);
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
        registerTest(event, environment, "mj_dynamo", BCCoreGameTests::mjDynamo);
        registerTest(event, environment, "transport_pipe_foundation", BCCoreGameTests::transportPipeFoundation);
        registerTest(event, environment, "transport_fluid_pipe_foundation", BCCoreGameTests::transportFluidPipeFoundation);
        registerTest(event, environment, "transport_wood_fluid_pipe", BCCoreGameTests::transportWoodFluidPipe);
        registerTest(event, environment, "transport_fast_isolated_fluid_pipes", BCCoreGameTests::transportFastIsolatedFluidPipes);
        registerTest(event, environment, "transport_iron_fluid_pipe", BCCoreGameTests::transportIronFluidPipe);
        registerTest(event, environment, "transport_clay_void_fluid_pipes", BCCoreGameTests::transportClayVoidFluidPipes);
        registerTest(event, environment, "transport_diamond_fluid_pipe", BCCoreGameTests::transportDiamondFluidPipe);
        registerTest(event, environment, "transport_diamond_wood_fluid_pipe", BCCoreGameTests::transportDiamondWoodFluidPipe);
        registerTest(event, environment, "transport_item_flow", BCCoreGameTests::transportItemFlow);
        registerTest(event, environment, "transport_special_item_pipes", BCCoreGameTests::transportSpecialItemPipes);
        registerTest(event, environment, "transport_routing_item_pipes", BCCoreGameTests::transportRoutingItemPipes);
        registerTest(event, environment, "transport_terminal_item_pipes", BCCoreGameTests::transportTerminalItemPipes);
        registerTest(event, environment, "transport_colored_item_pipes", BCCoreGameTests::transportColoredItemPipes);
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
        helper.assertValueEqual(oil.getTotalBurningTime(), 10_000, "oil burn time");
        helper.assertTrue(fuel != null, "light fuel definition missing");
        helper.assertValueEqual(fuel.getPowerPerCycle(), 6 * MjAPI.MJ, "light fuel power");
        helper.assertValueEqual(fuel.getTotalBurningTime(), 15_000, "light fuel burn time");
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

        var recipeInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(
                new ItemStack(buildcraft.transport.BCTransportItems.PIPE_WOOD_ITEM.get()),
                new ItemStack(Items.SLIME_BALL)));
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
                new ItemStack(inputPipe), new ItemStack(Items.SLIME_BALL)));
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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, path);
    }
}
