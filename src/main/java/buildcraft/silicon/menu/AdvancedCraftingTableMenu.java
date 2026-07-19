package buildcraft.silicon.menu;

import buildcraft.silicon.BCSiliconBlocks;
import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.block.entity.AdvancedCraftingTableBlockEntity;
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
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public final class AdvancedCraftingTableMenu extends AbstractContainerMenu {
    private static final int BLUEPRINT_SLOTS = 9;
    private static final int MACHINE_SLOTS = 33;
    private final BlockPos pos;
    private final AdvancedCraftingTableBlockEntity table;
    private final SimpleContainer blueprintView = new SimpleContainer(BLUEPRINT_SLOTS);
    private final ContainerData data;

    public AdvancedCraftingTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }
    public AdvancedCraftingTableMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCSiliconMenus.ADVANCED_CRAFTING_TABLE.get(), id);
        this.pos = pos.immutable();
        table = inventory.player.level().getBlockEntity(pos) instanceof AdvancedCraftingTableBlockEntity entity
            ? entity : null;
        for (int slot = 0; slot < BLUEPRINT_SLOTS; slot++) {
            if (table != null) {
                ItemResource resource = table.blueprint().getResource(slot);
                if (!resource.isEmpty()) blueprintView.setItem(slot, resource.toStack(1));
            }
            addSlot(new Slot(blueprintView, slot, 33 + slot % 3 * 18, 16 + slot / 3 * 18) {
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        if (table != null) {
            for (int slot = 0; slot < 15; slot++) addSlot(new ResourceHandlerSlot(
                table.materials(), table.materials()::set, slot, 15 + slot % 5 * 18, 85 + slot / 5 * 18));
            for (int slot = 0; slot < 9; slot++) addSlot(new ResourceHandlerSlot(
                table.results(), table.results()::set, slot, 109 + slot % 3 * 18, 85 + slot / 3 * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
            data = new ContainerData() {
                @Override public int get(int index) {
                    return (int) Math.min(1_000, table.storedLaserPower() * 1_000
                        / AdvancedCraftingTableBlockEntity.POWER_REQUIRED);
                }
                @Override public void set(int index, int value) {}
                @Override public int getCount() { return 1; }
            };
        } else {
            Container empty = new SimpleContainer(24);
            for (int slot = 0; slot < 24; slot++) addSlot(new Slot(empty, slot, 15, 85));
            data = new SimpleContainerData(1);
        }
        addDataSlots(data);
        addPlayerInventory(inventory);
    }
    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("block.buildcraftsilicon.advanced_crafting_table");
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new AdvancedCraftingTableMenu(id, inventory, pos);
            }
        }, buffer -> buffer.writeBlockPos(pos));
    }
    @Override public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < BLUEPRINT_SLOTS && table != null) {
            ItemStack template = getCarried().isEmpty() ? ItemStack.EMPTY : getCarried().copyWithCount(1);
            blueprintView.setItem(slotId, template);
            table.blueprint().set(slotId, template.isEmpty() ? ItemResource.EMPTY : ItemResource.of(template),
                template.isEmpty() ? 0 : 1);
            table.setChanged();
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }
    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 153 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 211));
    }
    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        if (slotId < MACHINE_SLOTS) {
            if (!moveItemStackTo(moving, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(moving, BLUEPRINT_SLOTS, BLUEPRINT_SLOTS + 15, false)) return ItemStack.EMPTY;
        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving);
        return original;
    }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCSiliconBlocks.ADVANCED_CRAFTING_TABLE.get())
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
    public int progressThousandths() { return data.get(0); }
}
