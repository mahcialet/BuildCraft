package buildcraft.builders.block.entity;

import buildcraft.builders.BCBuildersBlockEntities;
import buildcraft.builders.BCBuildersDataComponents;
import buildcraft.builders.snapshot.SnapshotData;
import buildcraft.builders.snapshot.SnapshotKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.ArrayList;
import java.util.List;

public final class ReplacerBlockEntity extends BlockEntity {
    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(3);

    public ReplacerBlockEntity(BlockPos pos, BlockState state) { super(BCBuildersBlockEntities.REPLACER.get(), pos, state); }
    public ItemStacksResourceHandler inventory() { return inventory; }

    public static void tick(Level level, BlockPos pos, BlockState state, ReplacerBlockEntity replacer) {
        if (level.isClientSide()) return;
        replacer.replace();
    }
    private void replace() {
        if (inventory.getAmountAsLong(0) <= 0 || inventory.getAmountAsLong(1) <= 0 || inventory.getAmountAsLong(2) <= 0) return;
        ItemStack blueprintStack = inventory.getResource(0).toStack(1);
        SnapshotData blueprint = level == null ? null
            : buildcraft.builders.item.SnapshotItem.resolve(blueprintStack, level);
        BlockState from = inventory.getResource(1).toStack(1).get(BCBuildersDataComponents.SCHEMATIC_STATE.get());
        BlockState to = inventory.getResource(2).toStack(1).get(BCBuildersDataComponents.SCHEMATIC_STATE.get());
        if (blueprint == null || !blueprint.valid() || blueprint.kind() != SnapshotKind.BLUEPRINT || from == null || to == null) return;
        List<BlockState> palette = new ArrayList<>(blueprint.palette());
        boolean changed = false;
        for (int index = 0; index < palette.size(); index++) {
            if (palette.get(index).equals(from)) { palette.set(index, to); changed = true; }
        }
        if (!changed) return;
        SnapshotData replaced = new SnapshotData(blueprint.kind(), blueprint.size(), blueprint.facing(),
                blueprint.offset(), palette, blueprint.blocks(), blueprint.name(), blueprint.rotate(),
                blueprint.excavate(), blueprint.allowCreative(), blueprint.creativeOnly());
        if (level instanceof net.minecraft.server.level.ServerLevel server
                && replaced.blocks().size() > buildcraft.builders.BCBuildersConfig.BLUEPRINT_EXTERNAL_THRESHOLD.get()) {
            buildcraft.builders.item.SnapshotItem.applyExternal(blueprintStack, replaced, server);
        } else {
            buildcraft.builders.item.SnapshotItem.apply(blueprintStack, replaced);
        }
        inventory.set(0, ItemResource.of(blueprintStack), 1);
        inventory.set(1, ItemResource.EMPTY, 0);
        inventory.set(2, ItemResource.EMPTY, 0);
        setChanged();
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input.childOrEmpty("inventory"));
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
    }
}
