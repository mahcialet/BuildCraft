package buildcraft.api.items;

import buildcraft.api.lists.ListMatchMode;
import buildcraft.api.lists.ListRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** One nine-entry line and its matching options. */
public record ListLineData(List<ItemStack> stacks, boolean precise, ListMatchMode mode) {
    public static final int WIDTH = 9;
    public static final Codec<ListLineData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC.listOf(0, WIDTH).fieldOf("stacks").forGetter(ListLineData::stacks),
        Codec.BOOL.optionalFieldOf("precise", false).forGetter(ListLineData::precise),
        ListMatchMode.CODEC.optionalFieldOf("mode", ListMatchMode.DIRECT).forGetter(ListLineData::mode)
    ).apply(instance, ListLineData::new));

    public ListLineData {
        if (stacks.size() > WIDTH) throw new IllegalArgumentException("A list line can contain at most " + WIDTH + " stacks");
        stacks = stacks.stream().map(stack -> stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1)).toList();
    }

    public static ListLineData empty() {
        return new ListLineData(List.of(), false, ListMatchMode.DIRECT);
    }

    public boolean hasItems() {
        return stacks.stream().anyMatch(stack -> !stack.isEmpty());
    }

    public boolean matches(ItemStack target) {
        if (mode == ListMatchMode.DIRECT) {
            return stacks.stream().filter(stack -> !stack.isEmpty()).anyMatch(stack ->
                precise ? ItemStack.isSameItemSameComponents(stack, target) : ItemStack.isSameItem(stack, target)
            );
        }
        return !stacks.isEmpty() && ListRegistry.matches(mode, stacks.getFirst(), target, precise);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ListLineData other)
            || precise != other.precise || mode != other.mode || stacks.size() != other.stacks.size()) return false;
        for (int i = 0; i < stacks.size(); i++) {
            if (!ItemStack.matches(stacks.get(i), other.stacks.get(i))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = Boolean.hashCode(precise);
        hash = 31 * hash + mode.hashCode();
        for (ItemStack stack : stacks) hash = 31 * hash + ItemStack.hashItemAndComponents(stack);
        return hash;
    }
}
