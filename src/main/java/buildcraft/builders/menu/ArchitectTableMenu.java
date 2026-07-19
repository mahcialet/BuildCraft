package buildcraft.builders.menu;

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersItems;
import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.block.entity.ArchitectTableBlockEntity;
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

public final class ArchitectTableMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final ArchitectTableBlockEntity table;
    private final int[] synced = new int[2];
    private final ContainerData data;

    public ArchitectTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }
    public ArchitectTableMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCBuildersMenus.ARCHITECT_TABLE.get(), id);
        this.pos = pos.immutable();
        table = inventory.player.level().getBlockEntity(pos) instanceof ArchitectTableBlockEntity found ? found : null;
        boolean serverSide = !inventory.player.level().isClientSide();
        data = new ContainerData() {
            @Override public int get(int index) {
                if (serverSide && table != null) return index == 0 ? table.cursor() : table.volumeSize();
                return synced[index];
            }
            @Override public void set(int index, int value) { synced[index] = value; }
            @Override public int getCount() { return 2; }
        };
        addDataSlots(data);
        if (table != null) {
            addSlot(new ResourceHandlerSlot(table.inventory(), table.inventory()::set, 0, 53, 35) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return (stack.is(BCBuildersItems.BLUEPRINT.get()) || stack.is(BCBuildersItems.TEMPLATE.get()))
                            && !stack.has(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get());
                }
            });
            addSlot(new ResourceHandlerSlot(table.inventory(), table.inventory()::set, 1, 107, 35) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        } else {
            Container empty = new SimpleContainer(2);
            addSlot(new Slot(empty, 0, 53, 35));
            addSlot(new Slot(empty, 1, 107, 35));
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 86 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 144));
    }

    public int progress() { return data.get(0); }
    public int progressMaximum() { return data.get(1); }
    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("block.buildcraftbuilders.architect_table"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new ArchitectTableMenu(id, inventory, pos);
            }
        }, buffer -> buffer.writeBlockPos(pos));
    }
    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        if (slotId < 2) {
            if (!moveItemStackTo(moving, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(moving, 0, 1, false)) return ItemStack.EMPTY;
        if (moving.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, moving);
        return original;
    }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCBuildersBlocks.ARCHITECT_TABLE.get())
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
