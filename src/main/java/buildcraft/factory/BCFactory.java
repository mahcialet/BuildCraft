package buildcraft.factory;

import buildcraft.core.BCCreativeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(BCFactory.MOD_ID)
public final class BCFactory {
    public static final String MOD_ID = "buildcraftfactory";

    public BCFactory(IEventBus modBus) {
        BCFactoryBlocks.register(modBus);
        BCFactoryBlockEntities.register(modBus);
        BCFactoryItems.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) {
            event.accept(BCFactoryItems.TANK.get());
        }
    }
}
