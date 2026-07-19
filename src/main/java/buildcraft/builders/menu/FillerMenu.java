package buildcraft.builders.menu;

import buildcraft.api.core.IControllable.ControlMode;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.FillerPattern;
import buildcraft.builders.block.entity.FillerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public final class FillerMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 27;
    private final BlockPos pos;
    private final FillerBlockEntity filler;

    public FillerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    public FillerMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCBuildersMenus.FILLER.get(), id);
        this.pos = pos.immutable();
        this.filler = inventory.player.level().getBlockEntity(pos) instanceof FillerBlockEntity entity
                ? entity : null;
        if (filler != null) {
            for (int slot = 0; slot < MACHINE_SLOTS; slot++) {
                addSlot(new ResourceHandlerSlot(filler.resources(), filler.resources()::set,
                        slot, 8 + slot % 9 * 18, 18 + slot / 9 * 18));
            }
        } else {
            Container empty = new SimpleContainer(MACHINE_SLOTS);
            for (int slot = 0; slot < MACHINE_SLOTS; slot++) {
                addSlot(new Slot(empty, slot, 8 + slot % 9 * 18, 18 + slot / 9 * 18));
            }
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 86 + row * 18));
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 144));
        }
    }

    public static MenuProvider provider(BlockPos pos) {
        return new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("block.buildcraftbuilders.filler");
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new FillerMenu(id, inventory, pos);
            }
        };
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(provider(pos), buffer -> buffer.writeBlockPos(pos));
    }

    public FillerBlockEntity filler() { return filler; }
    public ControlMode mode() { return filler == null ? ControlMode.ON : filler.controlMode(); }
    public FillerPattern pattern() { return filler == null ? FillerPattern.FILL : filler.pattern(); }
    public net.minecraft.core.Direction verticalDirection() {
        return filler == null ? net.minecraft.core.Direction.UP : filler.verticalDirection();
    }
    public net.minecraft.core.Direction horizontalDirection() {
        return filler == null ? net.minecraft.core.Direction.EAST : filler.horizontalDirection();
    }
    public boolean hollow() { return filler != null && filler.hollow(); }
    public net.minecraft.core.Direction sphereFacing() {
        return filler == null ? net.minecraft.core.Direction.DOWN : filler.sphereFacing();
    }
    public int sphereRotation() { return filler == null ? 0 : filler.sphereRotation(); }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (filler == null) return false;
        if (id >= 0 && id < ControlMode.values().length) {
            filler.setControlMode(ControlMode.values()[id]);
            return true;
        }
        int pattern = id - 10;
        if (pattern >= 0 && pattern < FillerPattern.values().length) {
            filler.setPattern(FillerPattern.values()[pattern]);
            return true;
        }
        if (id == 30) {
            filler.setVerticalDirection(filler.verticalDirection() == net.minecraft.core.Direction.UP
                    ? net.minecraft.core.Direction.DOWN : net.minecraft.core.Direction.UP);
            return true;
        }
        if (id == 31) {
            filler.setHorizontalDirection(filler.horizontalDirection().getClockWise());
            return true;
        }
        if (id == 32) {
            filler.setHollow(!filler.hollow());
            return true;
        }
        if (id == 33) {
            Direction[] directions = Direction.values();
            filler.setSphereFacing(directions[(filler.sphereFacing().ordinal() + 1) % directions.length]);
            return true;
        }
        if (id == 34) {
            filler.setSphereRotation(filler.sphereRotation() + 1);
            return true;
        }
        return false;
    }

    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        if (slotId < MACHINE_SLOTS) {
            if (!moveItemStackTo(moving, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(moving, 0, MACHINE_SLOTS, false)) return ItemStack.EMPTY;
        if (moving.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, moving);
        return original;
    }

    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCBuildersBlocks.FILLER.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
}
