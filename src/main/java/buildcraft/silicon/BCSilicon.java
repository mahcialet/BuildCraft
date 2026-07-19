package buildcraft.silicon;

import buildcraft.core.BCCreativeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import buildcraft.silicon.client.BCSiliconClient;

@Mod(BCSilicon.MOD_ID)
public final class BCSilicon {
    public static final String MOD_ID = "buildcraftsilicon";

    public BCSilicon(IEventBus modBus) {
        BCSiliconDataComponents.register(modBus);
        BCSiliconRecipes.register(modBus);
        BCSiliconBlocks.register(modBus);
        BCSiliconBlockEntities.register(modBus);
        BCSiliconItems.register(modBus);
        BCSiliconMenus.register(modBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) BCSiliconClient.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) return;
        event.accept(BCSiliconItems.LASER.get());
        event.accept(BCSiliconItems.ASSEMBLY_TABLE.get());
        event.accept(BCSiliconItems.ADVANCED_CRAFTING_TABLE.get());
        for (ChipsetType type : ChipsetType.values()) event.accept(BCSiliconItems.chipset(type));
    }
}
