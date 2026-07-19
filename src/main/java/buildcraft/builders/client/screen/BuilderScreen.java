package buildcraft.builders.client.screen;

import buildcraft.api.core.IControllable.ControlMode;
import buildcraft.builders.menu.BuilderMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BuilderScreen extends AbstractContainerScreen<BuilderMenu> {
    private final Button[] modes = new Button[ControlMode.values().length];
    private Button rotation;
    private Button excavate;
    public BuilderScreen(BuilderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 192);
        titleLabelX = 8; inventoryLabelY = 98;
    }
    @Override protected void init() {
        super.init();
        for (int i = 0; i < modes.length; i++) {
            int id = i;
            modes[i] = addRenderableWidget(Button.builder(Component.literal(ControlMode.values()[i].name().substring(0, 1)),
                    button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id))
                    .bounds(leftPos + 8 + i * 18, topPos + 90, 16, 14).build());
        }
        rotation = addRenderableWidget(Button.builder(Component.literal("0"),
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 10))
                .bounds(leftPos + 66, topPos + 90, 38, 14).build());
        excavate = addRenderableWidget(Button.builder(Component.literal("E"),
                button -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 11))
                .bounds(leftPos + 106, topPos + 90, 28, 14).build());
    }
    @Override protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < modes.length; i++) modes[i].active = menu.mode().ordinal() != i;
        rotation.setMessage(Component.literal(switch (menu.rotation()) {
            case NONE -> "0"; case CLOCKWISE_90 -> "90"; case CLOCKWISE_180 -> "180"; case COUNTERCLOCKWISE_90 -> "270";
        }));
        excavate.setMessage(Component.literal(menu.canExcavate() ? "EX" : "--"));
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        slot(graphics, 80, 14);
        for (int slot = 0; slot < 27; slot++) slot(graphics, 8 + slot % 9 * 18, 36 + slot / 9 * 18);
        int width = menu.progressMaximum() <= 0 ? 0 : Math.min(52, menu.progress() * 52 / menu.progressMaximum());
        graphics.fill(leftPos + 106, topPos + 18, leftPos + 158, topPos + 24, 0xFF555555);
        graphics.fill(leftPos + 106, topPos + 18, leftPos + 106 + width, topPos + 24, 0xFF00AA00);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) slot(graphics, 8 + column * 18, 110 + row * 18);
        for (int column = 0; column < 9; column++) slot(graphics, 8 + column * 18, 168);
    }
    private void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF373737);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }
}
