package buildcraft.silicon;

import buildcraft.api.mj.MjAPI;
import buildcraft.silicon.block.entity.AssemblyTableBlockEntity;
import buildcraft.silicon.block.entity.LaserBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCSiliconBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BCSilicon.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaserBlockEntity>> LASER =
        BLOCK_ENTITIES.register("laser", () -> new BlockEntityType<>(LaserBlockEntity::new, BCSiliconBlocks.LASER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblyTableBlockEntity>> ASSEMBLY_TABLE =
        BLOCK_ENTITIES.register("assembly_table", () -> new BlockEntityType<>(
            AssemblyTableBlockEntity::new, BCSiliconBlocks.ASSEMBLY_TABLE.get()));

    private BCSiliconBlockEntities() {}
    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        bus.addListener(BCSiliconBlockEntities::registerCapabilities);
    }
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, LASER.get(), (laser, side) -> laser.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, LASER.get(), (laser, side) -> laser.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_READABLE, LASER.get(), (laser, side) -> laser.mjReceiver());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ASSEMBLY_TABLE.get(),
            (table, side) -> table.inventory());
    }
}
