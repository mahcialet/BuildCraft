package buildcraft.builders.item;

import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.snapshot.ExternalSnapshotSavedData;
import buildcraft.builders.snapshot.SnapshotData;
import buildcraft.builders.snapshot.SnapshotKind;
import buildcraft.builders.snapshot.SnapshotReference;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public final class SnapshotItem extends Item {
    private final SnapshotKind kind;

    public SnapshotItem(Properties properties, SnapshotKind kind) {
        super(properties.stacksTo(16));
        this.kind = kind;
    }

    public SnapshotKind kind() {
        return kind;
    }

    public static ItemStack apply(ItemStack stack, SnapshotData snapshot) {
        stack.set(BCBuildersDataComponents.SNAPSHOT.get(), snapshot);
        stack.remove(BCBuildersDataComponents.SNAPSHOT_REFERENCE.get());
        return markUsed(stack, snapshot.kind());
    }

    public static ItemStack applyExternal(ItemStack stack, SnapshotData snapshot, ServerLevel level) {
        var id = ExternalSnapshotSavedData.get(level).store(snapshot);
        stack.remove(BCBuildersDataComponents.SNAPSHOT.get());
        stack.set(BCBuildersDataComponents.SNAPSHOT_REFERENCE.get(), SnapshotReference.of(id, snapshot));
        return markUsed(stack, snapshot.kind());
    }

    private static ItemStack markUsed(ItemStack stack, SnapshotKind kind) {
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("buildcraftbuilders",
            kind == SnapshotKind.BLUEPRINT ? "blueprint_used" : "template_used"));
        return stack;
    }

    public static boolean hasSnapshot(ItemStack stack) {
        return stack.has(BCBuildersDataComponents.SNAPSHOT.get())
            || stack.has(BCBuildersDataComponents.SNAPSHOT_REFERENCE.get());
    }

    public static SnapshotKind snapshotKind(ItemStack stack) {
        SnapshotData inline = stack.get(BCBuildersDataComponents.SNAPSHOT.get());
        if (inline != null) return inline.kind();
        SnapshotReference reference = stack.get(BCBuildersDataComponents.SNAPSHOT_REFERENCE.get());
        return reference == null ? null : reference.kind();
    }

    public static SnapshotData resolve(ItemStack stack, Level level) {
        SnapshotData inline = stack.get(BCBuildersDataComponents.SNAPSHOT.get());
        if (inline != null) return inline;
        SnapshotReference reference = stack.get(BCBuildersDataComponents.SNAPSHOT_REFERENCE.get());
        if (reference == null || !(level instanceof ServerLevel serverLevel)) return null;
        return ExternalSnapshotSavedData.get(serverLevel).resolve(reference.id()).orElse(null);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> builder, TooltipFlag flag) {
        SnapshotData snapshot = stack.get(BCBuildersDataComponents.SNAPSHOT.get());
        SnapshotReference reference = stack.get(BCBuildersDataComponents.SNAPSHOT_REFERENCE.get());
        if (snapshot == null && reference == null) {
            builder.accept(Component.literal("Blank"));
            return;
        }
        String name = snapshot != null ? snapshot.name() : reference.name();
        var size = snapshot != null ? snapshot.size() : reference.size();
        if (!name.isBlank()) builder.accept(Component.literal(name));
        builder.accept(Component.literal(size.getX() + " × " + size.getY() + " × " + size.getZ()));
    }
}
