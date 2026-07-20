package buildcraft.silicon;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjCapabilityHelper;
import buildcraft.silicon.block.entity.AssemblyTableBlockEntity;
import buildcraft.silicon.block.entity.LaserBlockEntity;
import buildcraft.silicon.block.entity.AdvancedCraftingTableBlockEntity;
import buildcraft.silicon.block.entity.IntegrationTableBlockEntity;
import buildcraft.silicon.block.entity.ChargingTableBlockEntity;
import buildcraft.silicon.block.entity.ProgrammingTableBlockEntity;
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
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedCraftingTableBlockEntity>>
        ADVANCED_CRAFTING_TABLE = BLOCK_ENTITIES.register("advanced_crafting_table", () -> new BlockEntityType<>(
            AdvancedCraftingTableBlockEntity::new, BCSiliconBlocks.ADVANCED_CRAFTING_TABLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IntegrationTableBlockEntity>>
        INTEGRATION_TABLE = BLOCK_ENTITIES.register("integration_table", () -> new BlockEntityType<>(
            IntegrationTableBlockEntity::new, BCSiliconBlocks.INTEGRATION_TABLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargingTableBlockEntity>>
        CHARGING_TABLE = BLOCK_ENTITIES.register("charging_table", () -> new BlockEntityType<>(
            ChargingTableBlockEntity::new, BCSiliconBlocks.CHARGING_TABLE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProgrammingTableBlockEntity>>
        PROGRAMMING_TABLE = BLOCK_ENTITIES.register("programming_table", () -> new BlockEntityType<>(
            ProgrammingTableBlockEntity::new, BCSiliconBlocks.PROGRAMMING_TABLE.get()));

    private BCSiliconBlockEntities() {}
    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        bus.addListener(BCSiliconBlockEntities::registerCapabilities);
    }
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, LASER.get(), (laser, side) -> laser.mjReceiver());
        event.registerBlockEntity(Capabilities.Energy.BLOCK, LASER.get(),
            (laser, side) -> new MjCapabilityHelper(laser.mjReceiver()).energy());
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, LASER.get(), (laser, side) -> laser.mjReceiver());
        event.registerBlockEntity(MjAPI.CAP_READABLE, LASER.get(), (laser, side) -> laser.mjReceiver());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ASSEMBLY_TABLE.get(),
            (table, side) -> table.inventory());
        event.registerBlockEntity(Capabilities.Item.BLOCK, ADVANCED_CRAFTING_TABLE.get(),
            (table, side) -> table.itemHandler());
        event.registerBlockEntity(Capabilities.Item.BLOCK, INTEGRATION_TABLE.get(),
            (table, side) -> table.itemHandler());
    }
}
