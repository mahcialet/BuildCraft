package buildcraft.silicon.recipe;

import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconRecipes;
import buildcraft.transport.BCTransportItems;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Dynamic legacy Assembly Table recipe: three Structure Pipes plus one appearance block. */
public final class FacadeRecipe implements AssemblyTableRecipe {
    public static final FacadeRecipe INSTANCE = new FacadeRecipe();
    public static final MapCodec<FacadeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, FacadeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    private static final long REQUIRED_POWER = 64_000_000L;

    private static BlockItem facadeBlock(AssemblyRecipeInput input) {
        BlockItem result = null;
        int pipes = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(BCTransportItems.PIPE_STRUCTURE.get())) {
                pipes += stack.getCount();
            } else if (stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() != Blocks.AIR && result == null) {
                result = blockItem;
            } else {
                return null;
            }
        }
        return pipes == 3 ? result : null;
    }

    @Override public boolean matches(AssemblyRecipeInput input, Level level) { return facadeBlock(input) != null; }
    @Override public ItemStack result(AssemblyRecipeInput input) {
        BlockItem block = facadeBlock(input);
        if (block == null) return ItemStack.EMPTY;
        ItemStack result = BCSiliconItems.facade(block.getBlock().defaultBlockState());
        result.setCount(6);
        return result;
    }
    @Override public long requiredPower() { return REQUIRED_POWER; }
    @Override public List<SlotUse> findSlots(AssemblyRecipeInput input) {
        List<SlotUse> uses = new ArrayList<>();
        int remainingPipes = 3;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(BCTransportItems.PIPE_STRUCTURE.get()) && remainingPipes > 0) {
                int count = Math.min(remainingPipes, stack.getCount());
                uses.add(new SlotUse(slot, count));
                remainingPipes -= count;
            }
        }
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.isEmpty() && !stack.is(BCTransportItems.PIPE_STRUCTURE.get())
                    && stack.getItem() instanceof BlockItem) {
                uses.add(new SlotUse(slot, 1));
                break;
            }
        }
        return remainingPipes == 0 ? List.copyOf(uses) : List.of();
    }
    @Override public RecipeSerializer<FacadeRecipe> getSerializer() { return BCSiliconRecipes.FACADE_SERIALIZER.get(); }
    @Override public RecipeType<? extends net.minecraft.world.item.crafting.Recipe<AssemblyRecipeInput>> getType() {
        return BCSiliconRecipes.ASSEMBLY_TYPE.get();
    }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return "buildcraftsilicon:assembly"; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
}
