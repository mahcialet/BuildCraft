package buildcraft.silicon.item;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.silicon.ChipsetType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RedstoneChipsetItem extends Item {
    public RedstoneChipsetItem(Properties properties) { super(properties); }

    @Override
    public Component getName(ItemStack stack) {
        ChipsetType type = stack.getOrDefault(BCSiliconDataComponents.CHIPSET_TYPE.get(), ChipsetType.RED);
        return Component.translatable("item.buildcraftsilicon.redstone_chipset." + type.getSerializedName());
    }
}
