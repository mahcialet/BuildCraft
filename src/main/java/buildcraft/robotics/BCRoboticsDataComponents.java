package buildcraft.robotics;

import buildcraft.robotics.zone.ZonePlan;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCRoboticsDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BCRobotics.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RobotBoardType>> BOARD_TYPE =
            COMPONENTS.register("board_type", () -> DataComponentType.<RobotBoardType>builder()
                    .persistent(RobotBoardType.CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ZonePlan>> ZONE_PLAN =
            COMPONENTS.register("zone_plan", () -> DataComponentType.<ZonePlan>builder()
                    .persistent(ZonePlan.CODEC).build());

    private BCRoboticsDataComponents() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
