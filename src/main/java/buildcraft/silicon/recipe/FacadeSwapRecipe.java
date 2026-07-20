package buildcraft.silicon.recipe;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Toggles a Facade between its connection-blocking solid and pass-through hollow forms. */
public final class FacadeSwapRecipe extends CustomRecipe {
    public static final FacadeSwapRecipe INSTANCE = new FacadeSwapRecipe();
    public static final MapCodec<FacadeSwapRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, FacadeSwapRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    private static ItemStack facade(CraftingInput input) {
        ItemStack facade = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!facade.isEmpty() || !stack.is(BCSiliconItems.PLUG_FACADE.get())) return ItemStack.EMPTY;
            facade = stack;
        }
        return facade;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !facade(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack source = facade(input);
        if (source.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = source.copyWithCount(1);
        boolean hollow = source.getOrDefault(BCSiliconDataComponents.FACADE_HOLLOW.get(), false);
        result.set(BCSiliconDataComponents.FACADE_HOLLOW.get(), !hollow);
        return result;
    }

    @Override
    public RecipeSerializer<FacadeSwapRecipe> getSerializer() {
        return BCSiliconRecipes.FACADE_SWAP_SERIALIZER.get();
    }
}
