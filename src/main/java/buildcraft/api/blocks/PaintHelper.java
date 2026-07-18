package buildcraft.api.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** Dispatches custom paint hooks and the vanilla mappings supported by BuildCraft 8. */
public final class PaintHelper {
    private static final Map<Block, Family> VANILLA = new IdentityHashMap<>();

    static {
        register(Blocks.GLASS, coloredGlass());
        register(Blocks.GLASS_PANE, coloredGlassPanes());
        register(Blocks.TERRACOTTA, coloredTerracotta());
    }

    private PaintHelper() {
    }

    public static InteractionResult attemptPaint(Level level, BlockPos pos, BlockState state,
        Vec3 hitPos, Direction side, @Nullable DyeColor color) {
        if (state.getBlock() instanceof IPaintable paintable) {
            InteractionResult result = paintable.paint(level, pos, state, hitPos, side, color);
            if (result != InteractionResult.PASS) return result;
        }
        Family family = VANILLA.get(state.getBlock());
        if (family == null) return InteractionResult.PASS;
        Block target = color == null ? family.clear : family.colored.get(color);
        if (target == null || target == state.getBlock()) return InteractionResult.FAIL;
        if (!level.isClientSide()) level.setBlock(pos, target.withPropertiesOf(state), Block.UPDATE_ALL);
        return InteractionResult.SUCCESS;
    }

    private static void register(Block clear, EnumMap<DyeColor, Block> colors) {
        Family family = new Family(clear, colors);
        VANILLA.put(clear, family);
        colors.values().forEach(block -> VANILLA.put(block, family));
    }

    private static EnumMap<DyeColor, Block> coloredGlass() {
        return colors(Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS,
            Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS,
            Blocks.PINK_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS, Blocks.LIGHT_GRAY_STAINED_GLASS,
            Blocks.CYAN_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS,
            Blocks.BROWN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS, Blocks.RED_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS);
    }

    private static EnumMap<DyeColor, Block> coloredGlassPanes() {
        return colors(Blocks.WHITE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS_PANE,
            Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS_PANE,
            Blocks.PINK_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE,
            Blocks.CYAN_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS_PANE,
            Blocks.BROWN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS_PANE);
    }

    private static EnumMap<DyeColor, Block> coloredTerracotta() {
        return colors(Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA,
            Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA,
            Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA,
            Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
            Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA);
    }

    private static EnumMap<DyeColor, Block> colors(Block... blocks) {
        EnumMap<DyeColor, Block> map = new EnumMap<>(DyeColor.class);
        DyeColor[] colors = DyeColor.values();
        for (int i = 0; i < colors.length; i++) map.put(colors[i], blocks[i]);
        return map;
    }

    private record Family(Block clear, EnumMap<DyeColor, Block> colored) {
    }
}
