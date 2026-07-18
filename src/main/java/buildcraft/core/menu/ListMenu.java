package buildcraft.core.menu;

import buildcraft.api.items.ListData;
import buildcraft.api.items.ListLineData;
import buildcraft.api.lists.ListMatchMode;
import buildcraft.core.BCCoreItems;
import buildcraft.core.BCCoreMenus;
import buildcraft.core.item.ItemList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative list editor with 18 fake slots. */
public final class ListMenu extends AbstractContainerMenu {
    public static final int WIDTH = ListLineData.WIDTH;
    public static final int HEIGHT = ListData.HEIGHT;
    public static final int FAKE_SLOTS = WIDTH * HEIGHT;

    private final Inventory inventory;
    private final InteractionHand hand;
    private final SimpleContainer filters = new SimpleContainer(FAKE_SLOTS);
    private final SimpleContainerData options = new SimpleContainerData(HEIGHT);

    public ListMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readEnum(InteractionHand.class));
    }

    public ListMenu(int id, Inventory inventory, InteractionHand hand) {
        super(BCCoreMenus.LIST.get(), id);
        this.inventory = inventory;
        this.hand = hand;
        load(ItemList.data(listStack()));
        for (int line = 0; line < HEIGHT; line++) {
            for (int column = 0; column < WIDTH; column++) {
                addSlot(new FakeSlot(filters, line * WIDTH + column, 8 + column * 18, 32 + line * 34));
            }
        }
        addStandardInventorySlots(inventory, 8, 109);
        addDataSlots(options);
    }

    public static MenuProvider provider(InteractionHand hand) {
        return new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("item.buildcraftcore.list");
            }

            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new ListMenu(id, inventory, hand);
            }
        };
    }

    private ItemStack listStack() {
        return inventory.player.getItemInHand(hand);
    }

    public InteractionHand hand() {
        return hand;
    }

    public String label() {
        return ItemList.data(listStack()).label();
    }

    public boolean precise(int line) {
        return (options.get(line) & 1) != 0;
    }

    public ListMatchMode mode(int line) {
        return ListMatchMode.values()[(options.get(line) >> 1) & 3];
    }

    public void setLabel(String label) {
        if (!stillValid(inventory.player)) return;
        ((ItemList) listStack().getItem()).setLabel(listStack(), label);
        broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int line = id / 3;
        int button = id % 3;
        if (line < 0 || line >= HEIGHT || button < 0 || button > 2) return false;
        int bits = options.get(line);
        if (button == 0) bits ^= 1;
        else {
            int modeBits = (bits >> 1) & 3;
            int mask = button == 1 ? 2 : 1;
            modeBits ^= mask;
            bits = (bits & 1) | modeBits << 1;
            if (modeBits != 0) {
                for (int column = 1; column < WIDTH; column++) filters.setItem(line * WIDTH + column, ItemStack.EMPTY);
            }
        }
        options.set(line, bits);
        save();
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < FAKE_SLOTS) {
            if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) {
                ItemStack carried = getCarried();
                filters.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
                save();
            } else if (input == ContainerInput.CLONE && player.getAbilities().instabuild) {
                ItemStack shown = filters.getItem(slotId);
                if (!shown.isEmpty()) setCarried(shown.copyWithCount(shown.getMaxStackSize()));
            }
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override public boolean canDragTo(Slot slot) {
        return !slot.isFake() && super.canDragTo(slot);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        return ItemStack.EMPTY;
    }

    @Override public boolean stillValid(Player player) {
        return !listStack().isEmpty() && listStack().is(BCCoreItems.LIST.get());
    }

    private void load(ListData data) {
        for (int line = 0; line < HEIGHT; line++) {
            ListLineData lineData = line < data.lines().size() ? data.lines().get(line) : ListLineData.empty();
            for (int column = 0; column < lineData.stacks().size(); column++) {
                filters.setItem(line * WIDTH + column, lineData.stacks().get(column).copyWithCount(1));
            }
            options.set(line, (lineData.precise() ? 1 : 0) | lineData.mode().ordinal() << 1);
        }
    }

    private void save() {
        if (!stillValid(inventory.player)) return;
        List<ListLineData> lines = new ArrayList<>(HEIGHT);
        for (int line = 0; line < HEIGHT; line++) {
            List<ItemStack> stacks = new ArrayList<>(WIDTH);
            int last = -1;
            for (int column = 0; column < WIDTH; column++) {
                ItemStack stack = filters.getItem(line * WIDTH + column);
                stacks.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
                if (!stack.isEmpty()) last = column;
            }
            lines.add(new ListLineData(List.copyOf(stacks.subList(0, last + 1)), precise(line), mode(line)));
        }
        ListData old = ItemList.data(listStack());
        ItemList.setData(listStack(), new ListData(old.label(), lines));
        broadcastChanges();
    }

    private static final class FakeSlot extends Slot {
        private FakeSlot(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean isFake() { return true; }
    }
}
