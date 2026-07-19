package buildcraft.silicon.client.screen;

import buildcraft.silicon.ChipsetType;
import buildcraft.silicon.menu.AssemblyTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AssemblyTableScreen extends AbstractContainerScreen<AssemblyTableMenu> {
    private final Button[] recipeButtons = new Button[ChipsetType.values().length];

    public AssemblyTableScreen(AssemblyTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 180);
        inventoryLabelY = 86;
    }

    @Override protected void init() {
        super.init();
        for (ChipsetType type : ChipsetType.values()) {
            int id = type.ordinal();
            recipeButtons[id] = addRenderableWidget(Button.builder(
                Component.literal(type.getSerializedName().substring(0, 1).toUpperCase()), button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                    }
                }).bounds(leftPos + 84 + id % 3 * 28, topPos + 20 + id / 3 * 22, 26, 20).build());
        }
    }

    @Override protected void containerTick() {
        super.containerTick();
        for (ChipsetType type : ChipsetType.values()) {
            recipeButtons[type.ordinal()].active = menu.selectedType() != type;
        }
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF202020);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 4; column++) {
            int x = leftPos + 8 + column * 18;
            int y = topPos + 18 + row * 18;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
            graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
        }
        int progress = menu.progressThousandths() * 72 / 1_000;
        graphics.fill(leftPos + 84, topPos + 67, leftPos + 156, topPos + 75, 0xFF303030);
        graphics.fill(leftPos + 84, topPos + 67, leftPos + 84 + progress, topPos + 75, 0xFFE03030);
    }
}
