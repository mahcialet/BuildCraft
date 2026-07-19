package buildcraft.builders.item;

import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.snapshot.SnapshotData;
import buildcraft.builders.snapshot.SnapshotKind;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class SnapshotItem extends Item {
    private final SnapshotKind kind;
    public SnapshotItem(Properties properties, SnapshotKind kind) {
        super(properties.stacksTo(16));
        this.kind = kind;
    }
    public SnapshotKind kind() { return kind; }
    public static ItemStack apply(ItemStack stack, SnapshotData snapshot) {
        stack.set(BCBuildersDataComponents.SNAPSHOT.get(), snapshot);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("buildcraftbuilders",
                snapshot.kind() == SnapshotKind.BLUEPRINT ? "blueprint_used" : "template_used"));
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> builder, TooltipFlag flag) {
        SnapshotData snapshot = stack.get(BCBuildersDataComponents.SNAPSHOT.get());
        if (snapshot == null) {
            builder.accept(Component.literal("Blank"));
            return;
        }
        if (!snapshot.name().isBlank()) builder.accept(Component.literal(snapshot.name()));
        builder.accept(Component.literal(snapshot.size().getX() + " × " + snapshot.size().getY()
                + " × " + snapshot.size().getZ()));
    }
}
