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
import net.minecraft.world.item.ItemStack;

public final class DiamondRouteMenu extends AbstractContainerMenu {
    public static final int FILTERS = 54;
    private final Inventory inventory;
    private final BlockPos pos;
    private final SimpleContainer shown = new SimpleContainer(FILTERS);

    public DiamondRouteMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    public DiamondRouteMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCTransportMenus.DIAMOND_ROUTE.get(), id);
        this.inventory = inventory;
        this.pos = pos;
        refresh();
        for (int direction = 0; direction < 6; direction++) {
            for (int column = 0; column < 9; column++) {
                int index = direction * 9 + column;
                addSlot(new FilterSlot(shown, index, 8 + column * 18, 18 + direction * 18));
            }
        }
        addStandardInventorySlots(inventory, 8, 140);
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("gui.buildcrafttransport.diamond_route");
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new DiamondRouteMenu(id, inventory, pos);
            }
        }, buffer -> buffer.writeBlockPos(pos));
    }

    private PipeHolderBlockEntity holder() {
        return inventory.player.level().getBlockEntity(pos) instanceof PipeHolderBlockEntity holder
            && (holder.pipeType() == PipeType.DIAMOND_ITEM || holder.pipeType() == PipeType.DIAMOND_FLUID)
            ? holder : null;
    }

    private void refresh() {
        PipeHolderBlockEntity holder = holder();
        if (holder == null) return;
        var filters = holder.diamondRouteFilters();
        for (int index = 0; index < FILTERS; index++) shown.setItem(index, filters.get(index));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < FILTERS && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
            PipeHolderBlockEntity holder = holder();
            if (holder != null) {
                ItemStack carried = getCarried();
                holder.setDiamondRouteFilter(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried);
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
