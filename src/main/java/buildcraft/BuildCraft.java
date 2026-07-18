package buildcraft;

import buildcraft.core.BCCoreItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/** Entry point for the incremental NeoForge port. */
@Mod(BuildCraft.MOD_ID)
public final class BuildCraft {
    /** The historical core module namespace, retained for world compatibility. */
    public static final String MOD_ID = "buildcraftcore";

    public BuildCraft(IEventBus modBus) {
        BCCoreItems.register(modBus);
    }
}
