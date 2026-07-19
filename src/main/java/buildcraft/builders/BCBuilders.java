package buildcraft.builders;

import buildcraft.builders.client.BCBuildersClient;
import buildcraft.core.BCCreativeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(BCBuilders.MOD_ID)
public final class BCBuilders {
    public static final String MOD_ID = "buildcraftbuilders";

    public BCBuilders(IEventBus modBus) {
        BCBuildersBlocks.register(modBus);
        BCBuildersBlockEntities.register(modBus);
        BCBuildersItems.register(modBus);
        BCBuildersDataComponents.register(modBus);
        BCBuildersMenus.register(modBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) BCBuildersClient.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) {
            event.accept(BCBuildersItems.FILLER.get());
            event.accept(BCBuildersItems.ARCHITECT_TABLE.get());
            event.accept(BCBuildersItems.BUILDER.get());
            event.accept(BCBuildersItems.REPLACER.get());
            event.accept(BCBuildersItems.QUARRY.get());
            event.accept(BCBuildersItems.FRAME.get());
            event.accept(BCBuildersItems.BLUEPRINT.get());
            event.accept(BCBuildersItems.TEMPLATE.get());
            event.accept(BCBuildersItems.SINGLE_SCHEMATIC.get());
        }
    }
}
