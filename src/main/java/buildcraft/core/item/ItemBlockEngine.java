package buildcraft.core.item;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.core.BCCoreItems;
import buildcraft.core.block.BlockEngine;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

/** Engine item preserving the historical engine type state. */
public final class ItemBlockEngine extends BlockItem {
    public ItemBlockEngine(BlockEngine block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack redstoneEngine() {
        return createStack(EnumEngineType.WOOD);
    }

    public static ItemStack creativeEngine() {
        return createStack(EnumEngineType.CREATIVE);
    }
    public static ItemStack stirlingEngine() {
        return createStack(EnumEngineType.STONE);
    }
    public static ItemStack combustionEngine() {
        return createStack(EnumEngineType.IRON);
    }

    public static ItemStack createStack(EnumEngineType type) {
        ItemStack stack = new ItemStack(BCCoreItems.ENGINE.get());
        stack.set(DataComponents.BLOCK_STATE,
            BlockItemStateProperties.EMPTY.with(BlockEngine.ENGINE_TYPE, type));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        EnumEngineType type = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY)
            .get(BlockEngine.ENGINE_TYPE);
        if (type == null) type = EnumEngineType.WOOD;
        return Component.translatable("block.buildcraftcore.engine." + type.getSerializedName());
    }
}
