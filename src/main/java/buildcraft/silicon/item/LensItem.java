package buildcraft.silicon.item;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.transport.item.LensAttachment;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.Optional;

public final class LensItem extends PipePlugItem implements LensAttachment {
    public LensItem(Properties properties) { super(properties); }

    @Override
    public Optional<DyeColor> lensColor(ItemStack stack) {
        return Optional.ofNullable(stack.get(BCSiliconDataComponents.LENS_COLOR.get()));
    }

    @Override
    public boolean isFilter(ItemStack stack) {
        return stack.getOrDefault(BCSiliconDataComponents.LENS_FILTER.get(), false);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component color = lensColor(stack)
                .<Component>map(value -> Component.translatable("color.minecraft." + value.getName()))
                .orElseGet(() -> Component.literal("Clear"));
        return Component.translatable("item.buildcraftsilicon.plug_lens." +
                (isFilter(stack) ? "filter" : "lens"), color);
    }
}
