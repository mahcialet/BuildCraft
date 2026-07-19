package buildcraft.builders.client.screen;

import buildcraft.builders.menu.ArchitectTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ArchitectTableScreen extends AbstractContainerScreen<ArchitectTableMenu> {
    public ArchitectTableScreen(ArchitectTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
        titleLabelX = 8;
        inventoryLabelY = 74;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        slot(graphics, 53, 35); slot(graphics, 107, 35);
        graphics.fill(leftPos + 75, topPos + 41, leftPos + 101, topPos + 47, 0xFF555555);
        int width = menu.progressMaximum() <= 0 ? 0 : Math.min(26, menu.progress() * 26 / menu.progressMaximum());
        graphics.fill(leftPos + 75, topPos + 41, leftPos + 75 + width, topPos + 47, 0xFFFFFF00);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) slot(graphics, 8 + column * 18, 86 + row * 18);
        for (int column = 0; column < 9; column++) slot(graphics, 8 + column * 18, 144);
    }
    private void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF373737);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }
}
