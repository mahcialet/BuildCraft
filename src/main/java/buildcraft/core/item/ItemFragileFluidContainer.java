package buildcraft.core.item;

import buildcraft.api.items.IItemFluidShard;
import buildcraft.core.BCCoreDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** Disposable, extraction-only shard that carries at most half a bucket. */
public final class ItemFragileFluidContainer extends Item implements IItemFluidShard {
    public static final int MAX_FLUID_HELD = 500;

    public ItemFragileFluidContainer(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Item item, FluidStack fluid) {
        if (fluid.isEmpty() || fluid.getAmount() <= 0 || fluid.getAmount() > MAX_FLUID_HELD) {
            throw new IllegalArgumentException("A fragile shard requires 1 to " + MAX_FLUID_HELD + " mB");
        }
        ItemStack stack = new ItemStack(item);
        setFluid(stack, fluid);
        return stack;
    }

    public static void setFluid(ItemStack stack, FluidStack fluid) {
        stack.set(BCCoreDataComponents.FRAGILE_FLUID.get(), SimpleFluidContent.copyOf(fluid));
    }

    public static FluidStack getFluid(ItemStack stack) {
        return stack.getOrDefault(BCCoreDataComponents.FRAGILE_FLUID.get(), SimpleFluidContent.EMPTY).copy();
    }

    @Override
    public void addFluidDrops(List<ItemStack> drops, FluidStack fluid) {
        int remaining = fluid.getAmount();
        while (remaining > 0) {
            int amount = Math.min(MAX_FLUID_HELD, remaining);
            drops.add(create(this, fluid.copyWithAmount(amount)));
            remaining -= amount;
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        return fluid.isEmpty() ? super.getName(stack)
            : Component.translatable("item.buildcraftcore.fragile_fluid_shard.filled", fluid.getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = getFluid(stack);
        if (!fluid.isEmpty()) tooltip.accept(Component.translatable(
            "item.buildcraftcore.fragile_fluid_shard.amount", fluid.getAmount(), MAX_FLUID_HELD
        ));
    }

    public static final class Handler extends ItemAccessFluidHandler {
        public Handler(ItemAccess access) {
            super(access, BCCoreDataComponents.FRAGILE_FLUID.get(), MAX_FLUID_HELD);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return 0;
        }

        @Override
        protected @Nullable ItemResource update(ItemResource current, int index,
            FluidResource resource, int newAmount) {
            return newAmount <= 0 ? null : super.update(current, index, resource, newAmount);
        }
    }
}
