package buildcraft.energy.mixin;

import buildcraft.energy.gen.OilBiomeReplacement;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiNoiseBiomeSource.class)
abstract class MultiNoiseBiomeSourceMixin {
    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"), cancellable = true)
    private void buildcraft$replaceOilBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler,
                                            CallbackInfoReturnable<Holder<Biome>> callback) {
        callback.setReturnValue(OilBiomeReplacement.replace(callback.getReturnValue(), quartX, quartZ));
    }
}
