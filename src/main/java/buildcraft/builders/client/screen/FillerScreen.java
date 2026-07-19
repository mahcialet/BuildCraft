package buildcraft.builders.client.screen;

import buildcraft.api.core.IControllable.ControlMode;
import buildcraft.builders.menu.FillerMenu;
import buildcraft.builders.FillerPattern;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FillerScreen extends AbstractContainerScreen<FillerMenu> {
    private final Button[] modeButtons = new Button[ControlMode.values().length];
    private final Button[] patternButtons = new Button[FillerPattern.values().length];
    public FillerScreen(FillerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
        titleLabelX = 8;
        titleLabelY = -100;
        inventoryLabelX = 8;
        inventoryLabelY = 75;
    }

    @Override protected void init() {
        super.init();
        for (int mode = 0; mode < ControlMode.values().length; mode++) {
            int id = mode;
            modeButtons[mode] = addRenderableWidget(Button.builder(
                    Component.literal(ControlMode.values()[mode].name().substring(0, 1)), button -> {
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                        }
                    }).bounds(leftPos + 116 + mode * 18, topPos + 2, 16, 14).build());
        }
        for (int pattern = 0; pattern < FillerPattern.values().length; pattern++) {
            int id = pattern;
            patternButtons[pattern] = addRenderableWidget(Button.builder(
                    Component.literal(patternLabel(FillerPattern.values()[pattern])), button -> {
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 10 + id);
                        }
                    }).bounds(leftPos + 8 + pattern * 22, topPos + 2, 20, 14).build());
        }
    }

    private static String patternLabel(FillerPattern pattern) {
        return switch (pattern) {
            case NONE -> "N";
            case CLEAR -> "C";
            case FILL -> "F";
            case BOX -> "B";
            case FRAME -> "R";
        };
    }

    @Override protected void containerTick() {
        super.containerTick();
        for (int mode = 0; mode < modeButtons.length; mode++) {
            modeButtons[mode].active = menu.mode().ordinal() != mode;
        }
        for (int pattern = 0; pattern < patternButtons.length; pattern++) {
            patternButtons[pattern].active = menu.pattern().ordinal() != pattern;
        }
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                            float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            slot(graphics, 8 + column * 18, 18 + row * 18);
            slot(graphics, 8 + column * 18, 86 + row * 18);
        }
        for (int column = 0; column < 9; column++) slot(graphics, 8 + column * 18, 144);
    }

    private void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF373737);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }
}
