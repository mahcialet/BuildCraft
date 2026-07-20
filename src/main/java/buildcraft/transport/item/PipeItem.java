package buildcraft.transport.item;

import buildcraft.transport.BCTransportDataComponents;
import buildcraft.transport.PipeType;
import buildcraft.transport.block.PipeHolderBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

public final class PipeItem extends BlockItem {
    private final PipeType pipeType;

    public PipeItem(PipeHolderBlock block, PipeType pipeType, Properties properties) {
        super(block, properties);
        this.pipeType = pipeType;
    }

    public PipeType pipeType() {
        return pipeType;
    }

    public ItemStack createStack(@Nullable DyeColor color) {
        ItemStack stack = new ItemStack(this);
        if (color != null) stack.set(BCTransportDataComponents.PIPE_COLOR.get(), color);
        return stack;
    }

    public static @Nullable DyeColor color(ItemStack stack) {
        return stack.get(BCTransportDataComponents.PIPE_COLOR.get());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.buildcrafttransport." + pipeType.itemId());
    }
}
