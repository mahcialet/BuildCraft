package buildcraft.silicon.client.screen;

import buildcraft.silicon.menu.IntegrationTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class IntegrationTableScreen extends AbstractContainerScreen<IntegrationTableMenu> {
    public IntegrationTableScreen(IntegrationTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 191);
        inventoryLabelY = 97;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.outline(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202020);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
            int x = leftPos + 44 + column * 18;
            int y = topPos + 31 + row * 18;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
            graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        }
        graphics.fill(leftPos + 137, topPos + 48, leftPos + 155, topPos + 66, 0xFF373737);
        int progress = menu.progressThousandths() * 70 / 1_000;
        graphics.fill(leftPos + 164, topPos + 22, leftPos + 168, topPos + 92, 0xFF303030);
        graphics.fill(leftPos + 164, topPos + 92 - progress, leftPos + 168, topPos + 92, 0xFFE03030);
    }
}
