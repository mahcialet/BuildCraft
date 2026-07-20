package buildcraft.silicon.client.screen;
import buildcraft.builders.BuildersGateActions;

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
    private final Button[][] parameterButtons = new Button[8][3];
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
                    click(index * 2)).bounds(leftPos + 8, topPos + 20 + row * 12, 52, 11).build());
            actionButtons[row] = addRenderableWidget(Button.builder(Component.literal("→"), button ->
                    click(16 + index)).bounds(leftPos + 62, topPos + 20 + row * 12, 48, 11).build());
            for (int parameter = 0; parameter < 3; parameter++) {
                int parameterIndex = parameter;
                parameterButtons[row][parameter] = addRenderableWidget(Button.builder(
                        Component.literal(Integer.toString(parameter + 1)), button ->
                                click(24 + index * 3 + parameterIndex))
                        .bounds(leftPos + 112 + parameter * 12, topPos + 20 + row * 12, 11, 11).build());
            }
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
            for (Button parameter : parameterButtons[row]) parameter.visible = visible;
            clearButtons[row].visible = visible;
            if (!visible) continue;
            GateTrigger trigger = menu.trigger(row);
            GateAction action = menu.action(row);
            triggerButtons[row].setMessage(Component.literal(trigger == null ? "+" : trigger.getSerializedName()));
            actionButtons[row].setMessage(Component.literal(action == null ? "→" : action.getSerializedName()));
            actionButtons[row].active = trigger != null;
            int optionCount = action == null ? 0 : BuildersGateActions.optionCount(action);
            boolean fillerAction = action != null && BuildersGateActions.pattern(action) != null;
            var options = java.util.List.of(menu.option(row, 0), menu.option(row, 1), menu.option(row, 2));
            for (int parameter = 0; parameter < parameterButtons[row].length; parameter++) {
                Button button = parameterButtons[row][parameter];
                button.active = trigger != null;
                button.visible = visible && (!fillerAction || parameter < optionCount);
                button.setMessage(Component.literal(parameter < optionCount
                        ? BuildersGateActions.optionLabel(action, options, parameter)
                        : Integer.toString(parameter + 1)));
            }
            clearButtons[row].active = trigger != null;
        }
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.outline(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202020);
    }
}
