package buildcraft.core.item;

import java.util.function.BiConsumer;
import net.minecraft.world.item.ItemStack;

/** Avoids loading client screen classes from item code on a dedicated server. */
public final class GuideClientHooks {
    private static BiConsumer<ItemStack, Boolean> opener = (stack, note) -> { };

    private GuideClientHooks() { }

    public static void install(BiConsumer<ItemStack, Boolean> clientOpener) {
        opener = clientOpener;
    }

    public static void open(ItemStack stack, boolean note) {
        opener.accept(stack, note);
    }
}
