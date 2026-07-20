package buildcraft.builders.menu;

import buildcraft.builders.BCBuildersMenus;
import buildcraft.builders.FillerPattern;
import buildcraft.builders.planner.FillerPlannerData;
import buildcraft.builders.planner.FillerPlannerSavedData;
import buildcraft.core.marker.VolumeBoxSavedData;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class FillerPlannerMenu extends AbstractContainerMenu {
    private final UUID boxId;
    private FillerPlannerData data;

    public FillerPlannerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readUUID(), readData(buffer));
    }
    private FillerPlannerMenu(int id, Inventory inventory, UUID boxId, FillerPlannerData data) {
        super(BCBuildersMenus.FILLER_PLANNER.get(), id); this.boxId = boxId; this.data = data;
    }
    public static void open(ServerPlayer player, UUID boxId) {
        FillerPlannerData data = FillerPlannerSavedData.get(player.level()).get(boxId).orElse(FillerPlannerData.defaults());
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("item.buildcraftbuilders.filler_planner"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player owner) { return new FillerPlannerMenu(id, inventory, boxId, data); }
        }, buffer -> { buffer.writeUUID(boxId); writeData(buffer, data); });
    }
    public FillerPlannerData data() { return data; }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (id >= 10 && id < 10 + FillerPattern.values().length) data = data.pattern(FillerPattern.values()[id - 10]);
        else if (id == 30) data = data.vertical(data.verticalDirection() == Direction.UP ? Direction.DOWN : Direction.UP);
        else if (id == 31) data = data.horizontal(data.horizontalDirection().getClockWise());
        else if (id == 32) data = data.hollow(!data.hollow());
        else if (id == 33) data = data.sphereFacing(Direction.values()[(data.sphereFacing().ordinal() + 1) % Direction.values().length]);
        else if (id == 34) data = data.sphereRotation(data.sphereRotation() + 1);
        else if (id == 35) data = data.shapeAxis(Direction.Axis.values()[(data.shapeAxis().ordinal() + 1) % Direction.Axis.values().length]);
        else if (id == 36) data = data.shapeRotation(data.shapeRotation() + 1);
        else if (id == 37) data = data.center((data.pyramidCenter() + 1) % 9);
        else if (id == 38) data = data.inverted(!data.inverted());
        else return false;
        if (!player.level().isClientSide() && player.level() instanceof net.minecraft.server.level.ServerLevel server) FillerPlannerSavedData.get(server).set(boxId, data);
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        if (player.level().isClientSide()) return true;
        return player.level() instanceof net.minecraft.server.level.ServerLevel server && VolumeBoxSavedData.get(server).boxes().stream()
            .anyMatch(box -> box.id().equals(boxId) && box.bounds().distanceToSqr(player.position()) <= 64);
    }
    private static void writeData(RegistryFriendlyByteBuf b, FillerPlannerData d) {
        b.writeByte(d.pattern().ordinal()); b.writeBoolean(d.inverted()); b.writeByte(d.verticalDirection().ordinal());
        b.writeByte(d.horizontalDirection().ordinal()); b.writeByte(d.pyramidCenter()); b.writeBoolean(d.hollow());
        b.writeByte(d.sphereFacing().ordinal()); b.writeByte(d.sphereRotation()); b.writeByte(d.shapeAxis().ordinal()); b.writeByte(d.shapeRotation());
    }
    private static FillerPlannerData readData(RegistryFriendlyByteBuf b) {
        return new FillerPlannerData(FillerPattern.values()[b.readUnsignedByte()], b.readBoolean(), Direction.values()[b.readUnsignedByte()],
            Direction.values()[b.readUnsignedByte()], b.readUnsignedByte(), b.readBoolean(), Direction.values()[b.readUnsignedByte()],
            b.readUnsignedByte(), Direction.Axis.values()[b.readUnsignedByte()], b.readUnsignedByte());
    }
}
