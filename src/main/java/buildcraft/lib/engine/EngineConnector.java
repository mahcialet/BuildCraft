package buildcraft.lib.engine;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.IMjRedstoneReceiver;

/** Output-side connector used by BuildCraft engines. */
public final class EngineConnector implements IMjConnector {
    private final boolean redstoneOnly;

    public EngineConnector(boolean redstoneOnly) {
        this.redstoneOnly = redstoneOnly;
    }

    @Override
    public boolean canConnect(IMjConnector other) {
        return other instanceof IMjReceiver receiver && receiver.canReceive()
            && (!redstoneOnly || other instanceof IMjRedstoneReceiver);
    }
}
