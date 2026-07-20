package buildcraft.transport;

import buildcraft.transport.recipe.PipeUpgradeRecipe;
import buildcraft.transport.recipe.ColoredPipeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCTransportRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BCTransport.MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PipeUpgradeRecipe>> PIPE_UPGRADE =
            SERIALIZERS.register("pipe_upgrade", () -> new RecipeSerializer<>(
                    PipeUpgradeRecipe.MAP_CODEC, PipeUpgradeRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColoredPipeRecipe>> COLORED_PIPE =
            SERIALIZERS.register("colored_pipe", () -> new RecipeSerializer<>(
                    ColoredPipeRecipe.MAP_CODEC, ColoredPipeRecipe.STREAM_CODEC));

    private BCTransportRecipes() {
    }

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
