package buildcraft.robotics;

import buildcraft.robotics.block.RequesterBlock;
import buildcraft.robotics.block.ZonePlannerBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCRoboticsBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BCRobotics.MOD_ID);
    public static final DeferredBlock<RequesterBlock> REQUESTER =
            BLOCKS.registerBlock("requester", RequesterBlock::new);
    public static final DeferredBlock<ZonePlannerBlock> ZONE_PLANNER =
            BLOCKS.registerBlock("zone_planner", ZonePlannerBlock::new);
    private BCRoboticsBlocks() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
