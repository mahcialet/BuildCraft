package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.api.enums.EnumDecoratedBlock;
import buildcraft.core.block.BlockDecoration;
import buildcraft.core.gametest.BuildCraftGameTestInstance;
import buildcraft.core.item.ItemBlockDecoration;
import buildcraft.core.item.ItemMarkerConnector;
import buildcraft.core.marker.PathConnection;
import buildcraft.core.marker.PathSavedData;
import buildcraft.core.block.entity.PathMarkerBlockEntity;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(BuildCraft.MOD_ID, path);
    }
}
