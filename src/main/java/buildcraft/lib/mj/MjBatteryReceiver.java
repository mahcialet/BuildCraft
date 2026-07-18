package buildcraft.lib.mj;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReadable;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjBattery;

/** Receiver/readable adapter around a public MJ battery. */
public class MjBatteryReceiver implements IMjReceiver, IMjReadable {
    protected final MjBattery battery;

    public MjBatteryReceiver(MjBattery battery) {
        this.battery = battery;
    }

    @Override
    public boolean canConnect(IMjConnector other) {
        return true;
    }

    @Override
    public long getPowerRequested() {
        return Math.max(0, battery.getCapacity() - battery.getStored());
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        return battery.addPowerChecking(microJoules, simulate);
    }

    @Override
    public long getStored() {
        return battery.getStored();
    }

    @Override
    public long getCapacity() {
        return battery.getCapacity();
    }
}
