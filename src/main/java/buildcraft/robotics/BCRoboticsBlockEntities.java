package buildcraft.robotics;

import buildcraft.robotics.block.entity.RequesterBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCRoboticsBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BCRobotics.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RequesterBlockEntity>> REQUESTER =
            BLOCK_ENTITIES.register("requester", () -> new BlockEntityType<>(
                    RequesterBlockEntity::new, BCRoboticsBlocks.REQUESTER.get()));
    private BCRoboticsBlockEntities() {}
    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        bus.addListener(BCRoboticsBlockEntities::registerCapabilities);
    }
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, REQUESTER.get(),
                (requester, side) -> requester.inventory());
    }
}
