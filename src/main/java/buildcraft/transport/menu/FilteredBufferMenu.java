package buildcraft.transport.menu;

import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.block.entity.FilteredBufferBlockEntity;
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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public final class FilteredBufferMenu extends AbstractContainerMenu {
    private static final int FILTER_SLOTS = FilteredBufferBlockEntity.SLOTS;
    private static final int MACHINE_SLOTS = FilteredBufferBlockEntity.SLOTS;
    private final BlockPos pos;
    private final FilteredBufferBlockEntity buffer;
    private final SimpleContainer shownFilters = new SimpleContainer(FILTER_SLOTS);

    public FilteredBufferMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos());
    }

    public FilteredBufferMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(BCTransportMenus.FILTERED_BUFFER.get(), id);
        this.pos = pos.immutable();
        this.buffer = playerInventory.player.level().getBlockEntity(pos)
                instanceof FilteredBufferBlockEntity found ? found : null;
        for (int slot = 0; slot < FILTER_SLOTS; slot++) {
            shownFilters.setItem(slot, buffer == null ? ItemStack.EMPTY : buffer.filter(slot));
            addSlot(new FilterSlot(shownFilters, slot, 8 + slot * 18, 18));
        }
        if (buffer != null) {
            for (int slot = 0; slot < MACHINE_SLOTS; slot++) {
                addSlot(new ResourceHandlerSlot(buffer.inventory(), buffer.inventory()::set,
                        slot, 8 + slot * 18, 54));
            }
        } else {
            Container empty = new SimpleContainer(MACHINE_SLOTS);
            for (int slot = 0; slot < MACHINE_SLOTS; slot++) addSlot(new Slot(empty, slot, 8 + slot * 18, 54));
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 86 + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 144));
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("block.buildcrafttransport.filtered_buffer");
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new FilteredBufferMenu(id, inventory, pos);
            }
        }, data -> data.writeBlockPos(pos));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < FILTER_SLOTS
                && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
            if (buffer != null) {
                ItemStack carried = getCarried();
                buffer.setFilter(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
                shownFilters.setItem(slotId, buffer.filter(slotId));
            }
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    @Override public boolean canDragTo(Slot slot) { return !slot.isFake() && super.canDragTo(slot); }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasItem() || slotId < FILTER_SLOTS) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        if (slotId < FILTER_SLOTS + MACHINE_SLOTS) {
            if (!moveItemStackTo(moving, FILTER_SLOTS + MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int machine = FILTER_SLOTS; machine < FILTER_SLOTS + MACHINE_SLOTS && !moving.isEmpty(); machine++) {
                if (slots.get(machine).mayPlace(moving)) moved |= moveItemStackTo(moving, machine, machine + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, moving);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCTransportBlocks.FILTERED_BUFFER.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }

    private static final class FilterSlot extends Slot {
        private FilterSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean isFake() { return true; }
    }
}
