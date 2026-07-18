package buildcraft.transport.client.screen;

import buildcraft.transport.menu.EmzuliMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class EmzuliScreen extends AbstractContainerScreen<EmzuliMenu> {
    private final Button[] colors = new Button[EmzuliMenu.PRESETS];

    public EmzuliScreen(EmzuliMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        int[] xs = { 49, 49, 106, 106 };
        int[] ys = { 19, 47, 19, 47 };
        for (int index = 0; index < colors.length; index++) {
            int preset = index;
            colors[index] = addRenderableWidget(Button.builder(colorText(index), button -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, preset);
                }
            }).bounds(leftPos + xs[index], topPos + ys[index], 20, 20).build());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int index = 0; index < colors.length; index++) colors[index].setMessage(colorText(index));
    }

    private Component colorText(int index) {
        Component text = menu.color(index).<Component>map(color -> Component.literal(color.getSerializedName().substring(0, 1).toUpperCase()))
            .orElse(Component.literal("-"));
        if (menu.current() == index) return text.copy().withStyle(ChatFormatting.GREEN);
        if (menu.active(index)) return text.copy().withStyle(ChatFormatting.YELLOW);
        return text;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.outline(leftPos, topPos, imageWidth, imageHeight, 0xFF202020);
        int[] xs = { 25, 25, 134, 134 };
        int[] ys = { 21, 49, 21, 49 };
        for (int index = 0; index < EmzuliMenu.PRESETS; index++) {
            graphics.fill(leftPos + xs[index] - 1, topPos + ys[index] - 1,
                leftPos + xs[index] + 17, topPos + ys[index] + 17, 0xFF373737);
        }
    }
}
