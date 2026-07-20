package buildcraft.core.item;

import buildcraft.core.BCCoreDataComponents;
import buildcraft.core.AdvancementUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The always-enabled BuildCraft guide book. */
public class ItemGuide extends Item {
    public static final String DEFAULT_BOOK = "buildcraftcore:main";

    public ItemGuide(Properties properties) {
        super(properties);
    }

    public static String book(ItemStack stack) {
        return stack.getOrDefault(BCCoreDataComponents.GUIDE_BOOK.get(), DEFAULT_BOOK);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) GuideClientHooks.open(player.getItemInHand(hand), false);
        else if (player instanceof ServerPlayer serverPlayer) AdvancementUtil.award(serverPlayer, "buildcraftcore:guide");
        return InteractionResult.SUCCESS;
    }
}
