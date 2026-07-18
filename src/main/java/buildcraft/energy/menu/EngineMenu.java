package buildcraft.energy.menu;

import buildcraft.api.enums.EnumEngineType;
import buildcraft.api.mj.MjAPI;
import buildcraft.core.BCCoreBlocks;
import buildcraft.core.block.BlockEngine;
import buildcraft.energy.BCEnergyMenus;
import buildcraft.energy.block.entity.CombustionEngineBlockEntity;
import buildcraft.energy.block.entity.RfEngineBlockEntity;
import buildcraft.energy.block.entity.StirlingEngineBlockEntity;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

/** Shared server-authoritative menu for Energy engine variants. */
public final class EngineMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 8;
    private final Inventory inventory;
    private final BlockPos pos;
    private final EngineKind kind;
    private final ContainerData data;
    private final int engineSlots;

    public EngineMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }

    public EngineMenu(int id, Inventory inventory, BlockPos pos) {
        super(BCEnergyMenus.ENGINE.get(), id);
        this.inventory = inventory;
        this.pos = pos.immutable();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        kind = EngineKind.of(blockEntity);
        data = new EngineData(blockEntity, kind);

        int count = 0;
        if (blockEntity instanceof StirlingEngineBlockEntity stirling) {
            addSlot(new ResourceHandlerSlot(stirling.fuelInventory(), stirling.fuelInventory()::set,
                0, 80, 35));
            count = 1;
        } else if (blockEntity instanceof RfEngineBlockEntity rf) {
            for (int slot = 0; slot < 4; slot++) {
                addSlot(new ResourceHandlerSlot(rf.upgrades(), rf.upgrades()::set,
                    slot, 53 + slot * 18, 35));
            }
            count = 4;
        }
        engineSlots = count;
        addPlayerInventory(inventory, 8, 84);
        addDataSlots(data);
    }

    public static MenuProvider provider(BlockPos pos) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.buildcraftenergy.engine");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                return new EngineMenu(id, inventory, pos);
            }
        };
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        player.openMenu(provider(pos), buffer -> buffer.writeBlockPos(pos));
    }

    private void addPlayerInventory(Inventory inventory, int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, left + column * 18, top + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack moving = original.copy();
        if (slotId < engineSlots) {
            if (!moveItemStackTo(moving, engineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (engineSlots == 0 || !moveItemStackTo(moving, 0, engineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (moving.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, moving);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(BCCoreBlocks.ENGINE.get())
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    public EngineKind kind() { return kind; }
    public int fuelOrEnergy() { return data.get(0); }
    public int secondary() { return data.get(1); }
    public int residue() { return data.get(2); }
    public int heatHundredths() { return data.get(3); }
    public int storedMjHundredths() { return data.get(4); }
    public int outputMjHundredths() { return data.get(5); }
    public int burnTime() { return data.get(6); }
    public int burnTotal() { return data.get(7); }

    public enum EngineKind {
        STIRLING, COMBUSTION, RF, UNKNOWN;

        static EngineKind of(BlockEntity entity) {
            if (entity instanceof StirlingEngineBlockEntity) return STIRLING;
            if (entity instanceof CombustionEngineBlockEntity) return COMBUSTION;
            if (entity instanceof RfEngineBlockEntity) return RF;
            return UNKNOWN;
        }
    }

    private record EngineData(BlockEntity engine, EngineKind kind) implements ContainerData {
        @Override
        public int get(int index) {
            return switch (kind) {
                case STIRLING -> stirling(index, (StirlingEngineBlockEntity) engine);
                case COMBUSTION -> combustion(index, (CombustionEngineBlockEntity) engine);
                case RF -> rf(index, (RfEngineBlockEntity) engine);
                case UNKNOWN -> 0;
            };
        }

        private static int stirling(int index, StirlingEngineBlockEntity engine) {
            return switch (index) {
                case 3 -> 2_000;
                case 4 -> mjHundredths(engine.storedPower());
                case 5 -> 100;
                case 6 -> engine.burnTime();
                case 7 -> engine.totalBurnTime();
                default -> 0;
            };
        }

        private static int combustion(int index, CombustionEngineBlockEntity engine) {
            return switch (index) {
                case 0 -> engine.tanks().getAmountAsInt(CombustionEngineBlockEntity.FUEL_TANK);
                case 1 -> engine.tanks().getAmountAsInt(CombustionEngineBlockEntity.COOLANT_TANK);
                case 2 -> engine.tanks().getAmountAsInt(CombustionEngineBlockEntity.RESIDUE_TANK);
                case 3 -> (int) Math.round(engine.heat() * 100);
                case 4 -> mjHundredths(engine.storedPower());
                case 5 -> 0;
                case 6 -> (int) Math.ceil(engine.burnTime());
                default -> 0;
            };
        }

        private static int rf(int index, RfEngineBlockEntity engine) {
            return switch (index) {
                case 0 -> engine.energy().getAmountAsInt();
                case 3 -> (int) Math.round(engine.heat() * 100);
                case 4 -> mjHundredths(engine.storedPower());
                case 5 -> mjHundredths(engine.mjPerTick());
                default -> 0;
            };
        }

        private static int mjHundredths(long microJoules) {
            return (int) Math.min(Integer.MAX_VALUE, microJoules / (MjAPI.MJ / 100));
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    }
}
