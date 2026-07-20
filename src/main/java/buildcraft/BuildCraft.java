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
import buildcraft.core.BCCoreFeatures;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import buildcraft.core.client.BCCoreClient;
import buildcraft.core.block.entity.OwnedBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Entry point for the incremental NeoForge port. */
@Mod(BuildCraft.MOD_ID)
public final class BuildCraft {
    /** The historical core module namespace, retained for world compatibility. */
    public static final String MOD_ID = "buildcraftcore";

    public BuildCraft(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, buildcraft.core.BCCoreConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, buildcraft.core.BCCoreConfig.CLIENT_SPEC);
        buildcraft.api.mj.MjAPI.setRfStatus(buildcraft.core.BCCoreConfig.rfStatus());
        BCCoreBlocks.register(modBus);
        BCCoreFeatures.register(modBus);
        BCCoreDataComponents.register(modBus);
        BCCoreBlockEntities.register(modBus);
        BCCoreMenus.register(modBus);
        BCCoreNetwork.register(modBus);
        BCCoreVolumeBoxes.register();
        BCCoreItems.register(modBus);
        BCCreativeTabs.register(modBus);
        BCCoreGameTests.register(modBus);
        NeoForge.EVENT_BUS.addListener(BuildCraft::recordMachineOwner);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            BCCoreClient.register(modBus);
        }
    }

    private static void recordMachineOwner(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof LivingEntity placer
            && event.getLevel().getBlockEntity(event.getPos()) instanceof OwnedBlockEntity machine) {
            machine.setOwner(placer);
        }
    }
}
