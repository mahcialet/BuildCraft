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
    private final int[] synced = new int[5];
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
                if (serverSide && table != null) return switch (index) {
                    case 0 -> table.cursor();
                    case 1 -> table.volumeSize();
                    case 2 -> table.rotate() ? 1 : 0;
                    case 3 -> table.excavate() ? 1 : 0;
                    case 4 -> table.allowCreative() ? 1 : 0;
                    default -> 0;
                };
                return synced[index];
            }
            @Override public void set(int index, int value) { synced[index] = value; }
            @Override public int getCount() { return 5; }
        };
        addDataSlots(data);
        if (table != null) {
            addSlot(new ResourceHandlerSlot(table.inventory(), table.inventory()::set, 0, 135, 35) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return (stack.is(BCBuildersItems.BLUEPRINT.get()) || stack.is(BCBuildersItems.TEMPLATE.get()))
                            && !stack.has(buildcraft.builders.BCBuildersDataComponents.SNAPSHOT.get());
                }
            });
            addSlot(new ResourceHandlerSlot(table.inventory(), table.inventory()::set, 1, 194, 35) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        } else {
            Container empty = new SimpleContainer(2);
            addSlot(new Slot(empty, 0, 135, 35));
            addSlot(new Slot(empty, 1, 194, 35));
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 88 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 88 + column * 18, 142));
    }

    public int progress() { return data.get(0); }
    public int progressMaximum() { return data.get(1); }
    public boolean rotate() { return data.get(2) != 0; }
    public boolean excavate() { return data.get(3) != 0; }
    public boolean allowCreative() { return data.get(4) != 0; }
    public String blueprintName() { return table == null ? "Blueprint" : table.blueprintName(); }
    public void setBlueprintName(String value) { if (table != null) table.setBlueprintName(value); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (table == null || !stillValid(player)) return false;
        if (id == 0) table.toggleRotate();
        else if (id == 1) table.toggleExcavate();
        else if (id == 2) table.toggleAllowCreative();
        else return false;
        return true;
    }
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
