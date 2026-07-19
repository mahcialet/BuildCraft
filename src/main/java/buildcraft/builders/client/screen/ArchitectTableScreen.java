package buildcraft.builders.client.screen;

import buildcraft.builders.menu.ArchitectTableMenu;
import buildcraft.builders.network.ArchitectNamePayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ArchitectTableScreen extends AbstractContainerScreen<ArchitectTableMenu> {
    private Button rotate;
    private Button excavate;
    private Button allowCreative;
    private EditBox name;

    public ArchitectTableScreen(ArchitectTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 166);
        titleLabelX = 90;
        inventoryLabelX = 88;
        inventoryLabelY = 72;
    }
    @Override protected void init() {
        super.init();
        rotate = addRenderableWidget(Button.builder(Component.empty(), button ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + 5, topPos + 30, 79, 20).build());
        excavate = addRenderableWidget(Button.builder(Component.empty(), button ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1))
                .bounds(leftPos + 5, topPos + 54, 79, 20).build());
        allowCreative = addRenderableWidget(Button.builder(Component.empty(), button ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 2))
                .bounds(leftPos + 5, topPos + 78, 79, 20).build());
        name = new EditBox(font, leftPos + 90, topPos + 61, 156, 14,
                Component.translatable("gui.buildcraftbuilders.architect.name"));
        name.setMaxLength(ArchitectNamePayload.MAX_LENGTH);
        name.setValue(menu.blueprintName());
        name.setResponder(value -> ClientPacketDistributor.sendToServer(
                new ArchitectNamePayload(menu.containerId, value)));
        addRenderableWidget(name);
        updateButtons();
    }
    @Override protected void containerTick() {
        super.containerTick();
        updateButtons();
    }
    private void updateButtons() {
        rotate.setMessage(Component.translatable(menu.rotate()
                ? "gui.buildcraftbuilders.architect.rotate" : "gui.buildcraftbuilders.architect.no_rotate"));
        excavate.setMessage(Component.translatable(menu.excavate()
                ? "gui.buildcraftbuilders.architect.excavate" : "gui.buildcraftbuilders.architect.no_excavate"));
        allowCreative.setMessage(Component.translatable(menu.allowCreative()
                ? "gui.buildcraftbuilders.architect.allow_creative" : "gui.buildcraftbuilders.architect.no_allow_creative"));
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        slot(graphics, 135, 35); slot(graphics, 194, 35);
        graphics.fill(leftPos + 158, topPos + 41, leftPos + 184, topPos + 47, 0xFF555555);
        int width = menu.progressMaximum() <= 0 ? 0 : Math.min(26, menu.progress() * 26 / menu.progressMaximum());
        graphics.fill(leftPos + 158, topPos + 41, leftPos + 158 + width, topPos + 47, 0xFFFFFF00);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) slot(graphics, 88 + column * 18, 84 + row * 18);
        for (int column = 0; column < 9; column++) slot(graphics, 88 + column * 18, 142);
    }
    private void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF373737);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }
}
