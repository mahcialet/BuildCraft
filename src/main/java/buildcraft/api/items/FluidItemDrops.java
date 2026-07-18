package buildcraft.api.items;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.function.Supplier;

/** Shared bridge used by future tanks to preserve their contents as item drops. */
public final class FluidItemDrops {
    private static volatile Supplier<? extends IItemFluidShard> item;

    private FluidItemDrops() {
    }

    public static void register(Supplier<? extends IItemFluidShard> shard) {
        if (shard == null) throw new NullPointerException("shard");
        item = shard;
    }

    public static void addFluidDrops(List<ItemStack> drops, FluidStack... fluids) {
        Supplier<? extends IItemFluidShard> supplier = item;
        if (supplier == null) return;
        IItemFluidShard shard = supplier.get();
        for (FluidStack fluid : fluids) if (fluid != null && !fluid.isEmpty()) shard.addFluidDrops(drops, fluid);
    }
}
