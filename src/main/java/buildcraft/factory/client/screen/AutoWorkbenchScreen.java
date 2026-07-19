package buildcraft.factory.client.screen;

import buildcraft.factory.menu.AutoWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AutoWorkbenchScreen extends AbstractContainerScreen<AutoWorkbenchMenu> {
    public AutoWorkbenchScreen(AutoWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 72;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF202020);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
            slot(graphics, 26 + column * 18, 17 + row * 18, 0xFF5A7898);
            slot(graphics, 80 + column * 18, 17 + row * 18, 0xFF8B8B8B);
        }
        slot(graphics, 152, 35, 0xFF6A986A);
    }

    private void slot(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF373737);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, color);
    }
}
