package buildcraft.robotics.client.screen;

import buildcraft.robotics.BCRobotics;
import buildcraft.robotics.menu.RequesterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class RequesterScreen extends AbstractContainerScreen<RequesterMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            BCRobotics.MOD_ID, "textures/gui/requester_gui.png");
    public RequesterScreen(RequesterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 196, 181);
        titleLabelY = -1000;
        inventoryLabelX = 19;
        inventoryLabelY = 89;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0,
                imageWidth, imageHeight, 256, 256);
    }
}
