package buildcraft.core.client.screen;

import buildcraft.BuildCraft;
import buildcraft.api.lists.ListMatchMode;
import buildcraft.core.menu.ListMenu;
import buildcraft.core.network.ListLabelPayload;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Modern editor for the historical two-line BuildCraft filter list. */
public final class ListScreen extends AbstractContainerScreen<ListMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        BuildCraft.MOD_ID, "textures/gui/list_new.png"
    );
    private final Button[][] modeButtons = new Button[ListMenu.HEIGHT][3];
    private EditBox label;

    public ListScreen(ListMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 191);
        inventoryLabelY = 97;
        titleLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();
        label = new EditBox(font, leftPos + 10, topPos + 9, 156, 14, Component.translatable("gui.buildcraftcore.list.label"));
        label.setMaxLength(32);
        label.setBordered(false);
        label.setValue(menu.label());
        label.setResponder(value -> ClientPacketDistributor.sendToServer(new ListLabelPayload(menu.containerId, value)));
        addRenderableWidget(label);

        for (int line = 0; line < ListMenu.HEIGHT; line++) {
            for (int button = 0; button < 3; button++) {
                final int id = line * 3 + button;
                Button widget = Button.builder(Component.empty(), pressed -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
                    }
                }).bounds(leftPos + 137 + button * 11, topPos + 51 + line * 34, 11, 12)
                    .tooltip(Tooltip.create(Component.translatable(switch (button) {
                        case 0 -> "gui.buildcraftcore.list.precise";
                        case 1 -> "gui.buildcraftcore.list.type";
                        default -> "gui.buildcraftcore.list.material";
                    }))).build();
                modeButtons[line][button] = widget;
                addRenderableWidget(widget);
            }
        }
        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        for (int line = 0; line < ListMenu.HEIGHT; line++) {
            ListMatchMode mode = menu.mode(line);
            boolean[] active = {
                menu.precise(line),
                mode == ListMatchMode.TYPE || mode == ListMatchMode.CLASS,
                mode == ListMatchMode.MATERIAL || mode == ListMatchMode.CLASS
            };
            String[] names = { "N", "T", "M" };
            for (int button = 0; button < 3; button++) {
                Component text = Component.literal(names[button]);
                modeButtons[line][button].setMessage(active[button] ? text.copy().withStyle(ChatFormatting.GREEN) : text);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0,
            imageWidth, imageHeight, 256, 256);
    }
}
