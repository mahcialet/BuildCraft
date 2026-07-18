package buildcraft.api.lists;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registry and built-in common-tag matcher for BuildCraft lists. */
public final class ListRegistry {
    private static final List<ListMatchHandler> HANDLERS = new CopyOnWriteArrayList<>();

    static {
        HANDLERS.add(ListRegistry::matchCommonTags);
    }

    private ListRegistry() {
    }

    public static void register(ListMatchHandler handler) {
        if (handler == null) throw new NullPointerException("handler");
        HANDLERS.add(handler);
    }

    public static List<ListMatchHandler> handlers() {
        return List.copyOf(HANDLERS);
    }

    public static boolean matches(ListMatchMode mode, ItemStack source, ItemStack target, boolean precise) {
        if (source.isEmpty() || target.isEmpty()) return false;
        for (ListMatchHandler handler : HANDLERS) {
            if (handler.matches(mode, source, target, precise)) return true;
        }
        return mode == ListMatchMode.TYPE && ItemStack.isSameItem(source, target)
            && (!precise || ItemStack.isSameItemSameComponents(source, target));
    }

    private static boolean matchCommonTags(ListMatchMode mode, ItemStack source, ItemStack target, boolean precise) {
        if (mode == ListMatchMode.DIRECT) return false;
        Set<String> sourceKeys = tagKeys(source, mode);
        if (sourceKeys.isEmpty()) return false;
        Set<String> targetKeys = tagKeys(target, mode);
        sourceKeys.retainAll(targetKeys);
        return !sourceKeys.isEmpty();
    }

    private static Set<String> tagKeys(ItemStack stack, ListMatchMode mode) {
        Set<String> keys = new HashSet<>();
        stack.typeHolder().tags().map(TagKey<Item>::location).forEach(id -> {
            String key = commonTagPart(id, mode);
            if (key != null) keys.add(key);
        });
        return keys;
    }

    private static String commonTagPart(Identifier id, ListMatchMode mode) {
        // NeoForge's replacement for ore-dictionary names is the c namespace,
        // normally shaped as c:<type>/<material> (for example ingots/iron).
        if (!id.getNamespace().equals("c")) return mode == ListMatchMode.CLASS ? id.toString() : null;
        String path = id.getPath();
        int slash = path.indexOf('/');
        if (slash < 1 || slash == path.length() - 1) return mode == ListMatchMode.CLASS ? id.toString() : null;
        return switch (mode) {
            case MATERIAL -> path.substring(slash + 1);
            case TYPE -> path.substring(0, slash);
            case CLASS -> id.toString();
            case DIRECT -> null;
        };
    }
}
