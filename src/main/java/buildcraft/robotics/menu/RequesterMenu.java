package buildcraft.robotics.menu;

import buildcraft.robotics.BCRoboticsBlocks;
import buildcraft.robotics.BCRoboticsMenus;
import buildcraft.robotics.block.entity.RequesterBlockEntity;
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
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public final class RequesterMenu extends AbstractContainerMenu {
    private static final int REQUEST_SLOTS = RequesterBlockEntity.SLOTS;
    private static final int MACHINE_SLOTS = RequesterBlockEntity.SLOTS;
    private final BlockPos pos;
    private final RequesterBlockEntity requester;
    private final SimpleContainer requestDisplay = new SimpleContainer(REQUEST_SLOTS);

    public RequesterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }
    public RequesterMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCRoboticsMenus.REQUESTER.get(), id);
        this.pos = pos.immutable();
        requester = inventory.player.level().getBlockEntity(pos) instanceof RequesterBlockEntity found ? found : null;
        for (int column = 0; column < 4; column++) for (int row = 0; row < 5; row++) {
            int index = column * 5 + row;
            requestDisplay.setItem(index, requester == null ? ItemStack.EMPTY : requester.requestTemplate(index));
            addSlot(new RequestSlot(requestDisplay, index, 9 + column * 18, 7 + row * 18));
        }
        if (requester != null) {
            for (int column = 0; column < 4; column++) for (int row = 0; row < 5; row++) {
                int index = column * 5 + row;
                addSlot(new ResourceHandlerSlot(requester.inventory(), requester.inventory()::set,
                        index, 117 + column * 18, 7 + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) {
                        ItemStack template = requester.requestTemplate(index);
                        return !template.isEmpty() && (template.getItem() instanceof buildcraft.api.items.IList list
                                ? list.matches(template, stack) : ItemStack.isSameItemSameComponents(template, stack));
                    }
                });
            }
        } else {
            Container empty = new SimpleContainer(MACHINE_SLOTS);
            for (int column = 0; column < 4; column++) for (int row = 0; row < 5; row++) {
                int index = column * 5 + row;
                addSlot(new Slot(empty, index, 117 + column * 18, 7 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 19 + column * 18, 101 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 19 + column * 18, 159));
    }
    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("block.buildcraftrobotics.requester"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new RequesterMenu(id, inventory, pos);
            }
        }, buffer -> buffer.writeBlockPos(pos));
    }
    @Override public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (slotId >= 0 && slotId < REQUEST_SLOTS && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
            ItemStack carried = getCarried();
            ItemStack template = carried.isEmpty() ? ItemStack.EMPTY : carried.copy();
            requestDisplay.setItem(slotId, template);
            if (requester != null) requester.setRequest(slotId, template);
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, input, player);
    }
    @Override public boolean canDragTo(Slot slot) { return !slot.isFake() && super.canDragTo(slot); }
    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasItem() || slotId < REQUEST_SLOTS) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        if (slotId < REQUEST_SLOTS + MACHINE_SLOTS) {
            if (!moveItemStackTo(moving, REQUEST_SLOTS + MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int machine = REQUEST_SLOTS; machine < REQUEST_SLOTS + MACHINE_SLOTS && !moving.isEmpty(); machine++) {
                if (slots.get(machine).mayPlace(moving)) moved |= moveItemStackTo(moving, machine, machine + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, moving);
        return original;
    }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCRoboticsBlocks.REQUESTER.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
    private static final class RequestSlot extends Slot {
        private RequestSlot(Container container, int index, int x, int y) { super(container, index, x, y); }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean isFake() { return true; }
    }
}
