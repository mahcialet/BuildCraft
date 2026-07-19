package buildcraft.lib.fluid;

import buildcraft.api.fuels.IFuel;
import buildcraft.api.fuels.IFuelManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Current, deterministic implementation of the public liquid-fuel registry. */
public enum FuelRegistry implements IFuelManager {
    INSTANCE;

    private final List<IFuel> fuels = new ArrayList<>();

    @Override
    public synchronized <F extends IFuel> F addFuel(F fuel) {
        if (fuel.getFluid().isEmpty()) throw new IllegalArgumentException("Fuel fluid cannot be empty");
        if (fuel.getPowerPerCycle() <= 0) throw new IllegalArgumentException("Fuel power must be positive");
        if (fuel.getTotalBurningTime() <= 0) throw new IllegalArgumentException("Fuel time must be positive");
        fuels.removeIf(existing -> existing.getFluid().getFluid() == fuel.getFluid().getFluid());
        fuels.add(fuel);
        return fuel;
    }

    /** Registers a built-in fuel without constructing component-bearing stacks during mod loading. */
    public synchronized IFuel addFuel(Supplier<? extends net.minecraft.world.level.material.Fluid> fluid,
        long powerPerCycle, int totalBurningTime) {
        if (powerPerCycle <= 0) throw new IllegalArgumentException("Fuel power must be positive");
        if (totalBurningTime <= 0) throw new IllegalArgumentException("Fuel time must be positive");
        IFuel fuel = new LazyFuel(fluid, powerPerCycle, totalBurningTime);
        fuels.add(fuel);
        return fuel;
    }

    /** Registers a component-free built-in dirty fuel lazily during mod construction. */
    public synchronized IDirtyFuel addDirtyFuel(
        Supplier<? extends net.minecraft.world.level.material.Fluid> fluid,
        long powerPerCycle, int totalBurningTime,
        Supplier<? extends net.minecraft.world.level.material.Fluid> residue, int residueAmount) {
        if (powerPerCycle <= 0 || totalBurningTime <= 0 || residueAmount <= 0) {
            throw new IllegalArgumentException("Dirty fuel values must be positive");
        }
        IDirtyFuel fuel = new LazyDirtyFuel(fluid, powerPerCycle, totalBurningTime, residue, residueAmount);
        fuels.add(fuel);
        return fuel;
    }

    @Override
    public IFuel addFuel(FluidStack fluid, long powerPerCycle, int totalBurningTime) {
        return addFuel(new Fuel(fluid.copyWithAmount(FluidType.BUCKET_VOLUME), powerPerCycle, totalBurningTime));
    }

    @Override
    public IDirtyFuel addDirtyFuel(FluidStack fluid, long powerPerCycle, int totalBurningTime, FluidStack residue) {
        if (residue.isEmpty()) throw new IllegalArgumentException("Fuel residue cannot be empty");
        return addFuel(new DirtyFuel(fluid.copyWithAmount(FluidType.BUCKET_VOLUME), powerPerCycle,
            totalBurningTime, residue.copy()));
    }

    @Override
    public synchronized Collection<IFuel> getFuels() {
        return List.copyOf(fuels);
    }

    @Override
    public synchronized @Nullable IFuel getFuel(FluidStack fluid) {
        if (fluid.isEmpty()) return null;
        for (IFuel fuel : fuels) {
            if (FluidStack.isSameFluidSameComponents(fuel.getFluid(), fluid)) return fuel;
        }
        return null;
    }

    public record Fuel(FluidStack fluid, long powerPerCycle, int totalBurningTime) implements IFuel {
        @Override public FluidStack getFluid() { return fluid.copy(); }
        @Override public long getPowerPerCycle() { return powerPerCycle; }
        @Override public int getTotalBurningTime() { return totalBurningTime; }
    }

    public record DirtyFuel(FluidStack fluid, long powerPerCycle, int totalBurningTime, FluidStack residue)
        implements IDirtyFuel {
        @Override public FluidStack getFluid() { return fluid.copy(); }
        @Override public long getPowerPerCycle() { return powerPerCycle; }
        @Override public int getTotalBurningTime() { return totalBurningTime; }
        @Override public FluidStack getResidue() { return residue.copy(); }
    }

    private record LazyFuel(Supplier<? extends net.minecraft.world.level.material.Fluid> supplier,
        long powerPerCycle, int totalBurningTime) implements IFuel {
        @Override public FluidStack getFluid() { return new FluidStack(supplier.get(), FluidType.BUCKET_VOLUME); }
        @Override public long getPowerPerCycle() { return powerPerCycle; }
        @Override public int getTotalBurningTime() { return totalBurningTime; }
    }

    private record LazyDirtyFuel(Supplier<? extends net.minecraft.world.level.material.Fluid> supplier,
        long powerPerCycle, int totalBurningTime,
        Supplier<? extends net.minecraft.world.level.material.Fluid> residueSupplier,
        int residueAmount) implements IDirtyFuel {
        @Override public FluidStack getFluid() { return new FluidStack(supplier.get(), FluidType.BUCKET_VOLUME); }
        @Override public long getPowerPerCycle() { return powerPerCycle; }
        @Override public int getTotalBurningTime() { return totalBurningTime; }
        @Override public FluidStack getResidue() { return new FluidStack(residueSupplier.get(), residueAmount); }
    }

}
