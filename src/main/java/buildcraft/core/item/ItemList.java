package buildcraft.core.item;

import buildcraft.api.items.IList;
import buildcraft.api.items.ListData;
import buildcraft.core.BCCoreDataComponents;
import buildcraft.core.AdvancementUtil;
import buildcraft.core.menu.ListMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Two-line configurable filter list used by pipes and machines. */
public final class ItemList extends Item implements IList {
    public ItemList(Properties properties) {
        super(properties);
    }

    public static ListData data(ItemStack stack) {
        return stack.getOrDefault(BCCoreDataComponents.LIST.get(), ListData.empty());
    }

    public static void setData(ItemStack stack, ListData data) {
        if (data.label().isEmpty() && !data.hasItems()) stack.remove(BCCoreDataComponents.LIST.get());
        else stack.set(BCCoreDataComponents.LIST.get(), data);
        if (data.hasItems()) stack.set(BCCoreDataComponents.LIST_USED.get(), true);
        else stack.remove(BCCoreDataComponents.LIST_USED.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            AdvancementUtil.award(serverPlayer, "buildcraftcore:list");
            serverPlayer.openMenu(ListMenu.provider(hand), buffer -> buffer.writeEnum(hand));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public String getLabel(ItemStack stack) {
        return data(stack).label();
    }

    @Override
    public boolean setLabel(ItemStack stack, String label) {
        String safe = label == null ? "" : label.substring(0, Math.min(label.length(), ListData.MAX_LABEL_LENGTH));
        ListData old = data(stack);
        setData(stack, new ListData(safe, old.lines()));
        return true;
    }

    @Override
    public boolean matches(ItemStack listStack, ItemStack target) {
        return data(listStack).matches(target);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
        Consumer<Component> tooltip, TooltipFlag flag) {
        String label = getLabel(stack);
        if (!label.isEmpty()) tooltip.accept(Component.literal(label).withStyle(ChatFormatting.ITALIC));
    }
}
