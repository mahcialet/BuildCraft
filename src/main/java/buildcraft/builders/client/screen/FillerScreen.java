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
    private Button verticalButton;
    private Button horizontalButton;
    private Button hollowButton;
    private Button sphereFacingButton;
    private Button sphereRotationButton;
    private Button shapeAxisButton;
    private Button shapeRotationButton;
    public FillerScreen(FillerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
        titleLabelX = 8;
        titleLabelY = -100;
        inventoryLabelX = 8;
        inventoryLabelY = -100;
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
                    }).bounds(leftPos + 116 + mode * 18, topPos + 70, 16, 14).build());
        }
        for (int pattern = 0; pattern < FillerPattern.values().length; pattern++) {
            int id = pattern;
            patternButtons[pattern] = addRenderableWidget(Button.builder(
                    Component.literal(patternLabel(FillerPattern.values()[pattern])), button -> {
                        if (minecraft != null && minecraft.gameMode != null) {
                            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 10 + id);
                        }
            }).bounds(leftPos + 8 + pattern % 10 * 16, topPos + 2 + pattern / 10 * 15, 15, 14).build());
        }
        verticalButton = addRenderableWidget(Button.builder(Component.literal("U"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 30);
            }
        }).bounds(leftPos + 8, topPos + 70, 20, 14).build());
        horizontalButton = addRenderableWidget(Button.builder(Component.literal("E"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 31);
            }
        }).bounds(leftPos + 30, topPos + 70, 20, 14).build());
        hollowButton = addRenderableWidget(Button.builder(Component.literal("O"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 32);
            }
        }).bounds(leftPos + 52, topPos + 70, 20, 14).build());
        sphereFacingButton = addRenderableWidget(Button.builder(Component.literal("D"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 33);
            }
        }).bounds(leftPos + 74, topPos + 70, 20, 14).build());
        sphereRotationButton = addRenderableWidget(Button.builder(Component.literal("0"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 34);
            }
        }).bounds(leftPos + 96, topPos + 70, 18, 14).build());
        shapeAxisButton = addRenderableWidget(Button.builder(Component.literal("Y"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 35);
            }
        }).bounds(leftPos + 74, topPos + 70, 20, 14).build());
        shapeRotationButton = addRenderableWidget(Button.builder(Component.literal("0"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 36);
            }
        }).bounds(leftPos + 96, topPos + 70, 18, 14).build());
    }

    private static String patternLabel(FillerPattern pattern) {
        return switch (pattern) {
            case NONE -> "N";
            case CLEAR -> "C";
            case FILL -> "F";
            case BOX -> "B";
            case FRAME -> "R";
            case PYRAMID -> "P";
            case STAIRS -> "S";
            case SPHERE -> "O";
            case HEMISPHERE -> "H";
            case QUARTER_SPHERE -> "Q";
            case EIGHTH_SPHERE -> "E";
            case ARC -> "a";
            case CIRCLE -> "c";
            case HEXAGON -> "h";
            case OCTAGON -> "o";
            case PENTAGON -> "p";
            case SEMICIRCLE -> "d";
            case SQUARE -> "q";
            case TRIANGLE -> "t";
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
        boolean advanced = menu.pattern() == FillerPattern.PYRAMID || menu.pattern() == FillerPattern.STAIRS;
        verticalButton.visible = advanced;
        verticalButton.setMessage(Component.literal(menu.verticalDirection() == net.minecraft.core.Direction.UP
                ? "U" : "D"));
        horizontalButton.visible = menu.pattern() == FillerPattern.STAIRS;
        horizontalButton.setMessage(Component.literal(
                menu.horizontalDirection().getName().substring(0, 1).toUpperCase(java.util.Locale.ROOT)));
        boolean sphere = menu.pattern().isSphere();
        hollowButton.visible = sphere;
        hollowButton.setMessage(Component.literal(menu.hollow() ? "H" : "O"));
        sphereFacingButton.visible = menu.pattern().openFaces() > 0;
        sphereFacingButton.setMessage(Component.literal(
                menu.sphereFacing().getName().substring(0, 1).toUpperCase(java.util.Locale.ROOT)));
        sphereRotationButton.visible = menu.pattern().openFaces() > 1;
        sphereRotationButton.setMessage(Component.literal(Integer.toString(menu.sphereRotation())));
        boolean shape2d = menu.pattern().isShape2d();
        hollowButton.visible |= shape2d;
        shapeAxisButton.visible = shape2d;
        shapeAxisButton.setMessage(Component.literal(menu.shapeAxis().getName().toUpperCase(java.util.Locale.ROOT)));
        shapeRotationButton.visible = shape2d;
        shapeRotationButton.setMessage(Component.literal(Integer.toString(menu.shapeRotation())));
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
