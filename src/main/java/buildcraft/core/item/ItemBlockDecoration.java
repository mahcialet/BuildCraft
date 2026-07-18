package buildcraft.core.item;

import buildcraft.api.enums.EnumDecoratedBlock;
import buildcraft.core.BCCoreItems;
import buildcraft.core.block.BlockDecoration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

/** Block item that preserves and names each decoration block state. */
public final class ItemBlockDecoration extends BlockItem {
    public ItemBlockDecoration(BlockDecoration block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack createStack(EnumDecoratedBlock type) {
        ItemStack stack = new ItemStack(BCCoreItems.DECORATED.get());
        stack.set(
            DataComponents.BLOCK_STATE,
            BlockItemStateProperties.EMPTY.with(BlockDecoration.DECORATION_TYPE, type)
        );
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        BlockItemStateProperties properties = stack.getOrDefault(
            DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY
        );
        EnumDecoratedBlock type = properties.get(BlockDecoration.DECORATION_TYPE);
        if (type == null) {
            type = EnumDecoratedBlock.DESTROY;
        }
        return Component.translatable("block.buildcraftcore.decorated." + type.getSerializedName());
    }
}
