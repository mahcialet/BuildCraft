package buildcraft.lib.fluid;

import buildcraft.api.fuels.ICoolant;
import buildcraft.api.fuels.ICoolantManager;
import buildcraft.api.fuels.ISolidCoolant;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** Deterministic implementation of the public coolant registry. */
public enum CoolantRegistry implements ICoolantManager {
    INSTANCE;

    private final List<ICoolant> coolants = new ArrayList<>();
    private final List<ISolidCoolant> solidCoolants = new ArrayList<>();

    @Override
    public synchronized <C extends ICoolant> C addCoolant(C coolant) {
        coolants.add(coolant);
        return coolant;
    }

    @Override
    public synchronized <C extends ISolidCoolant> C addSolidCoolant(C coolant) {
        solidCoolants.add(coolant);
        return coolant;
    }

    @Override
    public ICoolant addCoolant(FluidStack fluid, float degreesCoolingPerMB) {
        if (fluid.isEmpty()) throw new IllegalArgumentException("Coolant fluid cannot be empty");
        if (!(degreesCoolingPerMB > 0)) throw new IllegalArgumentException("Cooling value must be positive");
        FixedCoolant coolant = new FixedCoolant(fluid.copyWithAmount(1), degreesCoolingPerMB);
        synchronized (this) {
            coolants.removeIf(existing -> existing.matchesFluid(coolant.fluid));
            coolants.add(coolant);
        }
        return coolant;
    }

    /** Registers a coolant without constructing a component-bearing stack during mod bootstrap. */
    public synchronized ICoolant addCoolant(
        Supplier<? extends net.minecraft.world.level.material.Fluid> fluid, float degreesCoolingPerMB
    ) {
        if (!(degreesCoolingPerMB > 0)) throw new IllegalArgumentException("Cooling value must be positive");
        LazyCoolant coolant = new LazyCoolant(fluid, degreesCoolingPerMB);
        coolants.removeIf(existing -> existing instanceof LazyCoolant lazy && lazy.fluid.get() == fluid.get());
        coolants.add(coolant);
        return coolant;
    }

    @Override
    public ISolidCoolant addSolidCoolant(ItemStack solid, FluidStack fluid, float multiplier) {
        if (solid.isEmpty() || fluid.isEmpty()) throw new IllegalArgumentException("Solid coolant cannot be empty");
        if (!(multiplier > 0)) throw new IllegalArgumentException("Coolant multiplier must be positive");
        return addSolidCoolant(new FixedSolidCoolant(solid.copy(), fluid.copy(), multiplier));
    }

    @Override
    public synchronized Collection<ICoolant> getCoolants() {
        return List.copyOf(coolants);
    }

    @Override
    public synchronized Collection<ISolidCoolant> getSolidCoolants() {
        return List.copyOf(solidCoolants);
    }

    @Override
    public synchronized @Nullable ICoolant getCoolant(FluidStack fluid) {
        if (fluid.isEmpty()) return null;
        for (ICoolant coolant : coolants) if (coolant.matchesFluid(fluid)) return coolant;
        return null;
    }

    @Override
    public synchronized float getDegreesPerMb(FluidStack fluid, float heat) {
        if (fluid.isEmpty()) return 0;
        for (ICoolant coolant : coolants) {
            float degrees = coolant.getDegreesCoolingPerMB(fluid, heat);
            if (degrees > 0) return degrees;
        }
        return 0;
    }

    @Override
    public synchronized @Nullable ISolidCoolant getSolidCoolant(ItemStack solid) {
        if (solid.isEmpty()) return null;
        for (ISolidCoolant coolant : solidCoolants) {
            FluidStack converted = coolant.getFluidFromSolidCoolant(solid);
            if (converted != null && !converted.isEmpty()) return coolant;
        }
        return null;
    }

    private record FixedCoolant(FluidStack fluid, float degrees) implements ICoolant {
        @Override
        public boolean matchesFluid(FluidStack stack) {
            return !stack.isEmpty() && FluidStack.isSameFluidSameComponents(fluid, stack);
        }

        @Override
        public float getDegreesCoolingPerMB(FluidStack stack, float heat) {
            return matchesFluid(stack) ? degrees : 0;
        }
    }

    private record LazyCoolant(
        Supplier<? extends net.minecraft.world.level.material.Fluid> fluid, float degrees
    ) implements ICoolant {
        @Override
        public boolean matchesFluid(FluidStack stack) {
            return !stack.isEmpty() && stack.getFluid() == fluid.get();
        }

        @Override
        public float getDegreesCoolingPerMB(FluidStack stack, float heat) {
            return matchesFluid(stack) ? degrees : 0;
        }
    }

    private record FixedSolidCoolant(ItemStack solid, FluidStack fluid, float multiplier) implements ISolidCoolant {
        @Override
        public @Nullable FluidStack getFluidFromSolidCoolant(ItemStack stack) {
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, solid)) return null;
            int amount = (int) (stack.getCount() * (long) fluid.getAmount() * multiplier / solid.getCount());
            return amount > 0 ? fluid.copyWithAmount(amount) : null;
        }
    }
}
