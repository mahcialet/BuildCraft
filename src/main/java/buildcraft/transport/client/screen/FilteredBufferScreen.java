package buildcraft.transport.client.screen;

import buildcraft.transport.menu.FilteredBufferMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FilteredBufferScreen extends AbstractContainerScreen<FilteredBufferMenu> {
    public FilteredBufferScreen(FilteredBufferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
        inventoryLabelY = 74;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF202020);
        for (int slot = 0; slot < 9; slot++) {
            int x = leftPos + 7 + slot * 18;
            graphics.fill(x, topPos + 17, x + 18, topPos + 35, 0xFF606060);
            graphics.fill(x, topPos + 53, x + 18, topPos + 71, 0xFF373737);
        }
    }
}
