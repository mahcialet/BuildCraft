package buildcraft.transport.recipe;

import buildcraft.core.BCCoreItems;
import buildcraft.transport.BCTransportItems;
import buildcraft.transport.BCTransportRecipes;
import buildcraft.transport.PipeType;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StainedGlassBlock;

/** Historical pipe recipe variants using stained glass; the glass colour becomes the pipe shell colour. */
public final class ColoredPipeRecipe extends CustomRecipe {
    public static final ColoredPipeRecipe INSTANCE = new ColoredPipeRecipe();
    public static final MapCodec<ColoredPipeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, ColoredPipeRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    private static final TagKey<Item> SANDSTONE = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath("buildcrafttransport", "sandstone"));

    @Override public boolean matches(CraftingInput input, Level level) { return !result(input).isEmpty(); }
    @Override public ItemStack assemble(CraftingInput input) { return result(input); }

    private static ItemStack result(CraftingInput input) {
        for (int row = 0; row < input.height(); row++) {
            for (int column = 0; column + 2 < input.width(); column++) {
                ItemStack left = input.getItem(column, row);
                ItemStack glass = input.getItem(column + 1, row);
                ItemStack right = input.getItem(column + 2, row);
                if (left.isEmpty() || glass.isEmpty() || right.isEmpty()) continue;
                if (nonEmpty(input) != 3) return ItemStack.EMPTY;
                if (!(glass.getItem() instanceof BlockItem blockItem)
                        || !(blockItem.getBlock() instanceof StainedGlassBlock stainedGlass)) return ItemStack.EMPTY;
                DyeColor color = stainedGlass.getColor();
                PipeType type = type(left, right);
                if (type == null) return ItemStack.EMPTY;
                ItemStack result = BCTransportItems.pipeItem(type).createStack(color);
                result.setCount(8);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int nonEmpty(CraftingInput input) {
        int count = 0;
        for (int slot = 0; slot < input.size(); slot++) if (!input.getItem(slot).isEmpty()) count++;
        return count;
    }

    private static PipeType type(ItemStack left, ItemStack right) {
        if (left.is(Items.COBBLESTONE) && right.is(Items.COBBLESTONE)) return PipeType.COBBLESTONE_ITEM;
        if (left.is(Items.STONE) && right.is(Items.STONE)) return PipeType.STONE_ITEM;
        if (left.is(Items.QUARTZ_BLOCK) && right.is(Items.QUARTZ_BLOCK)) return PipeType.QUARTZ_ITEM;
        if (left.is(ItemTags.PLANKS) && right.is(ItemTags.PLANKS)) return PipeType.WOOD_ITEM;
        if (left.is(Items.GOLD_INGOT) && right.is(Items.GOLD_INGOT)) return PipeType.GOLD_ITEM;
        if (left.is(Items.IRON_INGOT) && right.is(Items.IRON_INGOT)) return PipeType.IRON_ITEM;
        if (left.is(Items.CLAY) && right.is(Items.CLAY)) return PipeType.CLAY_ITEM;
        if (left.is(SANDSTONE) && right.is(SANDSTONE)) return PipeType.SANDSTONE_ITEM;
        if (left.is(Items.OBSIDIAN) && right.is(Items.OBSIDIAN)) return PipeType.OBSIDIAN_ITEM;
        if (left.is(Items.LAPIS_BLOCK) && right.is(Items.LAPIS_BLOCK)) return PipeType.LAPIS_ITEM;
        if (left.is(Items.LAPIS_BLOCK) && right.is(Items.DIAMOND)) return PipeType.DAIZULI_ITEM;
        if (left.is(ItemTags.PLANKS) && right.is(Items.DIAMOND)) return PipeType.DIAMOND_WOOD_ITEM;
        if (left.is(Items.DIAMOND) && right.is(Items.DIAMOND)) return PipeType.DIAMOND_ITEM;
        if (left.is(Items.BLACK_DYE) && right.is(Items.REDSTONE)) return PipeType.VOID_ITEM;
        if (left.is(BCCoreItems.GEAR_GOLD.get()) && right.is(BCCoreItems.GEAR_GOLD.get())) {
            return PipeType.STRIPES_ITEM;
        }
        return null;
    }

    @Override public RecipeSerializer<ColoredPipeRecipe> getSerializer() {
        return BCTransportRecipes.COLORED_PIPE.get();
    }
}
