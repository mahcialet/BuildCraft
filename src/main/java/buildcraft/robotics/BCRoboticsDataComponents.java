package buildcraft.robotics;

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

    private BCRoboticsDataComponents() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
