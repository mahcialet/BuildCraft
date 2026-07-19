package buildcraft.builders.menu;

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.block.entity.BlueprintLibraryBlockEntity;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public final class BlueprintLibraryMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 4;
    private final BlockPos pos;
    private final BlueprintLibraryBlockEntity library;
    private final ContainerData data;
    public BlueprintLibraryMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }
    public BlueprintLibraryMenu(int id, Inventory playerInventory, BlockPos pos) {
        super(BCBuildersMenus.BLUEPRINT_LIBRARY.get(), id);
        this.pos = pos.immutable();
        library = playerInventory.player.level().getBlockEntity(pos) instanceof BlueprintLibraryBlockEntity found ? found : null;
        boolean server = !playerInventory.player.level().isClientSide();
        data = new ContainerData() {
            private final int[] synced = new int[4];
            @Override public int get(int index) {
                if (server && library != null) return switch (index) {
                    case 0 -> library.progressIn(); case 1 -> library.progressOut();
                    case 2 -> library.selected(); case 3 -> library.entryCount(); default -> 0;
                };
                return synced[index];
            }
            @Override public void set(int index, int value) { synced[index] = value; }
            @Override public int getCount() { return 4; }
        };
        addDataSlots(data);
        if (library != null) {
            addSlot(new ResourceHandlerSlot(library.inventory(), library.inventory()::set, 0, 35, 24) {
                @Override public boolean mayPlace(ItemStack stack) { return BlueprintLibraryBlockEntity.isStorable(stack); }
            });
            addSlot(new ResourceHandlerSlot(library.inventory(), library.inventory()::set, 1, 62, 24) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
            addSlot(new ResourceHandlerSlot(library.inventory(), library.inventory()::set, 2, 114, 24));
            addSlot(new ResourceHandlerSlot(library.inventory(), library.inventory()::set, 3, 141, 24) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        } else {
            Container empty = new SimpleContainer(4);
            addSlot(new Slot(empty, 0, 35, 24)); addSlot(new Slot(empty, 1, 62, 24));
            addSlot(new Slot(empty, 2, 114, 24)); addSlot(new Slot(empty, 3, 141, 24));
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 104 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(playerInventory, column, 8 + column * 18, 162));
    }
    public int progressIn() { return data.get(0); }
    public int progressOut() { return data.get(1); }
    public int selected() { return data.get(2); }
    public int entryCount() { return data.get(3); }
    public String selectedName() { return library == null ? "" : library.selectedName(); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (library == null) return false;
        return switch (id) {
            case 0 -> library.select(-1);
            case 1 -> library.select(1);
            case 2 -> library.deleteSelected();
            default -> false;
        };
    }
    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("block.buildcraftbuilders.blueprint_library"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new BlueprintLibraryMenu(id, inventory, pos);
            }
        }, buffer -> buffer.writeBlockPos(pos));
    }
    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem(); ItemStack moving = original.copy();
        if (slotId < MACHINE_SLOTS) {
            if (!moveItemStackTo(moving, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (BlueprintLibraryBlockEntity.isStorable(moving)) {
            if (!moveItemStackTo(moving, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(moving, 2, 3, false)) return ItemStack.EMPTY;
        if (moving.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, moving);
        return original;
    }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCBuildersBlocks.BLUEPRINT_LIBRARY.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
}
