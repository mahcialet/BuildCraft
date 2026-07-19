package buildcraft.silicon.item;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.transport.item.LensAttachment;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public final class LensItem extends PipePlugItem implements LensAttachment {
    public LensItem(Properties properties) { super(properties); }

    @Override
    public DyeColor lensColor(ItemStack stack) {
        return stack.getOrDefault(BCSiliconDataComponents.LENS_COLOR.get(), DyeColor.WHITE);
    }

    @Override
    public boolean isFilter(ItemStack stack) {
        return stack.getOrDefault(BCSiliconDataComponents.LENS_FILTER.get(), false);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component color = Component.translatable("color.minecraft." + lensColor(stack).getName());
        return Component.translatable("item.buildcraftsilicon.plug_lens." +
                (isFilter(stack) ? "filter" : "lens"), color);
    }
}
