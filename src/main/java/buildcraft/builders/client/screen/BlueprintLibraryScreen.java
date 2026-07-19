package buildcraft.builders.client.screen;

import buildcraft.builders.block.entity.BlueprintLibraryBlockEntity;
import buildcraft.builders.menu.BlueprintLibraryMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BlueprintLibraryScreen extends AbstractContainerScreen<BlueprintLibraryMenu> {
    private Button previous;
    private Button next;
    private Button delete;
    public BlueprintLibraryScreen(BlueprintLibraryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
        titleLabelX = 8; inventoryLabelY = 92;
    }
    @Override protected void init() {
        super.init();
        previous = addRenderableWidget(Button.builder(Component.literal("<"),
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + 36, topPos + 62, 24, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"),
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1))
                .bounds(leftPos + 116, topPos + 62, 24, 18).build());
        delete = addRenderableWidget(Button.builder(Component.literal("X"),
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2))
                .bounds(leftPos + 145, topPos + 62, 18, 18).build());
    }
    @Override protected void containerTick() {
        super.containerTick();
        boolean hasEntries = menu.entryCount() > 0;
        previous.active = hasEntries; next.active = hasEntries; delete.active = hasEntries;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        slot(graphics, 35, 24); slot(graphics, 62, 24); slot(graphics, 114, 24); slot(graphics, 141, 24);
        int in = Math.min(22, menu.progressIn() * 22 / BlueprintLibraryBlockEntity.PROCESS_TIME);
        int out = Math.min(22, menu.progressOut() * 22 / BlueprintLibraryBlockEntity.PROCESS_TIME);
        graphics.fill(leftPos + 37, topPos + 48, leftPos + 37 + in, topPos + 51, 0xFF3A8F3A);
        graphics.fill(leftPos + 116, topPos + 48, leftPos + 116 + out, topPos + 51, 0xFF3A8F3A);
        String selection = menu.entryCount() == 0 ? "No entries"
                : (menu.selected() + 1) + "/" + menu.entryCount() + " " + menu.selectedName();
        graphics.text(font, selection, leftPos + 8, topPos + 65, 0xFF303030, false);
    }
    private void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF373737);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }
}
