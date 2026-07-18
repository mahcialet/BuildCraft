package buildcraft.api.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Persistent label and two filter lines stored by a BuildCraft list item. */
public record ListData(String label, List<ListLineData> lines) {
    public static final int HEIGHT = 2;
    public static final int MAX_LABEL_LENGTH = 32;
    public static final Codec<ListData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.sizeLimitedString(MAX_LABEL_LENGTH).optionalFieldOf("label", "").forGetter(ListData::label),
        ListLineData.CODEC.listOf(0, HEIGHT).fieldOf("lines").forGetter(ListData::lines)
    ).apply(instance, ListData::new));

    public ListData {
        label = label == null ? "" : label;
        lines = List.copyOf(lines);
        if (label.length() > MAX_LABEL_LENGTH) throw new IllegalArgumentException("List label is too long");
        if (lines.size() > HEIGHT) throw new IllegalArgumentException("A list can contain at most " + HEIGHT + " lines");
    }

    public static ListData empty() {
        return new ListData("", List.of(ListLineData.empty(), ListLineData.empty()));
    }

    public boolean hasItems() {
        return lines.stream().anyMatch(ListLineData::hasItems);
    }

    public boolean matches(ItemStack target) {
        return lines.stream().anyMatch(line -> line.matches(target));
    }
}
