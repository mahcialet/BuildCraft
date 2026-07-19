package buildcraft.builders.menu;

import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.BCBuildersItems;
import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.block.entity.ReplacerBlockEntity;
import buildcraft.builders.snapshot.SnapshotKind;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public final class ReplacerMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private static final int MACHINE_SLOTS = 3;
    public ReplacerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos()); }
    public ReplacerMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCBuildersMenus.REPLACER.get(), id); this.pos = pos.immutable();
        ReplacerBlockEntity replacer = inventory.player.level().getBlockEntity(pos) instanceof ReplacerBlockEntity found ? found : null;
        if (replacer != null) {
            addSlot(new ResourceHandlerSlot(replacer.inventory(), replacer.inventory()::set, 0, 80, 20) {
                @Override public boolean mayPlace(ItemStack stack) {
                    var snapshot = stack.get(BCBuildersDataComponents.SNAPSHOT.get());
                    return snapshot != null && snapshot.kind() == SnapshotKind.BLUEPRINT;
                }
            });
            for (int slot = 1; slot < 3; slot++) addSlot(new ResourceHandlerSlot(
                    replacer.inventory(), replacer.inventory()::set, slot, slot == 1 ? 53 : 107, 52) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return stack.is(BCBuildersItems.SINGLE_SCHEMATIC.get())
                            && stack.has(BCBuildersDataComponents.SCHEMATIC_STATE.get());
                }
            });
        } else {
            Container empty = new SimpleContainer(3);
            addSlot(new Slot(empty, 0, 80, 20)); addSlot(new Slot(empty, 1, 53, 52)); addSlot(new Slot(empty, 2, 107, 52));
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 86 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 144));
    }
    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("block.buildcraftbuilders.replacer"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ReplacerMenu(id, inventory, pos); }
        }, buffer -> buffer.writeBlockPos(pos));
    }
    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem(); ItemStack moving = original.copy();
        if (slotId < MACHINE_SLOTS) { if (!moveItemStackTo(moving, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY; }
        else if (moving.has(BCBuildersDataComponents.SNAPSHOT.get())) { if (!moveItemStackTo(moving, 0, 1, false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(moving, 1, 3, false)) return ItemStack.EMPTY;
        if (moving.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player, moving); return original;
    }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCBuildersBlocks.REPLACER.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
}
