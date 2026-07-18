package buildcraft.energy;

import buildcraft.api.mj.MjAPI;
import buildcraft.core.BCCoreBlocks;
import buildcraft.energy.block.entity.StirlingEngineBlockEntity;
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

    private BCEnergyBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(BCEnergyBlockEntities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(MjAPI.CAP_CONNECTOR, ENGINE_STIRLING.get(),
            (engine, side) -> engine.connector(side));
        event.registerBlockEntity(Capabilities.Item.BLOCK, ENGINE_STIRLING.get(),
            (engine, side) -> engine.fuelInventory());
    }
}
