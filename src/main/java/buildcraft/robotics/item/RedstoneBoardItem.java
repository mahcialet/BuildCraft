package buildcraft.robotics.item;

import buildcraft.robotics.BCRobotics;
import buildcraft.robotics.BCRoboticsDataComponents;
import buildcraft.robotics.RobotBoardType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class RedstoneBoardItem extends Item {
    public RedstoneBoardItem(Properties properties) {
        super(properties.component(BCRoboticsDataComponents.BOARD_TYPE.get(), RobotBoardType.EMPTY));
    }

    public static ItemStack apply(ItemStack stack, RobotBoardType type) {
        stack.set(BCRoboticsDataComponents.BOARD_TYPE.get(), type);
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(
                BCRobotics.MOD_ID, "redstone_board_" + type.colour()));
        return stack;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                          Consumer<Component> tooltip, TooltipFlag flag) {
        RobotBoardType type = stack.getOrDefault(BCRoboticsDataComponents.BOARD_TYPE.get(), RobotBoardType.EMPTY);
        tooltip.accept(Component.translatable("item.buildcraftrobotics.redstone_board." + type.getSerializedName()));
        if (type != RobotBoardType.EMPTY) tooltip.accept(Component.translatable(
                "item.buildcraftrobotics.redstone_board.integration_cost", type.integrationCost()));
    }
}
