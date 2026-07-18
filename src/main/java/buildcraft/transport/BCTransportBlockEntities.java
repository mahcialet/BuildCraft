package buildcraft.transport;

import buildcraft.api.mj.MjAPI;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCTransportBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BCTransport.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeHolderBlockEntity>> PIPE_HOLDER =
        BLOCK_ENTITIES.register("pipe_holder", () -> new BlockEntityType<>(
            PipeHolderBlockEntity::new, false, BCTransportBlocks.PIPE_HOLDER.get()));

    private BCTransportBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        bus.addListener(BCTransportBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, PIPE_HOLDER.get(),
            (holder, side) -> side != null && holder.pipeType().carriesItems() ? holder.input(side) : null);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, PIPE_HOLDER.get(),
            (holder, side) -> side == null ? null : holder.fluidBuffer(side));
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, PIPE_HOLDER.get(), (holder, side) -> holder.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, PIPE_HOLDER.get(), (holder, side) -> holder.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_REDSTONE_RECEIVER, PIPE_HOLDER.get(),
            (holder, side) -> holder.mjReceiver());
    }
}
