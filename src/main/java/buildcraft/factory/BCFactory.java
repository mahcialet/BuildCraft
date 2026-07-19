package buildcraft.factory;

import buildcraft.core.BCCreativeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import buildcraft.factory.client.BCFactoryClient;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(BCFactory.MOD_ID)
public final class BCFactory {
    public static final String MOD_ID = "buildcraftfactory";

    public BCFactory(IEventBus modBus) {
        BCFactoryBlocks.register(modBus);
        BCFactoryBlockEntities.register(modBus);
        BCFactoryItems.register(modBus);
        BCFactoryMenus.register(modBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) BCFactoryClient.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) {
            event.accept(BCFactoryItems.TANK.get());
            event.accept(BCFactoryItems.FLOOD_GATE.get());
            event.accept(BCFactoryItems.PUMP.get());
            event.accept(BCFactoryItems.MINING_WELL.get());
            event.accept(BCFactoryItems.CHUTE.get());
            event.accept(BCFactoryItems.DISTILLER.get());
            event.accept(BCFactoryItems.HEAT_EXCHANGER.get());
            event.accept(BCFactoryItems.WATER_GEL.get());
            event.accept(BCFactoryItems.GEL.get());
            event.accept(BCFactoryItems.AUTO_WORKBENCH.get());
        }
    }
}
