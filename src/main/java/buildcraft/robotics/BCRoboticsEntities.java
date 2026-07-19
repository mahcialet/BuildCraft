package buildcraft.robotics;

import buildcraft.robotics.entity.RobotEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCRoboticsEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BCRobotics.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<RobotEntity>> ROBOT =
            ENTITIES.register("robot", () -> EntityType.Builder.of(RobotEntity::new, MobCategory.MISC)
                    .sized(0.75F, 0.75F)
                    .eyeHeight(0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(BCRobotics.MOD_ID, "robot"))));

    private BCRoboticsEntities() {}
    public static void register(IEventBus bus) { ENTITIES.register(bus); }
}
