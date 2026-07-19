package buildcraft.robotics.menu;

import buildcraft.api.items.MapLocationType;
import buildcraft.core.BCCoreDataComponents;
import buildcraft.core.BCCoreItems;
import buildcraft.robotics.BCRoboticsBlocks;
import buildcraft.robotics.BCRoboticsMenus;
import buildcraft.robotics.block.entity.ZonePlannerBlockEntity;
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

public final class ZonePlannerMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final ZonePlannerBlockEntity planner;
    private final int[] synced = new int[2];
    private final ContainerData data;
    public ZonePlannerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }
    public ZonePlannerMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCRoboticsMenus.ZONE_PLANNER.get(), id);
        this.pos = pos.immutable();
        planner = inventory.player.level().getBlockEntity(pos) instanceof ZonePlannerBlockEntity found ? found : null;
        boolean server = !inventory.player.level().isClientSide();
        data = new ContainerData() {
            @Override public int get(int index) {
                if (server && planner != null) return index == 0 ? planner.progressInput() : planner.progressOutput();
                return synced[index];
            }
            @Override public void set(int index, int value) { synced[index] = value; }
            @Override public int getCount() { return 2; }
        };
        addDataSlots(data);
        if (planner != null) addMachineSlots(planner);
        else addEmptyMachineSlots();
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 88 + column * 18, 146 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 88 + column * 18, 204));
    }
    private void addMachineSlots(ZonePlannerBlockEntity planner) {
        for (int column = 0; column < 4; column++) for (int row = 0; row < 4; row++) {
            int index = column * 4 + row;
            addSlot(brushSlot(planner, index, 8 + column * 18, 146 + row * 18));
        }
        addSlot(brushSlot(planner, ZonePlannerBlockEntity.SLOT_INPUT_BRUSH, 8, 125));
        addSlot(mapSlot(planner, ZonePlannerBlockEntity.SLOT_INPUT_MAP, 26, 125, true));
        addSlot(outputSlot(planner, ZonePlannerBlockEntity.SLOT_INPUT_RESULT, 74, 125));
        addSlot(brushSlot(planner, ZonePlannerBlockEntity.SLOT_OUTPUT_BRUSH, 233, 9));
        addSlot(mapSlot(planner, ZonePlannerBlockEntity.SLOT_OUTPUT_MAP, 233, 27, false));
        addSlot(outputSlot(planner, ZonePlannerBlockEntity.SLOT_OUTPUT_RESULT, 233, 75));
    }
    private ResourceHandlerSlot brushSlot(ZonePlannerBlockEntity planner, int index, int x, int y) {
        return new ResourceHandlerSlot(planner.inventory(), planner.inventory()::set, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ZonePlannerBlockEntity.brushColor(stack) != null;
            }
        };
    }
    private ResourceHandlerSlot mapSlot(ZonePlannerBlockEntity planner, int index, int x, int y, boolean zone) {
        return new ResourceHandlerSlot(planner.inventory(), planner.inventory()::set, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                if (!stack.is(BCCoreItems.MAP_LOCATION.get())) return false;
                MapLocationType type = stack.getOrDefault(BCCoreDataComponents.MAP_LOCATION_TYPE.get(), MapLocationType.CLEAN);
                return zone ? type == MapLocationType.ZONE : type == MapLocationType.CLEAN;
            }
        };
    }
    private ResourceHandlerSlot outputSlot(ZonePlannerBlockEntity planner, int index, int x, int y) {
        return new ResourceHandlerSlot(planner.inventory(), planner.inventory()::set, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }
    private void addEmptyMachineSlots() {
        Container empty = new SimpleContainer(ZonePlannerBlockEntity.SLOTS);
        for (int column = 0; column < 4; column++) for (int row = 0; row < 4; row++)
            addSlot(new Slot(empty, column * 4 + row, 8 + column * 18, 146 + row * 18));
        addSlot(new Slot(empty, 16, 8, 125)); addSlot(new Slot(empty, 17, 26, 125));
        addSlot(new Slot(empty, 18, 74, 125)); addSlot(new Slot(empty, 19, 233, 9));
        addSlot(new Slot(empty, 20, 233, 27)); addSlot(new Slot(empty, 21, 233, 75));
    }
    public int progressInput() { return data.get(0); }
    public int progressOutput() { return data.get(1); }
    public String mapName() { return planner == null ? "" : planner.mapName(); }
    public int[] preview() { return planner == null ? new int[80] : planner.preview(); }
    public buildcraft.robotics.zone.ZonePlan layer(int index) {
        return planner == null ? new buildcraft.robotics.zone.ZonePlan() : planner.layer(index);
    }
    public void setMapName(String name) { if (planner != null) planner.setMapName(name); }
    public void edit(int layer, BlockPos from, BlockPos to, boolean selected) {
        if (planner != null) planner.edit(layer, from.getX(), from.getZ(), to.getX(), to.getZ(), selected);
    }
    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("block.buildcraftrobotics.zone_planner"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new ZonePlannerMenu(id, inventory, pos);
            }
        }, buffer -> buffer.writeBlockPos(pos));
    }
    @Override public ItemStack quickMoveStack(Player player, int slotId) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCRoboticsBlocks.ZONE_PLANNER.get())
                && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
}
