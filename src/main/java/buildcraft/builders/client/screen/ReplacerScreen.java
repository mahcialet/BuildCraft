package buildcraft.builders.client.screen;

import buildcraft.builders.menu.ReplacerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ReplacerScreen extends AbstractContainerScreen<ReplacerMenu> {
    public ReplacerScreen(ReplacerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168); titleLabelX = 8; inventoryLabelY = 74;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        slot(graphics, 80, 20); slot(graphics, 53, 52); slot(graphics, 107, 52);
        graphics.fill(leftPos + 74, topPos + 56, leftPos + 102, topPos + 64, 0xFF555555);
        graphics.fill(leftPos + 82, topPos + 54, leftPos + 94, topPos + 66, 0xFFFFFF00);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) slot(graphics, 8 + column * 18, 86 + row * 18);
        for (int column = 0; column < 9; column++) slot(graphics, 8 + column * 18, 144);
    }
    private void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF373737);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }
}
