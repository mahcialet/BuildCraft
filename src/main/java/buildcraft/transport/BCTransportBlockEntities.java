package buildcraft.transport;

import buildcraft.api.mj.MjAPI;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import buildcraft.transport.block.entity.FilteredBufferBlockEntity;
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
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FilteredBufferBlockEntity>> FILTERED_BUFFER =
        BLOCK_ENTITIES.register("filtered_buffer", () -> new BlockEntityType<>(
            FilteredBufferBlockEntity::new, false, BCTransportBlocks.FILTERED_BUFFER.get()));

    private BCTransportBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        bus.addListener(BCTransportBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, PIPE_HOLDER.get(),
            (holder, side) -> side != null && holder.pipeType().carriesItems() ? holder.input(side) : null);
        event.registerBlockEntity(Capabilities.Item.BLOCK, FILTERED_BUFFER.get(),
            (buffer, side) -> buffer.inventory());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, PIPE_HOLDER.get(),
            (holder, side) -> holder.attachmentEnergyHandler(side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, PIPE_HOLDER.get(),
            (holder, side) -> side == null ? null : holder.fluidBuffer(side));
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, PIPE_HOLDER.get(), (holder, side) -> holder.mjConnector());
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, PIPE_HOLDER.get(), (holder, side) -> {
            buildcraft.api.mj.IMjReceiver attachment = holder.attachmentMjReceiver(side);
            if (attachment != null) return attachment;
            return holder.pipeType().isWoodenPowerInput()
                    ? (buildcraft.api.mj.IMjReceiver) holder.mjConnector() : holder.mjReceiver();
        });
        event.registerBlockEntity(MjAPI.CAP_REDSTONE_RECEIVER, PIPE_HOLDER.get(),
            (holder, side) -> holder.mjReceiver());
    }
}
