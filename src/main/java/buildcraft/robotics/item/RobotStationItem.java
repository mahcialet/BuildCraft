package buildcraft.robotics.item;

import buildcraft.robotics.BCRoboticsDataComponents;
import buildcraft.robotics.RobotStationData;
import buildcraft.robotics.RobotStationRegistry;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import buildcraft.transport.item.PipeAttachment;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RobotStationItem extends Item implements PipeAttachment {
    public RobotStationItem(Properties properties) {
        super(properties.stacksTo(16)
                .component(BCRoboticsDataComponents.ROBOT_STATION.get(), RobotStationData.AVAILABLE));
    }

    @Override
    public void tickAttachment(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {
        if (!(pipe.getLevel() instanceof ServerLevel level)) return;
        RobotStationData current = stack.getOrDefault(
                BCRoboticsDataComponents.ROBOT_STATION.get(), RobotStationData.AVAILABLE);
        RobotStationData updated = RobotStationRegistry.touch(level, pipe.getBlockPos(), side).data();
        if (!updated.equals(current)) {
            stack.set(BCRoboticsDataComponents.ROBOT_STATION.get(), updated);
            pipe.setAttachment(side, stack);
        }
    }
}
