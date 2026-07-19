package buildcraft.silicon.recipe;

import buildcraft.api.mj.MjAPI;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.ChipsetType;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public record AssemblyRecipe(List<Ingredient> ingredients, ItemStack output, long requiredPower) {
    public static List<AssemblyRecipe> chipsetRecipes() {
        return List.of(
            recipe(ChipsetType.RED, 10_000, Ingredient.of(Items.REDSTONE)),
            recipe(ChipsetType.IRON, 20_000, Ingredient.of(Items.REDSTONE), Ingredient.of(Items.IRON_INGOT)),
            recipe(ChipsetType.GOLD, 40_000, Ingredient.of(Items.REDSTONE), Ingredient.of(Items.GOLD_INGOT)),
            recipe(ChipsetType.QUARTZ, 60_000, Ingredient.of(Items.REDSTONE), Ingredient.of(Items.QUARTZ)),
            recipe(ChipsetType.DIAMOND, 80_000, Ingredient.of(Items.REDSTONE), Ingredient.of(Items.DIAMOND))
        );
    }

    private static AssemblyRecipe recipe(ChipsetType type, long mj, Ingredient... ingredients) {
        return new AssemblyRecipe(List.of(ingredients), BCSiliconItems.chipset(type), mj * MjAPI.MJ);
    }
}
