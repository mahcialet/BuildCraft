package buildcraft.silicon.item;

import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/** Timer plugs occupy and block their pipe side, matching the historical pluggable. */
public final class TimerItem extends PipePlugItem {
    public TimerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean blocksConnection(PipeHolderBlockEntity pipe, Direction side, ItemStack stack) {
        return true;
    }
}
