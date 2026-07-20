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

public final class DiamondWoodMenu extends AbstractContainerMenu {
    public static final int FILTERS = 9;
    private final Inventory inventory;
    private final BlockPos pos;
    private final SimpleContainer shown = new SimpleContainer(FILTERS);

    public DiamondWoodMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    public DiamondWoodMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCTransportMenus.DIAMOND_WOOD.get(), id);
        this.inventory = inventory;
        this.pos = pos;
        refresh();
        for (int index = 0; index < FILTERS; index++) {
            addSlot(new FilterSlot(shown, index, 8 + index * 18, 24));
        }
        addStandardInventorySlots(inventory, 8, 62);
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("gui.buildcrafttransport.diamond_wood");
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new DiamondWoodMenu(id, inventory, pos);
            }
        }, buffer -> buffer.writeBlockPos(pos));
    }

    private PipeHolderBlockEntity holder() {
        return inventory.player.level().getBlockEntity(pos) instanceof PipeHolderBlockEntity holder
            && (holder.pipeType() == PipeType.DIAMOND_WOOD_ITEM
                || holder.pipeType() == PipeType.DIAMOND_WOOD_FLUID) ? holder : null;
    }

    private void refresh() {
        PipeHolderBlockEntity holder = holder();
        if (holder == null) return;
        for (int index = 0; index < FILTERS; index++) shown.setItem(index, holder.diamondFilters().get(index));
    }

    public PipeHolderBlockEntity.DiamondFilterMode mode() {
        PipeHolderBlockEntity holder = holder();
        return holder == null ? PipeHolderBlockEntity.DiamondFilterMode.WHITE_LIST : holder.diamondFilterMode();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0) return false;
        PipeHolderBlockEntity holder = holder();
        if (holder == null) return false;
        var values = PipeHolderBlockEntity.DiamondFilterMode.values();
        holder.setDiamondFilterMode(values[(holder.diamondFilterMode().ordinal() + 1) % values.length]);
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < FILTERS && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
            PipeHolderBlockEntity holder = holder();
            if (holder != null) {
                ItemStack carried = getCarried();
                holder.setDiamondFilter(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
                if (player instanceof ServerPlayer serverPlayer
                    && holder.diamondFilters().stream().filter(filter -> !filter.isEmpty()).count() >= FILTERS - 2) {
                    buildcraft.core.AdvancementUtil.award(
                        serverPlayer, "buildcrafttransport:too_many_pipe_filters");
                }
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
