package buildcraft.factory.menu;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.BCFactoryMenus;
import buildcraft.factory.block.entity.AutoWorkbenchBlockEntity;
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
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public final class AutoWorkbenchMenu extends AbstractContainerMenu {
    private static final int BLUEPRINT_SLOTS = 9;
    private static final int MACHINE_SLOTS = 19;
    private final Inventory playerInventory;
    private final BlockPos pos;
    private final AutoWorkbenchBlockEntity workbench;
    private final SimpleContainer blueprintView = new SimpleContainer(BLUEPRINT_SLOTS);

    public AutoWorkbenchMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    public AutoWorkbenchMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCFactoryMenus.AUTO_WORKBENCH.get(), id);
        this.playerInventory = inventory;
        this.pos = pos.immutable();
        this.workbench = inventory.player.level().getBlockEntity(pos) instanceof AutoWorkbenchBlockEntity entity
            ? entity : null;
        for (int slot = 0; slot < BLUEPRINT_SLOTS; slot++) {
            if (workbench != null) {
                ItemResource resource = workbench.blueprint().getResource(slot);
                if (!resource.isEmpty()) blueprintView.setItem(slot, resource.toStack(1));
            }
            addSlot(new Slot(blueprintView, slot, 26 + slot % 3 * 18, 17 + slot / 3 * 18) {
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        if (workbench != null) {
            for (int slot = 0; slot < 9; slot++) {
                addSlot(new ResourceHandlerSlot(workbench.materials(), workbench.materials()::set,
                    slot, 80 + slot % 3 * 18, 17 + slot / 3 * 18));
            }
            addSlot(new ResourceHandlerSlot(workbench.result(), workbench.result()::set, 0, 152, 35) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        } else {
            Container empty = new SimpleContainer(10);
            for (int slot = 0; slot < 10; slot++) addSlot(new Slot(empty, slot, 80, 17));
        }
        addPlayerInventory(inventory);
    }

    public static MenuProvider provider(BlockPos pos) {
        return new MenuProvider() {
            @Override public Component getDisplayName() {
                return Component.translatable("block.buildcraftfactory.autoworkbench_item");
            }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new AutoWorkbenchMenu(id, inventory, pos);
            }
        };
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(provider(pos), buffer -> buffer.writeBlockPos(pos));
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < BLUEPRINT_SLOTS && workbench != null) {
            ItemStack template = getCarried().isEmpty() ? ItemStack.EMPTY : getCarried().copyWithCount(1);
            blueprintView.setItem(slotId, template);
            workbench.blueprint().set(slotId, template.isEmpty() ? ItemResource.EMPTY : ItemResource.of(template),
                template.isEmpty() ? 0 : 1);
            workbench.setChanged();
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
    }

    @Override public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        if (slotId < MACHINE_SLOTS) {
            if (!moveItemStackTo(moving, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(moving, BLUEPRINT_SLOTS, BLUEPRINT_SLOTS + 9, false)) {
            return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving);
        return original;
    }

    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCFactoryBlocks.AUTO_WORKBENCH.get())
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }
}
