package buildcraft.builders.library;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

public record LibraryEntry(int id, String name, ItemStack stack) {
    public static final Codec<LibraryEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(LibraryEntry::id),
            Codec.STRING.fieldOf("name").forGetter(LibraryEntry::name),
            ItemStack.CODEC.fieldOf("stack").forGetter(LibraryEntry::stack)
    ).apply(instance, LibraryEntry::new));
    public LibraryEntry {
        stack = stack.copyWithCount(1);
    }
}
