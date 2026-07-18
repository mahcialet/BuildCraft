package buildcraft.core;

import buildcraft.BuildCraft;
import buildcraft.core.block.entity.PathMarkerBlockEntity;
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

    private BCCoreBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
