package buildcraft.builders.library;

import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.snapshot.SnapshotData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

public final class BlueprintLibrarySavedData extends SavedData {
    public static final Codec<BlueprintLibrarySavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LibraryEntry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(data -> data.entries),
            Codec.INT.optionalFieldOf("next_id", 1).forGetter(data -> data.nextId)
    ).apply(instance, BlueprintLibrarySavedData::new));
    public static final SavedDataType<BlueprintLibrarySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("buildcraftbuilders", "blueprint_library"),
            BlueprintLibrarySavedData::new, CODEC);
    private final ArrayList<LibraryEntry> entries;
    private int nextId;

    public BlueprintLibrarySavedData() { this(List.of(), 1); }
    private BlueprintLibrarySavedData(List<LibraryEntry> entries, int nextId) {
        this.entries = new ArrayList<>(entries);
        this.nextId = Math.max(nextId, entries.stream().mapToInt(LibraryEntry::id).max().orElse(0) + 1);
    }
    public static BlueprintLibrarySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
    public List<LibraryEntry> entries() { return List.copyOf(entries); }
    public LibraryEntry entry(int index) { return index >= 0 && index < entries.size() ? entries.get(index) : null; }
    public int add(ItemStack stack) {
        ItemStack stored = stack.copyWithCount(1);
        for (int index = 0; index < entries.size(); index++) {
            if (ItemStack.isSameItemSameComponents(entries.get(index).stack(), stored)) return index;
        }
        entries.add(new LibraryEntry(nextId++, name(stored), stored));
        setDirty();
        return entries.size() - 1;
    }
    public boolean remove(int index) {
        if (index < 0 || index >= entries.size()) return false;
        entries.remove(index);
        setDirty();
        return true;
    }
    private static String name(ItemStack stack) {
        SnapshotData snapshot = stack.get(BCBuildersDataComponents.SNAPSHOT.get());
        if (snapshot != null && !snapshot.name().isBlank()) return snapshot.name();
        var book = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (book != null && !book.title().raw().isBlank()) return book.title().raw();
        return stack.getHoverName().getString();
    }
}
