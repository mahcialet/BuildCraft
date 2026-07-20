package buildcraft.silicon.menu;

import buildcraft.silicon.BCSiliconDataComponents;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconMenus;
import buildcraft.silicon.gate.GateAction;
import buildcraft.silicon.gate.GateLogic;
import buildcraft.silicon.gate.GateProgram;
import buildcraft.silicon.gate.GateRule;
import buildcraft.silicon.gate.GateTrigger;
import buildcraft.silicon.item.GateItem;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.block.entity.PipeHolderBlockEntity;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GateMenu extends AbstractContainerMenu {
    private static final int MAX_RULES = 8;
    private final BlockPos pos;
    private final Direction side;
    private final PipeHolderBlockEntity pipe;
    private final ContainerData data;

    public GateMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), Direction.from3DDataValue(buffer.readUnsignedByte()));
    }
    public GateMenu(int id, Inventory inventory, BlockPos pos, Direction side) {
        super(BCSiliconMenus.GATE.get(), id);
        this.pos = pos.immutable();
        this.side = side;
        pipe = inventory.player.level().getBlockEntity(pos) instanceof PipeHolderBlockEntity holder ? holder : null;
        data = new ContainerData() {
            @Override public int get(int index) {
                ItemStack gate = gate();
                if (gate.isEmpty()) return index < 2 ? 0 : -1;
                if (index == 0) return GateItem.slots(gate);
                if (index == 1) return gate.getOrDefault(
                        BCSiliconDataComponents.GATE_LOGIC.get(), GateLogic.AND).ordinal();
                int row = (index - 2) / 2;
                var rules = gate.getOrDefault(BCSiliconDataComponents.GATE_PROGRAM.get(), GateProgram.EMPTY).rules();
                if (row >= rules.size()) return -1;
                return (index & 1) == 0 ? rules.get(row).trigger().ordinal() : rules.get(row).action().ordinal();
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 2 + MAX_RULES * 2; }
        };
        addDataSlots(data);
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 128 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 186));
    }
    public static void open(ServerPlayer player, BlockPos pos, Direction side) {
        player.openMenu(new MenuProvider() {
            @Override public Component getDisplayName() { return Component.translatable("screen.buildcraftsilicon.gate"); }
            @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new GateMenu(id, inventory, pos, side);
            }
        }, buffer -> { buffer.writeBlockPos(pos); buffer.writeByte(side.get3DDataValue()); });
    }
    private ItemStack gate() {
        if (pipe == null) return ItemStack.EMPTY;
        ItemStack stack = pipe.attachment(side);
        return stack.is(BCSiliconItems.PLUG_GATE.get()) ? stack : ItemStack.EMPTY;
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        ItemStack gate = gate();
        boolean actionButton = id >= 16;
        int row = actionButton ? id - 16 : id / 2;
        if (gate.isEmpty() || row < 0 || row >= GateItem.slots(gate)) return false;
        var rules = new ArrayList<>(gate.getOrDefault(
                BCSiliconDataComponents.GATE_PROGRAM.get(), GateProgram.EMPTY).rules());
        if (actionButton) {
            while (rules.size() <= row) rules.add(new GateRule(GateTrigger.TRUE, GateAction.REDSTONE_OUTPUT));
            GateRule old = rules.get(row);
            GateAction next = GateAction.values()[(old.action().ordinal() + 1) % GateAction.values().length];
            rules.set(row, new GateRule(old.trigger(), next, old.actionSide()));
        } else if ((id & 1) == 1) {
            if (row >= rules.size()) return false;
            rules.remove(row);
        } else {
            while (rules.size() <= row) rules.add(new GateRule(GateTrigger.TRUE, GateAction.REDSTONE_OUTPUT));
            GateRule old = rules.get(row);
            GateTrigger next = GateTrigger.values()[(old.trigger().ordinal() + 1) % GateTrigger.values().length];
            rules.set(row, new GateRule(next, old.action(), old.actionSide()));
        }
        ItemStack updated = gate.copy();
        updated.set(BCSiliconDataComponents.GATE_PROGRAM.get(), new GateProgram(rules));
        pipe.setAttachment(side, updated);
        broadcastChanges();
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int slotId) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCTransportBlocks.PIPE_HOLDER.get())
                && !gate().isEmpty() && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64;
    }
    public int ruleSlots() { return data.get(0); }
    public GateLogic logic() { return GateLogic.values()[Math.clamp(data.get(1), 0, GateLogic.values().length - 1)]; }
    public GateTrigger trigger(int row) {
        int value = data.get(2 + row * 2);
        return value < 0 ? null : GateTrigger.values()[Math.clamp(value, 0, GateTrigger.values().length - 1)];
    }
    public GateAction action(int row) {
        int value = data.get(3 + row * 2);
        return value < 0 ? null : GateAction.values()[Math.clamp(value, 0, GateAction.values().length - 1)];
    }
}
