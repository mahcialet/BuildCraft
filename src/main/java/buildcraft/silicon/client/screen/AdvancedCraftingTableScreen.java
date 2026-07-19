package buildcraft.silicon.client.screen;

import buildcraft.silicon.menu.AdvancedCraftingTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AdvancedCraftingTableScreen extends AbstractContainerScreen<AdvancedCraftingTableMenu> {
    public AdvancedCraftingTableScreen(AdvancedCraftingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 241);
        inventoryLabelY = 141;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF202020);
        slots(graphics, 33, 16, 3, 3, 0xFF5A7898);
        slots(graphics, 15, 85, 5, 3, 0xFF8B8B8B);
        slots(graphics, 109, 85, 3, 3, 0xFF6A986A);
        int height = menu.progressThousandths() * 70 / 1_000;
        graphics.fill(leftPos + 164, topPos + 7, leftPos + 168, topPos + 77, 0xFF303030);
        graphics.fill(leftPos + 164, topPos + 77 - height, leftPos + 168, topPos + 77, 0xFFE03030);
    }
    private void slots(GuiGraphicsExtractor graphics, int startX, int startY, int width, int height, int color) {
        for (int row = 0; row < height; row++) for (int column = 0; column < width; column++) {
            int x = leftPos + startX + column * 18;
            int y = topPos + startY + row * 18;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
            graphics.fill(x, y, x + 16, y + 16, color);
        }
    }
}
