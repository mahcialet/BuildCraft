package buildcraft.energy.gen;

import buildcraft.energy.BCEnergy;
import buildcraft.energy.BCEnergyConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Random;

/** Server-scoped state used by the biome-source hook that recreates BuildCraft's GenLayer replacements. */
public final class OilBiomeReplacement {
    public static final ResourceKey<Biome> OIL_OCEAN = key("oil_ocean");
    public static final ResourceKey<Biome> OIL_DESERT = key("oil_desert");
    private static final int OFFSET_RANGE = 500_000;
    private static volatile State state;

    private OilBiomeReplacement() {
    }

    public static void serverStarting(ServerAboutToStartEvent event) {
        var biomes = event.getServer().registryAccess().lookupOrThrow(Registries.BIOME);
        long seed = event.getServer().getWorldGenSettings().options().seed();
        Random random = new Random(seed);
        state = new State(
                biomes.getOrThrow(OIL_OCEAN), biomes.getOrThrow(OIL_DESERT),
                random.nextInt(OFFSET_RANGE) - OFFSET_RANGE / 2,
                random.nextInt(OFFSET_RANGE) - OFFSET_RANGE / 2
        );
    }

    public static void serverStopped(ServerStoppedEvent event) {
        state = null;
    }

    public static Holder<Biome> replace(Holder<Biome> original, int quartX, int quartZ) {
        if (!buildcraft.core.BCCoreConfig.WORLDGEN_ENABLED.get()) return original;
        State current = state;
        if (current == null || !original.is(BiomeTags.IS_OVERWORLD)) return original;
        double blockX = quartX * 4.0;
        double blockZ = quartZ * 4.0;
        if (BCEnergyConfig.ENABLE_OIL_OCEAN_BIOME.get() && original.is(Tags.Biomes.IS_OCEAN)
                && LegacySimplexNoise.noise((blockX + current.xOffset) * 0.0005,
                        (blockZ + current.zOffset) * 0.0005) > 0.9) {
            return current.ocean;
        }
        if (BCEnergyConfig.ENABLE_OIL_DESERT_BIOME.get()
                && original.is(Tags.Biomes.IS_HOT) && original.is(Tags.Biomes.IS_DRY)
                && original.is(Tags.Biomes.IS_SANDY)
                && LegacySimplexNoise.noise((blockX + current.xOffset) * 0.001,
                        (blockZ + current.zOffset) * 0.001) > 0.7) {
            return current.desert;
        }
        return original;
    }

    public static boolean oilOceanNoise(int quartX, int quartZ) {
        State current = state;
        return current != null && LegacySimplexNoise.noise((quartX * 4.0 + current.xOffset) * 0.0005,
                (quartZ * 4.0 + current.zOffset) * 0.0005) > 0.9;
    }

    public static boolean oilDesertNoise(int quartX, int quartZ) {
        State current = state;
        return current != null && LegacySimplexNoise.noise((quartX * 4.0 + current.xOffset) * 0.001,
                (quartZ * 4.0 + current.zOffset) * 0.001) > 0.7;
    }

    private static ResourceKey<Biome> key(String path) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(BCEnergy.MOD_ID, path));
    }

    private record State(Holder<Biome> ocean, Holder<Biome> desert, int xOffset, int zOffset) {
    }
}
