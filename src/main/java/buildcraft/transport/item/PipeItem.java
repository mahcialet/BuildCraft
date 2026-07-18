package buildcraft.transport.item;

import buildcraft.transport.PipeType;
import buildcraft.transport.block.PipeHolderBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public final class PipeItem extends BlockItem {
    private final PipeType pipeType;

    public PipeItem(PipeHolderBlock block, PipeType pipeType, Properties properties) {
        super(block, properties);
        this.pipeType = pipeType;
    }

    public PipeType pipeType() {
        return pipeType;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.buildcrafttransport." + pipeType.itemId());
    }
}
