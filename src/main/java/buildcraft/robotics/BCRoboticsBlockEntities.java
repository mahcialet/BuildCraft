package buildcraft.robotics;

import buildcraft.robotics.block.entity.RequesterBlockEntity;
import buildcraft.robotics.block.entity.ZonePlannerBlockEntity;
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
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZonePlannerBlockEntity>> ZONE_PLANNER =
            BLOCK_ENTITIES.register("zone_planner", () -> new BlockEntityType<>(
                    ZonePlannerBlockEntity::new, BCRoboticsBlocks.ZONE_PLANNER.get()));
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
