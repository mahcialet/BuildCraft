package buildcraft.robotics.client.screen;

import buildcraft.robotics.BCRobotics;
import buildcraft.robotics.block.entity.ZonePlannerBlockEntity;
import buildcraft.robotics.menu.ZonePlannerMenu;
import buildcraft.robotics.network.ZoneEditPayload;
import buildcraft.robotics.network.ZoneNamePayload;
import buildcraft.robotics.zone.ZonePlan;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ZonePlannerScreen extends AbstractContainerScreen<ZonePlannerMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            BCRobotics.MOD_ID, "textures/gui/zone_planner.png");
    private static final int MAP_X = 8, MAP_Y = 9, MAP_WIDTH = 213, MAP_HEIGHT = 100;
    private EditBox name;
    private int centerX;
    private int centerZ;
    private int blocksPerPixel = 1;
    private BlockPos selectionStart;
    private BlockPos selectionEnd;
    private boolean panning;
    private double panMouseX;
    private double panMouseY;
    private int panCenterX;
    private int panCenterZ;

    public ZonePlannerScreen(ZonePlannerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 228);
        titleLabelY = -1000;
        inventoryLabelX = 88;
        inventoryLabelY = 134;
    }
    @Override protected void init() {
        super.init();
        name = new EditBox(font, leftPos + 91, topPos + 124, 128, 14,
                Component.translatable("gui.buildcraftrobotics.zone_planner.name"));
        name.setMaxLength(ZoneNamePayload.MAX_LENGTH);
        name.setValue(menu.mapName());
        name.setResponder(value -> ClientPacketDistributor.sendToServer(new ZoneNamePayload(menu.containerId, value)));
        addRenderableWidget(name);
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0,
                imageWidth, imageHeight, 256, 256);
        int[] preview = menu.preview();
        for (int row = 0; row < 8; row++) for (int column = 0; column < 10; column++) {
            int x0 = leftPos + MAP_X + column * MAP_WIDTH / 10;
            int x1 = leftPos + MAP_X + (column + 1) * MAP_WIDTH / 10;
            int y0 = topPos + MAP_Y + row * MAP_HEIGHT / 8;
            int y1 = topPos + MAP_Y + (row + 1) * MAP_HEIGHT / 8;
            int color = preview[row * 10 + column];
            graphics.fill(x0, y0, x1, y1, color == 0 ? 0xFF303030 : color);
        }
        DyeColor active = activeColor();
        if (active != null) drawLayer(graphics, menu.layer(active.getId()), active);
        else for (DyeColor color : DyeColor.values()) drawLayer(graphics, menu.layer(color.getId()), color);
        if (selectionStart != null && selectionEnd != null) {
            BlockPos min = new BlockPos(Math.min(selectionStart.getX(), selectionEnd.getX()), 0,
                    Math.min(selectionStart.getZ(), selectionEnd.getZ()));
            BlockPos max = new BlockPos(Math.max(selectionStart.getX(), selectionEnd.getX()), 0,
                    Math.max(selectionStart.getZ(), selectionEnd.getZ()));
            int x0 = screenX(min.getX()), x1 = screenX(max.getX() + 1);
            int y0 = screenY(min.getZ()), y1 = screenY(max.getZ() + 1);
            graphics.fill(x0, y0, x1, y0 + 1, 0xFFFFFFFF);
            graphics.fill(x0, y1 - 1, x1, y1, 0xFFFFFFFF);
            graphics.fill(x0, y0, x0 + 1, y1, 0xFFFFFFFF);
            graphics.fill(x1 - 1, y0, x1, y1, 0xFFFFFFFF);
        }
        int inWidth = menu.progressInput() * 28 / ZonePlannerBlockEntity.PROCESS_TIME;
        int outHeight = menu.progressOutput() * 28 / ZonePlannerBlockEntity.PROCESS_TIME;
        graphics.fill(leftPos + 44, topPos + 128, leftPos + 44 + inWidth, topPos + 137, 0xFF00AA00);
        graphics.fill(leftPos + 236, topPos + 73 - outHeight, leftPos + 245, topPos + 73, 0xFF00AA00);
    }
    private void drawLayer(GuiGraphicsExtractor graphics, ZonePlan layer, DyeColor color) {
        int argb = 0x88000000 | color.getTextureDiffuseColor() & 0xFFFFFF;
        for (int py = 0; py < MAP_HEIGHT; py++) for (int px = 0; px < MAP_WIDTH; px++) {
            int x = centerX + (px - MAP_WIDTH / 2) * blocksPerPixel;
            int z = centerZ + (py - MAP_HEIGHT / 2) * blocksPerPixel;
            if (layer.get(x, z)) graphics.fill(leftPos + MAP_X + px, topPos + MAP_Y + py,
                    leftPos + MAP_X + px + 1, topPos + MAP_Y + py + 1, argb);
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (!insideMap(event.x(), event.y())) return false;
        DyeColor color = activeColor();
        if (color != null && (event.button() == 0 || event.button() == 1)) {
            selectionStart = mapPosition(event.x(), event.y());
            selectionEnd = selectionStart;
            setDragging(true);
            return true;
        }
        if (color == null && event.button() == 0) {
            panning = true; panMouseX = event.x(); panMouseY = event.y();
            panCenterX = centerX; panCenterZ = centerZ; setDragging(true); return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (selectionStart != null) { selectionEnd = mapPosition(event.x(), event.y()); return true; }
        if (panning) {
            centerX = panCenterX - (int) ((event.x() - panMouseX) * blocksPerPixel);
            centerZ = panCenterZ - (int) ((event.y() - panMouseY) * blocksPerPixel);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (selectionStart != null) {
            DyeColor color = activeColor();
            if (color != null) ClientPacketDistributor.sendToServer(new ZoneEditPayload(menu.containerId,
                    color.getId(), selectionStart, selectionEnd == null ? selectionStart : selectionEnd,
                    event.button() == 0));
            selectionStart = null; selectionEnd = null; setDragging(false); return true;
        }
        if (panning) { panning = false; setDragging(false); return true; }
        return super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (insideMap(mouseX, mouseY) && scrollY != 0) {
            blocksPerPixel = Math.clamp(blocksPerPixel + (scrollY < 0 ? 1 : -1), 1, 16);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    private DyeColor activeColor() {
        return ZonePlannerBlockEntity.brushColor(menu.getCarried());
    }
    private boolean insideMap(double x, double y) {
        return x >= leftPos + MAP_X && x < leftPos + MAP_X + MAP_WIDTH
                && y >= topPos + MAP_Y && y < topPos + MAP_Y + MAP_HEIGHT;
    }
    private BlockPos mapPosition(double x, double y) {
        return new BlockPos(centerX + ((int) x - leftPos - MAP_X - MAP_WIDTH / 2) * blocksPerPixel,
                0, centerZ + ((int) y - topPos - MAP_Y - MAP_HEIGHT / 2) * blocksPerPixel);
    }
    private int screenX(int relativeX) {
        return leftPos + MAP_X + MAP_WIDTH / 2 + (relativeX - centerX) / blocksPerPixel;
    }
    private int screenY(int relativeZ) {
        return topPos + MAP_Y + MAP_HEIGHT / 2 + (relativeZ - centerZ) / blocksPerPixel;
    }
}
