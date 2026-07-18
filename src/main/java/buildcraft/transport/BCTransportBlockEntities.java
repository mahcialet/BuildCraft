package buildcraft.transport;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCTransportBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BCTransport.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeHolderBlockEntity>> PIPE_HOLDER =
        BLOCK_ENTITIES.register("pipe_holder", () -> new BlockEntityType<>(
            PipeHolderBlockEntity::new, BCTransportBlocks.PIPE_HOLDER.get()
        ));

    private BCTransportBlockEntities() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
