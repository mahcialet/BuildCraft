package buildcraft.builders;

import buildcraft.api.core.MachineAPI;
import buildcraft.api.mj.MjAPI;
import buildcraft.builders.block.entity.FillerBlockEntity;
import buildcraft.builders.block.entity.ArchitectTableBlockEntity;
import buildcraft.builders.block.entity.BuilderBlockEntity;
import buildcraft.builders.block.entity.ReplacerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCBuildersBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BCBuilders.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FillerBlockEntity>> FILLER =
            BLOCK_ENTITIES.register("filler", () -> new BlockEntityType<>(
                    FillerBlockEntity::new, BCBuildersBlocks.FILLER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArchitectTableBlockEntity>> ARCHITECT_TABLE =
            BLOCK_ENTITIES.register("architect_table", () -> new BlockEntityType<>(
                    ArchitectTableBlockEntity::new, BCBuildersBlocks.ARCHITECT_TABLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BuilderBlockEntity>> BUILDER =
            BLOCK_ENTITIES.register("builder", () -> new BlockEntityType<>(BuilderBlockEntity::new, BCBuildersBlocks.BUILDER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReplacerBlockEntity>> REPLACER =
            BLOCK_ENTITIES.register("replacer", () -> new BlockEntityType<>(ReplacerBlockEntity::new, BCBuildersBlocks.REPLACER.get()));

    private BCBuildersBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        bus.addListener(BCBuildersBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, FILLER.get(),
                (filler, side) -> filler.resources());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ARCHITECT_TABLE.get(),
                (table, side) -> table.inventory());
        event.registerBlockEntity(Capabilities.Item.BLOCK, BUILDER.get(), (builder, side) -> builder.inventory());
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, BUILDER.get(), (builder, side) -> builder.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, BUILDER.get(), (builder, side) -> builder.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_READABLE, BUILDER.get(), (builder, side) -> builder.mjReceiver());
        event.registerBlockEntity(MachineAPI.CAP_HAS_WORK, BUILDER.get(), (builder, side) -> builder);
        event.registerBlockEntity(MachineAPI.CAP_CONTROLLABLE, BUILDER.get(), (builder, side) -> builder);
        event.registerBlockEntity(Capabilities.Item.BLOCK, REPLACER.get(), (replacer, side) -> replacer.inventory());
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, FILLER.get(),
                (filler, side) -> filler.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, FILLER.get(),
                (filler, side) -> filler.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_READABLE, FILLER.get(),
                (filler, side) -> filler.mjReceiver());
        event.registerBlockEntity(MachineAPI.CAP_HAS_WORK, FILLER.get(),
                (filler, side) -> filler);
        event.registerBlockEntity(MachineAPI.CAP_CONTROLLABLE, FILLER.get(),
                (filler, side) -> filler);
    }
}
