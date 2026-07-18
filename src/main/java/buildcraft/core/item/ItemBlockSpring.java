package buildcraft.core.item;

import buildcraft.api.enums.EnumSpring;
import buildcraft.core.BCCoreItems;
import buildcraft.core.block.BlockSpring;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

/** Block item that preserves and names the water and oil spring states. */
public final class ItemBlockSpring extends BlockItem {
    public ItemBlockSpring(BlockSpring block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack createStack(EnumSpring type) {
        ItemStack stack = new ItemStack(BCCoreItems.SPRING.get());
        stack.set(DataComponents.BLOCK_STATE,
            BlockItemStateProperties.EMPTY.with(BlockSpring.SPRING_TYPE, type));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        EnumSpring type = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
            .get(BlockSpring.SPRING_TYPE);
        if (type == null) type = EnumSpring.WATER;
        return Component.translatable("block.buildcraftcore.spring." + type.getSerializedName());
    }
}
