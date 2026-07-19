package buildcraft.robotics.item;

import buildcraft.robotics.BCRoboticsDataComponents;
import buildcraft.robotics.BCRoboticsEntities;
import buildcraft.robotics.BCRoboticsItems;
import buildcraft.robotics.RobotBoardType;
import buildcraft.robotics.RobotItemData;
import buildcraft.robotics.RobotStationRegistry;
import buildcraft.robotics.entity.RobotEntity;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public final class RobotItem extends Item {
    public RobotItem(Properties properties) {
        super(properties.stacksTo(16)
                .component(BCRoboticsDataComponents.ROBOT.get(), RobotItemData.EMPTY)
                .component(BCRoboticsDataComponents.BOARD_TYPE.get(), RobotBoardType.EMPTY));
    }

    public static ItemStack create(RobotBoardType board, long energy) {
        ItemStack stack = new ItemStack(BCRoboticsItems.ROBOT.get());
        stack.set(BCRoboticsDataComponents.ROBOT.get(), new RobotItemData(board, energy));
        stack.set(BCRoboticsDataComponents.BOARD_TYPE.get(), board);
        if (board != RobotBoardType.EMPTY) stack.set(DataComponents.MAX_STACK_SIZE, 1);
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction side = context.getClickedFace();
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof PipeHolderBlockEntity holder)
                || !holder.attachment(side).is(BCRoboticsItems.ROBOT_STATION.get())) {
            return InteractionResult.PASS;
        }
        RobotItemData data = context.getItemInHand().getOrDefault(
                BCRoboticsDataComponents.ROBOT.get(), RobotItemData.EMPTY);
        if (data.board() == RobotBoardType.EMPTY) return InteractionResult.FAIL;
        if (!(context.getLevel() instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        RobotStationRegistry.Station station = RobotStationRegistry.touch(
                serverLevel, holder.getBlockPos(), side);
        RobotEntity robot = new RobotEntity(BCRoboticsEntities.ROBOT.get(), serverLevel);
        robot.setBoard(data.board());
        robot.setEnergy(data.energy());
        if (!station.reserve(robot.getUUID()) || !robot.dock(station)) return InteractionResult.FAIL;
        if (!serverLevel.addFreshEntity(robot)) {
            station.release(robot.getUUID());
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
