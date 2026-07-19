package buildcraft.silicon.recipe;

import buildcraft.silicon.BCSiliconRecipes;
import buildcraft.silicon.ChipsetType;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record AssemblyRecipe(Optional<ChipsetType> type, List<Ingredient> ingredients,
                             ItemStackTemplate output, long requiredPower) implements Recipe<AssemblyRecipeInput> {
    public static final MapCodec<AssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ChipsetType.CODEC.optionalFieldOf("chipset_type").forGetter(AssemblyRecipe::type),
            Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(AssemblyRecipe::ingredients),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(AssemblyRecipe::output),
            com.mojang.serialization.Codec.LONG.fieldOf("mj").forGetter(recipe -> recipe.requiredPower / 1_000_000L)
    ).apply(instance, (type, ingredients, result, mj) ->
            new AssemblyRecipe(type, ingredients, result, Math.multiplyExact(mj, 1_000_000L))));

    @Override public boolean matches(AssemblyRecipeInput input, Level level) {
        if (type.isPresent() && input.selectedType() != type.get()) return false;
        boolean[] used = new boolean[input.size()];
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int slot = 0; slot < input.size(); slot++) {
                if (!used[slot] && ingredient.test(input.getItem(slot))) {
                    used[slot] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
    public ItemStack result() { return output.create(); }
    @Override public ItemStack assemble(AssemblyRecipeInput input) { return result(); }
    @Override public RecipeSerializer<? extends Recipe<AssemblyRecipeInput>> getSerializer() {
        return BCSiliconRecipes.ASSEMBLY_SERIALIZER.get();
    }
    @Override public RecipeType<? extends Recipe<AssemblyRecipeInput>> getType() {
        return BCSiliconRecipes.ASSEMBLY_TYPE.get();
    }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.create(ingredients); }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return "buildcraftsilicon:assembly"; }
}
