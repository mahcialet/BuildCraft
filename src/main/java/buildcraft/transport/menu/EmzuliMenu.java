package buildcraft.transport.menu;

import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.PipeType;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class EmzuliMenu extends AbstractContainerMenu {
    public static final int PRESETS = 4;
    private final Inventory inventory;
    private final BlockPos pos;
    private final SimpleContainer shown = new SimpleContainer(PRESETS);

    public EmzuliMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    public EmzuliMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCTransportMenus.EMZULI.get(), id);
        this.inventory = inventory;
        this.pos = pos;
        refresh();
        int[] xs = { 25, 25, 134, 134 };
        int[] ys = { 21, 49, 21, 49 };
        for (int index = 0; index < PRESETS; index++) addSlot(new FilterSlot(shown, index, xs[index], ys[index]));
        addStandardInventorySlots(inventory, 8, 84);
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("gui.buildcrafttransport.emzuli");
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new EmzuliMenu(id, inventory, pos);
            }
        }, buffer -> buffer.writeBlockPos(pos));
    }

    private PipeHolderBlockEntity holder() {
        return inventory.player.level().getBlockEntity(pos) instanceof PipeHolderBlockEntity holder
            && holder.pipeType() == PipeType.EMZULI_ITEM ? holder : null;
    }

    private void refresh() {
        PipeHolderBlockEntity holder = holder();
        if (holder == null) return;
        for (int index = 0; index < PRESETS; index++) shown.setItem(index, holder.emzuliFilters().get(index));
    }

    public Optional<DyeColor> color(int index) {
        PipeHolderBlockEntity holder = holder();
        return holder == null ? Optional.empty() : holder.emzuliColor(index);
    }

    public boolean active(int index) {
        PipeHolderBlockEntity holder = holder();
        return holder != null && holder.emzuliActive(index);
    }

    public int current() {
        PipeHolderBlockEntity holder = holder();
        return holder == null ? -1 : holder.emzuliCurrent();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= PRESETS * 2) return false;
        PipeHolderBlockEntity holder = holder();
        if (holder == null) return false;
        holder.cycleEmzuliColor(id % PRESETS, id >= PRESETS);
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < PRESETS && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
            PipeHolderBlockEntity holder = holder();
            if (holder != null) {
                ItemStack carried = getCarried();
                holder.setEmzuliFilter(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
                refresh();
            }
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override public boolean canDragTo(Slot slot) { return !slot.isFake() && super.canDragTo(slot); }
    @Override public ItemStack quickMoveStack(Player player, int slotId) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return holder() != null && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    private static final class FilterSlot extends Slot {
        private FilterSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean isFake() { return true; }
    }
}
