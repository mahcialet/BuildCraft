package buildcraft.silicon.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public interface AssemblyTableRecipe extends Recipe<AssemblyRecipeInput> {
    long requiredPower();
    List<SlotUse> findSlots(AssemblyRecipeInput input);

    record SlotUse(int slot, int count) {}

    @Override default ItemStack assemble(AssemblyRecipeInput input) {
        return result(input);
    }

    ItemStack result(AssemblyRecipeInput input);
}
