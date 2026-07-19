package buildcraft.silicon;

import buildcraft.core.BCCreativeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(BCSilicon.MOD_ID)
public final class BCSilicon {
    public static final String MOD_ID = "buildcraftsilicon";

    public BCSilicon(IEventBus modBus) {
        BCSiliconDataComponents.register(modBus);
        BCSiliconItems.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) return;
        for (ChipsetType type : ChipsetType.values()) event.accept(BCSiliconItems.chipset(type));
    }
}
