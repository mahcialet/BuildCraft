package buildcraft.core.item;

import buildcraft.core.BCCoreDataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Portable link to a guide page, preserving the historical note_id contract. */
public class ItemGuideNote extends Item {
    public ItemGuideNote(Properties properties) {
        super(properties);
    }

    public static String note(ItemStack stack) {
        return stack.getOrDefault(BCCoreDataComponents.GUIDE_NOTE.get(), "");
    }

    public static ItemStack forPage(Item item, String pageId) {
        ItemStack stack = new ItemStack(item);
        stack.set(BCCoreDataComponents.GUIDE_NOTE.get(), pageId);
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) GuideClientHooks.open(player.getItemInHand(hand), true);
        return InteractionResult.SUCCESS;
    }
}
