package buildcraft.silicon.recipe;

import buildcraft.silicon.ChipsetType;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record AssemblyRecipeInput(List<ItemStack> items, ChipsetType selectedType) implements RecipeInput {
    @Override public ItemStack getItem(int index) { return items.get(index); }
    @Override public int size() { return items.size(); }
}
