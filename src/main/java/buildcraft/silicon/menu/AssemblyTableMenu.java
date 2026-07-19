package buildcraft.silicon.menu;

import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.ChipsetType;
import buildcraft.silicon.block.entity.AssemblyTableBlockEntity;
import buildcraft.silicon.recipe.AssemblySelection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public final class AssemblyTableMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 12;
    private final BlockPos pos;
    private final AssemblyTableBlockEntity table;
    private final ContainerData data;

    public AssemblyTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    public AssemblyTableMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCSiliconMenus.ASSEMBLY_TABLE.get(), id);
        this.pos = pos.immutable();
        this.table = inventory.player.level().getBlockEntity(pos) instanceof AssemblyTableBlockEntity entity
            ? entity : null;
        if (table != null) {
            for (int slot = 0; slot < MACHINE_SLOTS; slot++) {
                addSlot(new ResourceHandlerSlot(table.inventory(), table.inventory()::set,
                    slot, 8 + slot % 4 * 18, 18 + slot / 4 * 18));
            }
            data = new ContainerData() {
                @Override public int get(int index) {
                    if (index == 0) return table.selection().ordinal();
                    long required = table.selectedRequiredPower();
                    return required <= 0 ? 0 : (int) Math.min(1_000, table.storedLaserPower() * 1_000 / required);
                }
                @Override public void set(int index, int value) {}
                @Override public int getCount() { return 2; }
            };
        } else {
            var empty = new SimpleContainer(MACHINE_SLOTS);
            for (int slot = 0; slot < MACHINE_SLOTS; slot++) addSlot(new Slot(empty, slot, 8, 18));
            data = new SimpleContainerData(2);
        }
        addDataSlots(data);
        addPlayerInventory(inventory);
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("block.buildcraftsilicon.assembly_table");
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new AssemblyTableMenu(id, inventory, pos);
            }
        }, buffer -> buffer.writeBlockPos(pos));
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (table == null || id < 0 || id >= AssemblySelection.values().length) return false;
        table.setSelection(AssemblySelection.values()[id]);
        return true;
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 98 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 156));
    }

    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        if (slotId < MACHINE_SLOTS) {
            if (!moveItemStackTo(moving, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(moving, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving);
        return original;
    }

    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCSiliconBlocks.ASSEMBLY_TABLE.get())
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
    public AssemblySelection selection() {
        return AssemblySelection.values()[Math.clamp(data.get(0), 0, AssemblySelection.values().length - 1)];
    }
    public ChipsetType selectedType() { return selection().chipset().orElse(ChipsetType.RED); }
    public int progressThousandths() { return data.get(1); }
}
