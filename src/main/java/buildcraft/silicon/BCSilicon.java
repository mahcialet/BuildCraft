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
        event.accept(BCSiliconItems.INTEGRATION_TABLE.get());
        event.accept(BCSiliconItems.PLUG_PULSAR.get());
        event.accept(BCSiliconItems.PLUG_LIGHT_SENSOR.get());
        event.accept(BCSiliconItems.PLUG_TIMER.get());
        event.accept(BCSiliconItems.GATE_COPIER.get());
        for (ChipsetType type : ChipsetType.values()) event.accept(BCSiliconItems.chipset(type));
        for (var material : buildcraft.silicon.gate.GateMaterial.values()) {
            if (material == buildcraft.silicon.gate.GateMaterial.CLAY_BRICK) {
                event.accept(BCSiliconItems.gate(material, buildcraft.silicon.gate.GateLogic.AND,
                        buildcraft.silicon.gate.GateModifier.NO_MODIFIER));
            } else {
                for (var logic : buildcraft.silicon.gate.GateLogic.values()) {
                    for (var modifier : buildcraft.silicon.gate.GateModifier.values()) {
                        event.accept(BCSiliconItems.gate(material, logic, modifier));
                    }
                }
            }
        }
    }
}
