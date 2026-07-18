package buildcraft.transport.client.screen;

import buildcraft.transport.menu.DiamondWoodMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class DiamondWoodScreen extends AbstractContainerScreen<DiamondWoodMenu> {
    private Button modeButton;

    public DiamondWoodScreen(DiamondWoodMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 144);
        titleLabelY = 6;
        inventoryLabelY = 50;
    }

    @Override
    protected void init() {
        super.init();
        modeButton = addRenderableWidget(Button.builder(modeText(), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + 8, topPos + 43, 160, 16).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        modeButton.setMessage(modeText());
    }

    private Component modeText() {
        return Component.translatable("gui.buildcrafttransport.diamond_wood.mode",
            Component.translatable("gui.buildcrafttransport.diamond_wood.mode." + menu.mode().getSerializedName()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF202020);
        for (int index = 0; index < DiamondWoodMenu.FILTERS; index++) {
            int x = leftPos + 8 + index * 18;
            graphics.fill(x - 1, topPos + 23, x + 17, topPos + 41, 0xFF373737);
            graphics.fill(x, topPos + 24, x + 16, topPos + 40, 0xFF8B8B8B);
        }
    }
}
