package buildcraft.silicon.recipe;

import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class FacadeRecipe extends CustomRecipe {
    public static final FacadeRecipe INSTANCE = new FacadeRecipe();
    public static final MapCodec<FacadeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, FacadeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private static BlockItem facadeBlock(CraftingInput input) {
        BlockItem result = null;
        int walls = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.is(Blocks.COBBLESTONE_WALL.asItem())) {
                walls++;
            } else if (stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() != Blocks.AIR && result == null) {
                result = blockItem;
            } else {
                return null;
            }
        }
        return walls == 3 ? result : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return facadeBlock(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        BlockItem block = facadeBlock(input);
        return block == null ? ItemStack.EMPTY : BCSiliconItems.facade(block.getBlock().defaultBlockState());
    }

    @Override
    public RecipeSerializer<FacadeRecipe> getSerializer() {
        return BCSiliconRecipes.FACADE_SERIALIZER.get();
    }
}
