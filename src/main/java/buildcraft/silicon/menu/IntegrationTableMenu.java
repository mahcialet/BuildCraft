package buildcraft.silicon.menu;

import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.block.entity.IntegrationTableBlockEntity;
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

public final class IntegrationTableMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 10;
    private final BlockPos pos;
    private final IntegrationTableBlockEntity table;
    private final ContainerData data;

    public IntegrationTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }
    public IntegrationTableMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCSiliconMenus.INTEGRATION_TABLE.get(), id);
        this.pos = pos.immutable();
        table = inventory.player.level().getBlockEntity(pos) instanceof IntegrationTableBlockEntity entity ? entity : null;
        if (table != null) {
            addSlot(new ResourceHandlerSlot(table.target(), table.target()::set, 0, 80, 49));
            int integrationSlot = 0;
            for (int row = 0; row < 3; row++) for (int column = 0; column < 3; column++) {
                if (row == 1 && column == 1) continue;
                addSlot(new ResourceHandlerSlot(table.integrations(), table.integrations()::set,
                        integrationSlot++, 44 + column * 18, 31 + row * 18));
            }
            addSlot(new ResourceHandlerSlot(table.result(), table.result()::set, 0, 138, 49) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
            data = new ContainerData() {
                @Override public int get(int index) {
                    return (int) Math.min(1_000, table.storedLaserPower() * 1_000
                            / IntegrationTableBlockEntity.POWER_REQUIRED);
                }
                @Override public void set(int index, int value) {}
                @Override public int getCount() { return 1; }
            };
        } else {
            SimpleContainer empty = new SimpleContainer(MACHINE_SLOTS);
            for (int slot = 0; slot < MACHINE_SLOTS; slot++) addSlot(new Slot(empty, slot, 8, 18));
            data = new SimpleContainerData(1);
        }
        addDataSlots(data);
        addPlayerInventory(inventory);
    }
    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("block.buildcraftsilicon.integration_table");
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new IntegrationTableMenu(id, inventory, pos);
            }
        }, buffer -> buffer.writeBlockPos(pos));
    }
    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 109 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 167));
    }
    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        if (slotId < MACHINE_SLOTS) {
            if (!moveItemStackTo(moving, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(moving, 0, 9, false)) return ItemStack.EMPTY;
        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving);
        return original;
    }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCSiliconBlocks.INTEGRATION_TABLE.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
    public int progressThousandths() { return data.get(0); }
}
