package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.core.block.entity.PathMarkerBlockEntity;
import buildcraft.core.block.entity.VolumeMarkerBlockEntity;
import buildcraft.core.block.entity.RedstoneEngineBlockEntity;
import buildcraft.core.block.entity.CreativeEngineBlockEntity;
import buildcraft.core.block.entity.PowerTesterBlockEntity;
import buildcraft.api.mj.MjAPI;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entities used by migrated BuildCraft core blocks. */
public final class BCCoreBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BuildCraft.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PathMarkerBlockEntity>> MARKER_PATH =
        BLOCK_ENTITIES.register("marker_path", () -> new BlockEntityType<>(
            PathMarkerBlockEntity::new, BCCoreBlocks.MARKER_PATH.get()
        ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VolumeMarkerBlockEntity>> MARKER_VOLUME =
        BLOCK_ENTITIES.register("marker_volume", () -> new BlockEntityType<>(
            VolumeMarkerBlockEntity::new, BCCoreBlocks.MARKER_VOLUME.get()
        ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RedstoneEngineBlockEntity>> ENGINE_REDSTONE =
        BLOCK_ENTITIES.register("engine_redstone", () -> new BlockEntityType<>(
            RedstoneEngineBlockEntity::new, BCCoreBlocks.ENGINE.get()
        ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeEngineBlockEntity>> ENGINE_CREATIVE =
        BLOCK_ENTITIES.register("engine_creative", () -> new BlockEntityType<>(
            CreativeEngineBlockEntity::new, BCCoreBlocks.ENGINE.get()
        ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerTesterBlockEntity>> POWER_TESTER =
        BLOCK_ENTITIES.register("power_tester", () -> new BlockEntityType<>(
            PowerTesterBlockEntity::new, BCCoreBlocks.POWER_TESTER.get()
        ));

    private BCCoreBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(BCCoreBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, POWER_TESTER.get(), (tester, side) -> tester);
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, POWER_TESTER.get(), (tester, side) -> tester);
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, ENGINE_REDSTONE.get(),
            (engine, side) -> engine.connector(side));
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, ENGINE_CREATIVE.get(),
                (engine, side) -> engine.connector(side));
        event.registerBlockEntity(buildcraft.api.core.EngineAPI.CAP_POWER_STAGE,
                ENGINE_REDSTONE.get(), (engine, side) -> engine);
        event.registerBlockEntity(buildcraft.api.core.EngineAPI.CAP_POWER_STAGE,
                ENGINE_CREATIVE.get(), (engine, side) -> engine);
    }
}
