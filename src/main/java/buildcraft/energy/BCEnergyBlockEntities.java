package buildcraft.energy;

import buildcraft.api.mj.MjAPI;
import buildcraft.core.BCCoreBlocks;
import buildcraft.energy.block.entity.StirlingEngineBlockEntity;
import buildcraft.energy.block.entity.CombustionEngineBlockEntity;
import buildcraft.energy.block.entity.RfEngineBlockEntity;
import buildcraft.energy.block.entity.DynamoMjBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entities owned by the Energy module. */
public final class BCEnergyBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BCEnergy.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StirlingEngineBlockEntity>> ENGINE_STIRLING =
        BLOCK_ENTITIES.register("engine_stirling", () -> new BlockEntityType<>(
            StirlingEngineBlockEntity::new, BCCoreBlocks.ENGINE.get()
        ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CombustionEngineBlockEntity>> ENGINE_COMBUSTION =
        BLOCK_ENTITIES.register("engine_combustion", () -> new BlockEntityType<>(
            CombustionEngineBlockEntity::new, BCCoreBlocks.ENGINE.get()
        ));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RfEngineBlockEntity>> ENGINE_RF =
        BLOCK_ENTITIES.register("engine_rf", () -> new BlockEntityType<>(
            RfEngineBlockEntity::new, BCCoreBlocks.ENGINE.get()
        ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DynamoMjBlockEntity>> MJ_DYNAMO =
        BLOCK_ENTITIES.register("mj_dynamo", () -> new BlockEntityType<>(
            DynamoMjBlockEntity::new, BCEnergyBlocks.MJ_DYNAMO.get()
        ));

    private BCEnergyBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(BCEnergyBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(buildcraft.api.core.EngineAPI.CAP_POWER_STAGE,
                ENGINE_STIRLING.get(), (engine, side) -> engine);
        event.registerBlockEntity(buildcraft.api.core.EngineAPI.CAP_POWER_STAGE,
                ENGINE_COMBUSTION.get(), (engine, side) -> engine);
        event.registerBlockEntity(buildcraft.api.core.EngineAPI.CAP_POWER_STAGE,
                ENGINE_RF.get(), (engine, side) -> engine);
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, ENGINE_STIRLING.get(),
            (engine, side) -> engine.connector(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ENGINE_STIRLING.get(),
            (engine, side) -> engine.fuelInventory());
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, ENGINE_COMBUSTION.get(),
            (engine, side) -> engine.connector(side));
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ENGINE_COMBUSTION.get(),
            (engine, side) -> engine.tanks());
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, ENGINE_RF.get(),
            (engine, side) -> engine.connector(side));
        event.registerBlockEntity(Capabilities.Energy.BLOCK, ENGINE_RF.get(),
            (engine, side) -> engine.energy());
        event.registerBlockEntity(MjAPI.CAP_RECEIVER, MJ_DYNAMO.get(),
            (dynamo, side) -> side != dynamo.outputDirection() ? dynamo.mjReceiver() : null);
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, MJ_DYNAMO.get(),
            (dynamo, side) -> side != dynamo.outputDirection() ? dynamo.mjReceiver() : null);
        event.registerBlockEntity(MjAPI.CAP_READABLE, MJ_DYNAMO.get(),
            (dynamo, side) -> side != dynamo.outputDirection() ? dynamo.mjReadable() : null);
        event.registerBlockEntity(Capabilities.Energy.BLOCK, MJ_DYNAMO.get(),
            (dynamo, side) -> side == dynamo.outputDirection() ? dynamo.energy() : null);
        event.registerBlockEntity(Capabilities.Item.BLOCK, MJ_DYNAMO.get(),
            (dynamo, side) -> dynamo.upgrades());
    }
}
