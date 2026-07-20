package buildcraft.silicon.client.screen;

import buildcraft.silicon.gate.GateTrigger;
import buildcraft.silicon.gate.GateAction;
import buildcraft.silicon.menu.GateMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class GateScreen extends AbstractContainerScreen<GateMenu> {
    private final Button[] triggerButtons = new Button[8];
    private final Button[] actionButtons = new Button[8];
    private final Button[] clearButtons = new Button[8];
    public GateScreen(GateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        inventoryLabelY = 116;
    }
    @Override protected void init() {
        super.init();
        for (int row = 0; row < 8; row++) {
            int index = row;
            triggerButtons[row] = addRenderableWidget(Button.builder(Component.literal("+"), button ->
                    click(index * 2)).bounds(leftPos + 12, topPos + 20 + row * 12, 68, 11).build());
            actionButtons[row] = addRenderableWidget(Button.builder(Component.literal("→"), button ->
                    click(16 + index)).bounds(leftPos + 82, topPos + 20 + row * 12, 62, 11).build());
            clearButtons[row] = addRenderableWidget(Button.builder(Component.literal("×"), button ->
                    click(index * 2 + 1)).bounds(leftPos + 148, topPos + 20 + row * 12, 16, 11).build());
        }
    }
    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    @Override protected void containerTick() {
        super.containerTick();
        for (int row = 0; row < 8; row++) {
            boolean visible = row < menu.ruleSlots();
            triggerButtons[row].visible = visible;
            actionButtons[row].visible = visible;
            clearButtons[row].visible = visible;
            if (!visible) continue;
            GateTrigger trigger = menu.trigger(row);
            GateAction action = menu.action(row);
            triggerButtons[row].setMessage(Component.literal(trigger == null ? "+" : trigger.getSerializedName()));
            actionButtons[row].setMessage(Component.literal(action == null ? "→" : action.getSerializedName()));
            actionButtons[row].active = trigger != null;
            clearButtons[row].active = trigger != null;
        }
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.outline(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202020);
    }
}
