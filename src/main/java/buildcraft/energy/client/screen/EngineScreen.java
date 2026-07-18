package buildcraft.energy.client.screen;

import buildcraft.energy.menu.EngineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.util.Mth;

/** Compact synchronized status screen shared by the migrated Energy engines. */
public final class EngineScreen extends AbstractContainerScreen<EngineMenu> {
    public EngineScreen(EngineMenu menu, Inventory inventory, Component title) {
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
        graphics.fill(leftPos + 4, topPos + 18, leftPos + 172, topPos + 70, 0xFF8B8B8B);
        graphics.outline(leftPos + 4, topPos + 18, 168, 52, 0xFF373737);

        switch (menu.kind()) {
            case STIRLING -> {
                slot(graphics, 80, 35);
                bar(graphics, 24, 27, 12, 34, menu.burnTime(), Math.max(1, menu.burnTotal()), 0xFFE07020);
            }
            case COMBUSTION -> {
                bar(graphics, 22, 25, 12, 38, menu.fuelOrEnergy(), 10_000, 0xFFD68020);
                bar(graphics, 42, 25, 12, 38, menu.secondary(), 10_000, 0xFF3060E0);
                bar(graphics, 62, 25, 12, 38, menu.residue(), 10_000, 0xFF8A308A);
                bar(graphics, 82, 25, 12, 38, menu.heatHundredths(), 25_000, 0xFFE03020);
            }
            case RF, DYNAMO -> {
                for (int slot = 0; slot < 4; slot++) slot(graphics, 53 + slot * 18, 35);
                bar(graphics, 22, 25, 12, 38, menu.fuelOrEnergy(), 10_000, 0xFFE02020);
            }
            case UNKNOWN -> {
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) slot(graphics, 8 + column * 18, 84 + row * 18);
        }
        for (int column = 0; column < 9; column++) slot(graphics, 8 + column * 18, 142);
    }

    private void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF373737);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }

    private void bar(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
        int value, int maximum, int color) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + width + 1, topPos + y + height + 1,
            0xFF202020);
        int filled = Mth.clamp((int) ((long) height * Math.max(0, value) / Math.max(1, maximum)), 0, height);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, 0xFF303030);
        graphics.fill(leftPos + x, topPos + y + height - filled, leftPos + x + width, topPos + y + height, color);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFF202020, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
        graphics.text(font, Component.translatable("gui.buildcraftenergy.engine.heat",
            menu.heatHundredths() / 100.0), 102, 24, 0xFF202020, false);
        graphics.text(font, Component.translatable("gui.buildcraftenergy.engine.power",
            menu.storedMjHundredths() / 100.0), 102, 36, 0xFF202020, false);
        graphics.text(font, Component.translatable("gui.buildcraftenergy.engine.output",
            menu.outputMjHundredths() / 100.0), 102, 48, 0xFF202020, false);
    }
}
