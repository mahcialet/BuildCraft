package buildcraft.energy.client;

import buildcraft.energy.BCEnergy;
import buildcraft.energy.BCEnergyFluids;
import buildcraft.energy.BCEnergyBlockEntities;
import buildcraft.core.client.render.EngineRenderer;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import buildcraft.energy.BCEnergyMenus;
import buildcraft.energy.client.screen.EngineScreen;

/** Client-only model registration for Energy fluids. */
public final class BCEnergyClient {
    private BCEnergyClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BCEnergyClient::registerFluidModels);
        modBus.addListener(BCEnergyClient::registerRenderers);
        modBus.addListener(BCEnergyClient::registerScreens);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BCEnergyBlockEntities.ENGINE_STIRLING.get(), EngineRenderer::new);
        event.registerBlockEntityRenderer(BCEnergyBlockEntities.ENGINE_COMBUSTION.get(), EngineRenderer::new);
        event.registerBlockEntityRenderer(BCEnergyBlockEntities.ENGINE_RF.get(), EngineRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BCEnergyMenus.ENGINE.get(), EngineScreen::new);
    }

    private static void registerFluidModels(RegisterFluidModelsEvent event) {
        for (var family : BCEnergyFluids.REFINERY_FLUIDS.values()) {
            for (var variant : family.variants()) {
                event.register(model(variant.textureName(), variant.tint()), variant.source(), variant.flowing());
            }
        }
    }

    private static FluidModel.Unbaked model(String name, int tint) {
        return new FluidModel.Unbaked(
            new Material(Identifier.fromNamespaceAndPath(BCEnergy.MOD_ID, "block/fluids/" + name + "_still")),
            new Material(Identifier.fromNamespaceAndPath(BCEnergy.MOD_ID, "block/fluids/" + name + "_flow")),
            null,
            (net.neoforged.neoforge.client.fluid.FluidTintSource) state -> tint
        );
    }
}
