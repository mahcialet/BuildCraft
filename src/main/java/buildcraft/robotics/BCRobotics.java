package buildcraft.robotics;

import buildcraft.core.BCCreativeTabs;
import buildcraft.robotics.client.BCRoboticsClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(BCRobotics.MOD_ID)
public final class BCRobotics {
    public static final String MOD_ID = "buildcraftrobotics";

    public BCRobotics(IEventBus modBus) {
        BCRoboticsBlocks.register(modBus);
        BCRoboticsBlockEntities.register(modBus);
        BCRoboticsEntities.register(modBus);
        BCRoboticsDataComponents.register(modBus);
        BCRoboticsItems.register(modBus);
        BCRoboticsMenus.register(modBus);
        BCRoboticsNetwork.register(modBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) BCRoboticsClient.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) return;
        event.accept(BCRoboticsItems.REQUESTER.get());
        event.accept(BCRoboticsItems.ZONE_PLANNER.get());
        event.accept(BCRoboticsItems.ROBOT_STATION.get());
        event.accept(BCRoboticsItems.ROBOT.get());
        event.accept(BCRoboticsItems.ROBOT_GOGGLES.get());
        for (RobotBoardType type : RobotBoardType.values()) {
            if (type != RobotBoardType.EMPTY) {
                event.accept(buildcraft.robotics.item.RobotItem.create(type, 0));
                event.accept(buildcraft.robotics.item.RobotItem.create(type,
                        RobotItemData.MAX_ENERGY));
            }
        }
        event.accept(BCRoboticsItems.REDSTONE_BOARD.get());
        for (RobotBoardType type : RobotBoardType.values()) {
            if (type != RobotBoardType.EMPTY) event.accept(BCRoboticsItems.board(type));
        }
    }
}
