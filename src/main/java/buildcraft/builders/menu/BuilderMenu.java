package buildcraft.builders.menu;

import buildcraft.api.core.IControllable.ControlMode;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.block.entity.BuilderBlockEntity;
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
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public final class BuilderMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 28;
    private final BlockPos pos;
    private final BuilderBlockEntity builder;
    private final int[] synced = new int[5];
    private final ContainerData data;

    public BuilderMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) { this(id, inventory, buffer.readBlockPos()); }
    public BuilderMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCBuildersMenus.BUILDER.get(), id);
        this.pos = pos.immutable();
        builder = inventory.player.level().getBlockEntity(pos) instanceof BuilderBlockEntity found ? found : null;
        boolean server = !inventory.player.level().isClientSide();
        data = new ContainerData() {
            @Override public int get(int index) {
                if (server && builder != null) return switch (index) {
                    case 0 -> builder.controlMode().ordinal(); case 1 -> builder.rotation().ordinal();
                    case 2 -> builder.canExcavate() ? 1 : 0; case 3 -> builder.cursor(); default -> builder.volumeSize();
                };
                return synced[index];
            }
            @Override public void set(int index, int value) { synced[index] = value; }
            @Override public int getCount() { return 5; }
        };
        addDataSlots(data);
        if (builder != null) {
            addSlot(new ResourceHandlerSlot(builder.inventory(), builder.inventory()::set, 0, 80, 14) {
                @Override public boolean mayPlace(ItemStack stack) { return buildcraft.builders.item.SnapshotItem.hasSnapshot(stack); }
            });
            for (int slot = 1; slot < MACHINE_SLOTS; slot++) addSlot(new ResourceHandlerSlot(
                    builder.inventory(), builder.inventory()::set, slot, 8 + (slot - 1) % 9 * 18, 36 + (slot - 1) / 9 * 18));
        } else {
            Container empty = new SimpleContainer(MACHINE_SLOTS);
            addSlot(new Slot(empty, 0, 80, 14));
            for (int slot = 1; slot < MACHINE_SLOTS; slot++) addSlot(new Slot(empty, slot,
                    8 + (slot - 1) % 9 * 18, 36 + (slot - 1) / 9 * 18));
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 110 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 168));
    }
    public ControlMode mode() { return ControlMode.values()[Math.floorMod(data.get(0), ControlMode.values().length)]; }
    public Rotation rotation() { return Rotation.values()[Math.floorMod(data.get(1), Rotation.values().length)]; }
    public boolean canExcavate() { return data.get(2) != 0; }
    public int progress() { return data.get(3); }
    public int progressMaximum() { return data.get(4); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (builder == null) return false;
        if (id >= 0 && id < ControlMode.values().length) { builder.setControlMode(ControlMode.values()[id]); return true; }
        if (id == 10) { builder.setRotation(Rotation.values()[(builder.rotation().ordinal() + 1) % Rotation.values().length]); return true; }
        if (id == 11) { builder.setCanExcavate(!builder.canExcavate()); return true; }
        return false;
    }
    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("block.buildcraftbuilders.builder"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new BuilderMenu(id, inventory, pos); }
        }, buffer -> buffer.writeBlockPos(pos));
    }
    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem(); ItemStack moving = original.copy();
        if (slotId < MACHINE_SLOTS) { if (!moveItemStackTo(moving, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY; }
        else if (buildcraft.builders.item.SnapshotItem.hasSnapshot(moving)) { if (!moveItemStackTo(moving, 0, 1, false)) return ItemStack.EMPTY; }
        else if (!moveItemStackTo(moving, 1, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        if (moving.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player, moving); return original;
    }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCBuildersBlocks.BUILDER.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
}
