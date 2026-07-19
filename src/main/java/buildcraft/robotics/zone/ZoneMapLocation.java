package buildcraft.robotics.zone;

import buildcraft.api.items.MapLocationData;
import buildcraft.api.items.MapLocationType;
import buildcraft.core.BCCoreDataComponents;
import buildcraft.robotics.BCRoboticsDataComponents;
import net.minecraft.world.item.ItemStack;

public final class ZoneMapLocation {
    private ZoneMapLocation() {}
    public static void set(ItemStack stack, ZonePlan plan, String name) {
        stack.set(BCCoreDataComponents.MAP_LOCATION_TYPE.get(), MapLocationType.ZONE);
        stack.set(BCCoreDataComponents.MAP_LOCATION.get(), MapLocationData.clean().withName(name));
        stack.set(BCRoboticsDataComponents.ZONE_PLAN.get(), new ZonePlan(plan));
    }
    public static ZonePlan get(ItemStack stack) {
        ZonePlan plan = stack.get(BCRoboticsDataComponents.ZONE_PLAN.get());
        return plan == null ? null : new ZonePlan(plan);
    }
}
