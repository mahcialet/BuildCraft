package buildcraft.lib.mj;

import buildcraft.api.mj.IMjRedstoneReceiver;
import buildcraft.api.mj.MjBattery;

/** Battery adapter that explicitly accepts redstone-engine power. */
public final class MjRedstoneBatteryReceiver extends MjBatteryReceiver implements IMjRedstoneReceiver {
    public MjRedstoneBatteryReceiver(MjBattery battery) {
        super(battery);
    }
}
