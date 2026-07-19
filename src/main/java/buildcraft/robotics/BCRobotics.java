package buildcraft.robotics;

import buildcraft.core.BCCreativeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(BCRobotics.MOD_ID)
public final class BCRobotics {
    public static final String MOD_ID = "buildcraftrobotics";

    public BCRobotics(IEventBus modBus) {
        BCRoboticsDataComponents.register(modBus);
        BCRoboticsItems.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) return;
        event.accept(BCRoboticsItems.REDSTONE_BOARD.get());
        for (RobotBoardType type : RobotBoardType.values()) {
            if (type != RobotBoardType.EMPTY) event.accept(BCRoboticsItems.board(type));
        }
    }
}
