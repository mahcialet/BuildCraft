package buildcraft.transport.recipe;

import buildcraft.transport.BCTransportItems;
import buildcraft.transport.BCTransportRecipes;
import buildcraft.transport.PipeType;
import buildcraft.transport.item.PipeItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Preserves the optional shell colour through pipe upgrades and their historical undo recipes. */
public final class PipeUpgradeRecipe extends CustomRecipe {
    public static final PipeUpgradeRecipe INSTANCE = new PipeUpgradeRecipe();
    public static final MapCodec<PipeUpgradeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, PipeUpgradeRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !result(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return result(input);
    }

    private static ItemStack result(CraftingInput input) {
        ItemStack pipeStack = ItemStack.EMPTY;
        ItemStack extra = ItemStack.EMPTY;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof PipeItem) {
                if (!pipeStack.isEmpty()) return ItemStack.EMPTY;
                pipeStack = stack;
            } else {
                if (!extra.isEmpty()) return ItemStack.EMPTY;
                extra = stack;
            }
        }
        if (pipeStack.isEmpty()) return ItemStack.EMPTY;
        PipeType source = ((PipeItem) pipeStack.getItem()).pipeType();
        PipeType target;
        if (extra.isEmpty()) {
            target = source == PipeType.EMZULI_ITEM ? PipeType.DIAMOND_WOOD_ITEM : undo(source);
        } else if (extra.is(BCTransportItems.WATERPROOF.get())) {
            target = fluid(source);
        } else if (extra.is(Items.REDSTONE)) {
            target = source.carriesPower() ? rf(source) : power(source);
        } else if (extra.is(Items.LAPIS_BLOCK) && source == PipeType.DIAMOND_WOOD_ITEM) {
            target = PipeType.EMZULI_ITEM;
        } else {
            return ItemStack.EMPTY;
        }
        return target == null ? ItemStack.EMPTY
                : BCTransportItems.pipeItem(target).createStack(PipeItem.color(pipeStack));
    }

    private static PipeType undo(PipeType type) {
        if (type.carriesRf()) return switch (type) {
            case WOOD_RF -> PipeType.WOOD_POWER;
            case COBBLESTONE_RF -> PipeType.COBBLESTONE_POWER;
            case STONE_RF -> PipeType.STONE_POWER;
            default -> null;
        };
        if (type.carriesPower()) return byFamily(type, 2, 0);
        if (type.carriesFluids()) return byFamily(type, 1, 0);
        return null;
    }

    private static PipeType fluid(PipeType type) { return byFamily(type, 0, 1); }
    private static PipeType power(PipeType type) { return byFamily(type, 0, 2); }
    private static PipeType rf(PipeType type) {
        return switch (type) {
            case WOOD_POWER -> PipeType.WOOD_RF;
            case COBBLESTONE_POWER -> PipeType.COBBLESTONE_RF;
            case STONE_POWER -> PipeType.STONE_RF;
            default -> null;
        };
    }

    private static PipeType byFamily(PipeType type, int from, int to) {
        String name = type.name();
        String suffix = switch (from) { case 0 -> "_ITEM"; case 1 -> "_FLUID"; case 2 -> "_POWER"; default -> "_RF"; };
        if (!name.endsWith(suffix)) return null;
        String targetSuffix = switch (to) { case 0 -> "_ITEM"; case 1 -> "_FLUID"; case 2 -> "_POWER"; default -> "_RF"; };
        try {
            return PipeType.valueOf(name.substring(0, name.length() - suffix.length()) + targetSuffix);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public RecipeSerializer<PipeUpgradeRecipe> getSerializer() {
        return BCTransportRecipes.PIPE_UPGRADE.get();
    }
}
