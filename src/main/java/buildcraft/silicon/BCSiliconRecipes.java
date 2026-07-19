package buildcraft.silicon;

import buildcraft.silicon.recipe.AssemblyRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCSiliconRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, BCSilicon.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, BCSilicon.MOD_ID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblyRecipe>> ASSEMBLY_TYPE =
        TYPES.register("assembly", () -> RecipeType.simple(Identifier.fromNamespaceAndPath(BCSilicon.MOD_ID, "assembly")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblyRecipe>> ASSEMBLY_SERIALIZER =
        SERIALIZERS.register("assembly", () -> new RecipeSerializer<>(AssemblyRecipe.CODEC,
            ByteBufCodecs.fromCodecWithRegistries(AssemblyRecipe.CODEC.codec())));
    private BCSiliconRecipes() {}
    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }
}
