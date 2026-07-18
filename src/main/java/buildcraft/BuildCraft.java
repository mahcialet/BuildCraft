package buildcraft;

import buildcraft.core.BCCoreItems;
import buildcraft.core.BCCreativeTabs;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.BCCoreGameTests;
import buildcraft.core.BCCoreBlockEntities;
import buildcraft.core.BCCoreDataComponents;
import buildcraft.core.BCCoreMenus;
import buildcraft.core.BCCoreNetwork;
import buildcraft.core.BCCoreVolumeBoxes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import buildcraft.core.client.BCCoreClient;

/** Entry point for the incremental NeoForge port. */
@Mod(BuildCraft.MOD_ID)
public final class BuildCraft {
    /** The historical core module namespace, retained for world compatibility. */
    public static final String MOD_ID = "buildcraftcore";

    public BuildCraft(IEventBus modBus) {
        BCCoreBlocks.register(modBus);
        BCCoreDataComponents.register(modBus);
        BCCoreBlockEntities.register(modBus);
        BCCoreMenus.register(modBus);
        BCCoreNetwork.register(modBus);
        BCCoreVolumeBoxes.register();
        BCCoreItems.register(modBus);
        BCCreativeTabs.register(modBus);
        BCCoreGameTests.register(modBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            BCCoreClient.register(modBus);
        }
    }
}
