package buildcraft.energy;

import buildcraft.api.mj.MjAPI;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Canonical BuildCraft 8 petroleum distillation and heat-state recipes. */
public final class BCEnergyRefineryRecipes {
    public record DistillationRecipe(Fluid input, int inputAmount, Fluid gasOutput, int gasAmount,
        Fluid liquidOutput, int liquidAmount, long powerRequired) {}

    public record HeatExchangeRecipe(Fluid input, Fluid output, int amount, int heatDelta) {}

    private static final Map<Fluid, DistillationRecipe> DISTILLATION = new IdentityHashMap<>();
    private static final Map<Fluid, HeatExchangeRecipe> HEATING = new IdentityHashMap<>();
    private static final Map<Fluid, HeatExchangeRecipe> COOLING = new IdentityHashMap<>();
    private static boolean bootstrapped;

    private BCEnergyRefineryRecipes() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;
        addDistillation("oil", 16, "fuel_gaseous", 8, "oil_heavy", 3, 0, 32);
        addDistillation("oil", 16, "fuel_mixed_light", 10, "oil_dense", 2, 1, 16);
        addDistillation("oil", 16, "oil_distilled", 8, "oil_residue", 1, 2, 12);
        addDistillation("oil_distilled", 8, "fuel_gaseous", 8, "fuel_mixed_heavy", 5, 0, 24);
        addDistillation("oil_distilled", 8, "fuel_mixed_light", 10, "fuel_dense", 2, 1, 16);
        addDistillation("fuel_mixed_light", 10, "fuel_gaseous", 8, "fuel_light", 6, 0, 24);
        addDistillation("oil_heavy", 3, "fuel_light", 6, "oil_dense", 2, 1, 16);
        addDistillation("oil_heavy", 3, "fuel_mixed_heavy", 5, "oil_residue", 1, 2, 12);
        addDistillation("fuel_mixed_heavy", 5, "fuel_light", 6, "fuel_dense", 2, 1, 16);
        addDistillation("oil_dense", 2, "fuel_dense", 2, "oil_residue", 1, 2, 12);
        for (var family : BCEnergyFluids.REFINERY_FLUIDS.values()) {
            for (int heat = 0; heat < 2; heat++) {
                Fluid cool = family.heat(heat).source().get();
                Fluid hot = family.heat(heat + 1).source().get();
                HEATING.put(cool, new HeatExchangeRecipe(cool, hot, 10, 1));
                COOLING.put(hot, new HeatExchangeRecipe(hot, cool, 10, -1));
            }
        }
    }

    private static void addDistillation(String input, int inputAmount, String gas, int gasAmount,
        String liquid, int liquidAmount, int heat, long mj) {
        Fluid inputFluid = BCEnergyFluids.refineryFluid(input).heat(heat).source().get();
        DistillationRecipe recipe = new DistillationRecipe(inputFluid, inputAmount,
            BCEnergyFluids.refineryFluid(gas).heat(heat).source().get(), gasAmount,
            BCEnergyFluids.refineryFluid(liquid).heat(heat).source().get(), liquidAmount, mj * MjAPI.MJ);
        if (DISTILLATION.put(inputFluid, recipe) != null) {
            throw new IllegalStateException("Duplicate distillation recipe for " + input + " heat " + heat);
        }
    }

    public static DistillationRecipe distillation(Fluid input) { return DISTILLATION.get(input); }
    public static HeatExchangeRecipe heating(Fluid input) { return HEATING.get(input); }
    public static HeatExchangeRecipe cooling(Fluid input) { return COOLING.get(input); }
    public static List<DistillationRecipe> distillationRecipes() {
        return List.copyOf(new ArrayList<>(DISTILLATION.values()));
    }
    public static List<HeatExchangeRecipe> heatingRecipes() {
        return List.copyOf(new ArrayList<>(HEATING.values()));
    }
    public static List<HeatExchangeRecipe> coolingRecipes() {
        return List.copyOf(new ArrayList<>(COOLING.values()));
    }
}
