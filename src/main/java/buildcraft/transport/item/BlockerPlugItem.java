package buildcraft.transport.item;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public final class BlockerPlugItem extends TransportPlugItem {
    public BlockerPlugItem(Properties properties) { super(properties); }

    @Override
    public boolean blocksConnection(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {
        return true;
    }
}
