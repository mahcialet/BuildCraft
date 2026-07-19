package buildcraft.robotics;

import buildcraft.robotics.item.RedstoneBoardItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCRoboticsItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BCRobotics.MOD_ID);
    public static final DeferredItem<RedstoneBoardItem> REDSTONE_BOARD =
            ITEMS.registerItem("redstone_board", RedstoneBoardItem::new);
    public static final DeferredItem<?> REQUESTER =
            ITEMS.registerSimpleBlockItem("requester", BCRoboticsBlocks.REQUESTER);

    private BCRoboticsItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
    public static ItemStack board(RobotBoardType type) {
        return RedstoneBoardItem.apply(new ItemStack(REDSTONE_BOARD.get()), type);
    }
}
