package buildcraft.robotics.block.entity;

import buildcraft.api.items.MapLocationType;
import buildcraft.api.items.PaintbrushData;
import buildcraft.core.BCCoreDataComponents;
import buildcraft.core.BCCoreItems;
import buildcraft.core.item.ItemPaintbrush;
import buildcraft.robotics.BCRoboticsBlockEntities;
import buildcraft.robotics.block.ZonePlannerBlock;
import buildcraft.robotics.zone.ZoneMapLocation;
import buildcraft.robotics.zone.ZonePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ZonePlannerBlockEntity extends BlockEntity {
    public static final int PROCESS_TIME = 200;
    public static final int BRUSH_SLOTS = 16;
    public static final int SLOT_INPUT_BRUSH = 16;
    public static final int SLOT_INPUT_MAP = 17;
    public static final int SLOT_INPUT_RESULT = 18;
    public static final int SLOT_OUTPUT_BRUSH = 19;
    public static final int SLOT_OUTPUT_MAP = 20;
    public static final int SLOT_OUTPUT_RESULT = 21;
    public static final int SLOTS = 22;
    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(SLOTS);
    private final ZonePlan[] layers = new ZonePlan[16];
    private int progressInput;
    private int progressOutput;
    private String mapName = "";
    private int previewTimer;
    private int[] preview = new int[80];

    public ZonePlannerBlockEntity(BlockPos pos, BlockState state) {
        super(BCRoboticsBlockEntities.ZONE_PLANNER.get(), pos, state);
        Arrays.setAll(layers, ignored -> new ZonePlan());
    }
    public ItemStacksResourceHandler inventory() { return inventory; }
    public ZonePlan layer(int index) { return new ZonePlan(layers[Math.floorMod(index, 16)]); }
    public int progressInput() { return progressInput; }
    public int progressOutput() { return progressOutput; }
    public String mapName() { return mapName; }
    public int[] preview() { return preview.clone(); }
    public void setMapName(String value) {
        String safe = value == null ? "" : value.strip();
        if (safe.length() > 32) safe = safe.substring(0, 32);
        if (!mapName.equals(safe)) { mapName = safe; sync(); }
    }
    public boolean edit(int layer, int minX, int minZ, int maxX, int maxZ, boolean selected) {
        if (layer < 0 || layer >= 16) return false;
        int x0 = Math.max(-1024, Math.min(minX, maxX));
        int z0 = Math.max(-1024, Math.min(minZ, maxZ));
        int x1 = Math.min(1023, Math.max(minX, maxX));
        int z1 = Math.min(1023, Math.max(minZ, maxZ));
        long cells = (long) (x1 - x0 + 1) * (z1 - z0 + 1);
        if (cells <= 0 || cells > 262_144) return false;
        for (int z = z0; z <= z1; z++) for (int x = x0; x <= x1; x++) layers[layer].set(x, z, selected);
        sync();
        return true;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, ZonePlannerBlockEntity planner) {
        if (!(level instanceof ServerLevel server)) return;
        planner.processInput();
        planner.processOutput();
        boolean active = planner.progressInput > 0 || planner.progressOutput > 0;
        if (state.hasProperty(ZonePlannerBlock.ACTIVE) && state.getValue(ZonePlannerBlock.ACTIVE) != active)
            level.setBlock(pos, state.setValue(ZonePlannerBlock.ACTIVE, active), Block.UPDATE_ALL);
        if (++planner.previewTimer >= 100) {
            planner.previewTimer = 0;
            planner.recalculatePreview(server);
        }
    }
    private void processInput() {
        ItemStack brush = stack(SLOT_INPUT_BRUSH);
        ItemStack source = stack(SLOT_INPUT_MAP);
        ZonePlan zone = ZoneMapLocation.get(source);
        DyeColor color = brushColor(brush);
        if (color == null || zone == null || mapType(source) != MapLocationType.ZONE || !stack(SLOT_INPUT_RESULT).isEmpty()) {
            if (progressInput != 0) { progressInput = 0; sync(); }
            return;
        }
        if (++progressInput < PROCESS_TIME) { setChanged(); return; }
        layers[color.getId()] = zone.translated(-worldPosition.getX(), -worldPosition.getZ());
        mapName = buildcraft.core.BCCoreItems.MAP_LOCATION.get().getStoredName(source);
        inventory.set(SLOT_INPUT_MAP, ItemResource.EMPTY, 0);
        inventory.set(SLOT_INPUT_RESULT, ItemResource.of(BCCoreItems.MAP_LOCATION.get()), 1);
        progressInput = 0;
        sync();
    }
    private void processOutput() {
        ItemStack brush = stack(SLOT_OUTPUT_BRUSH);
        ItemStack target = stack(SLOT_OUTPUT_MAP);
        DyeColor color = brushColor(brush);
        if (color == null || !target.is(BCCoreItems.MAP_LOCATION.get()) || mapType(target) != MapLocationType.CLEAN
                || !stack(SLOT_OUTPUT_RESULT).isEmpty()) {
            if (progressOutput != 0) { progressOutput = 0; sync(); }
            return;
        }
        if (++progressOutput < PROCESS_TIME) { setChanged(); return; }
        ItemStack result = target.copyWithCount(1);
        ZoneMapLocation.set(result, layers[color.getId()].translated(worldPosition.getX(), worldPosition.getZ()), mapName);
        inventory.set(SLOT_OUTPUT_MAP, ItemResource.EMPTY, 0);
        inventory.set(SLOT_OUTPUT_RESULT, ItemResource.of(result), 1);
        progressOutput = 0;
        sync();
    }
    private void recalculatePreview(ServerLevel level) {
        int[] next = new int[80];
        for (int row = 0; row < 8; row++) for (int column = 0; column < 10; column++) {
            int x = worldPosition.getX() + (column - 5) * 10 + 5;
            int z = worldPosition.getZ() + (row - 4) * 10 + 5;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            BlockPos sample = new BlockPos(x, y, z);
            next[row * 10 + column] = level.getBlockState(sample).getMapColor(level, sample)
                    .calculateARGBColor(MapColor.Brightness.NORMAL);
        }
        if (!Arrays.equals(preview, next)) { preview = next; sync(); }
    }
    public static DyeColor brushColor(ItemStack stack) {
        PaintbrushData data = stack.is(BCCoreItems.PAINTBRUSH.get()) ? ItemPaintbrush.data(stack) : null;
        return data == null ? null : data.color();
    }
    private static MapLocationType mapType(ItemStack stack) {
        return stack.getOrDefault(BCCoreDataComponents.MAP_LOCATION_TYPE.get(), MapLocationType.CLEAN);
    }
    private ItemStack stack(int slot) {
        long amount = inventory.getAmountAsLong(slot);
        return amount <= 0 ? ItemStack.EMPTY : inventory.getResource(slot).toStack((int) amount);
    }
    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input.childOrEmpty("inventory"));
        List<ZonePlan> storedLayers = input.read("layers", ZonePlan.CODEC.listOf(0, 16)).orElse(List.of());
        for (int i = 0; i < layers.length; i++) layers[i] = i < storedLayers.size()
                ? new ZonePlan(storedLayers.get(i)) : new ZonePlan();
        progressInput = Math.clamp(input.getIntOr("progress_input", 0), 0, PROCESS_TIME);
        progressOutput = Math.clamp(input.getIntOr("progress_output", 0), 0, PROCESS_TIME);
        mapName = input.getStringOr("map_name", "");
        List<Integer> colors = input.read("preview", com.mojang.serialization.Codec.INT.listOf(0, 80)).orElse(List.of());
        if (colors.size() == 80) preview = colors.stream().mapToInt(Integer::intValue).toArray();
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
        output.store("layers", ZonePlan.CODEC.listOf(), Arrays.asList(layers));
        if (progressInput > 0) output.putInt("progress_input", progressInput);
        if (progressOutput > 0) output.putInt("progress_output", progressOutput);
        if (!mapName.isEmpty()) output.putString("map_name", mapName);
        output.store("preview", com.mojang.serialization.Codec.INT.listOf(),
                Arrays.stream(preview).boxed().toList());
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
}
