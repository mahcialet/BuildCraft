package buildcraft.silicon.item;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.transport.item.FacadeAttachment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class FacadeItem extends PipePlugItem implements FacadeAttachment {
    public FacadeItem(Properties properties) { super(properties); }

    @Override
    public BlockState facadeState(ItemStack stack) {
        return stack.getOrDefault(BCSiliconDataComponents.FACADE_STATE.get(), Blocks.STONE.defaultBlockState());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.buildcraftsilicon.plug_facade", facadeState(stack).getBlock().getName());
    }
}
