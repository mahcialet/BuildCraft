package buildcraft.transport.client.screen;

import buildcraft.transport.BCTransport;
import buildcraft.core.BCCoreConfig;
import buildcraft.transport.menu.DiamondRouteMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class DiamondRouteScreen extends AbstractContainerScreen<DiamondRouteMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        BCTransport.MOD_ID, "textures/gui/filter.png"
    );
    private static final Identifier COLOR_BLIND_TEXTURE = Identifier.fromNamespaceAndPath(
        BCTransport.MOD_ID, "textures/gui/filter_cb.png"
    );

    public DiamondRouteScreen(DiamondRouteMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 175, 225);
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 128;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Identifier texture = BCCoreConfig.COLOR_BLIND_MODE.get() ? COLOR_BLIND_TEXTURE : TEXTURE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0, 0,
            imageWidth, imageHeight, 256, 256);
    }
}
