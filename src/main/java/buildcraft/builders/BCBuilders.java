package buildcraft.builders;

import buildcraft.builders.client.BCBuildersClient;
import buildcraft.core.BCCreativeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

@Mod(BCBuilders.MOD_ID)
public final class BCBuilders {
    public static final String MOD_ID = "buildcraftbuilders";
    public static final TicketController QUARRY_TICKETS = new TicketController(
            Identifier.fromNamespaceAndPath(MOD_ID, "quarry"), (level, helper) -> {
                for (var owner : helper.getBlockTickets().keySet()) {
                    if (!level.getBlockState(owner).is(BCBuildersBlocks.QUARRY.get())) helper.removeAllTickets(owner);
                }
            });

    public BCBuilders(IEventBus modBus) {
        BCBuildersBlocks.register(modBus);
        BCBuildersBlockEntities.register(modBus);
        BCBuildersItems.register(modBus);
        BCBuildersDataComponents.register(modBus);
        BCBuildersMenus.register(modBus);
        BCBuildersNetwork.register(modBus);
        BCBuildersFillerPlanners.register();
        modBus.addListener(BCBuilders::registerTicketControllers);
        if (FMLEnvironment.getDist() == Dist.CLIENT) BCBuildersClient.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
    }
    private static void registerTicketControllers(RegisterTicketControllersEvent event) {
        event.register(QUARRY_TICKETS);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(BCCreativeTabs.MAIN.getKey())) {
            event.accept(BCBuildersItems.FILLER.get());
            event.accept(BCBuildersItems.ARCHITECT_TABLE.get());
            event.accept(BCBuildersItems.BUILDER.get());
            event.accept(BCBuildersItems.REPLACER.get());
            event.accept(BCBuildersItems.QUARRY.get());
            event.accept(BCBuildersItems.FRAME.get());
            event.accept(BCBuildersItems.BLUEPRINT_LIBRARY.get());
            event.accept(BCBuildersItems.CONSTRUCTION_MARKER.get());
            event.accept(BCBuildersItems.BLUEPRINT.get());
            event.accept(BCBuildersItems.TEMPLATE.get());
            event.accept(BCBuildersItems.SINGLE_SCHEMATIC.get());
            event.accept(BCBuildersItems.FILLER_PLANNER.get());
        }
    }
}
