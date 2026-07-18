package buildcraft.transport;

import net.minecraft.util.StringRepresentable;

public enum PipeType implements StringRepresentable {
    STRUCTURE("structure", "pipe_structure", "Structure Pipe"),
    COBBLESTONE_ITEM("cobblestone_item", "pipe_cobble_item", "Cobblestone Transport Pipe"),
    STONE_ITEM("stone_item", "pipe_stone_item", "Stone Transport Pipe"),
    QUARTZ_ITEM("quartz_item", "pipe_quartz_item", "Quartz Transport Pipe");

    public static final PipeType[] VALUES = values();
    private final String serializedName;
    private final String itemId;
    private final String englishName;

    PipeType(String serializedName, String itemId, String englishName) {
        this.serializedName = serializedName;
        this.itemId = itemId;
        this.englishName = englishName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public String itemId() {
        return itemId;
    }

    public String englishName() {
        return englishName;
    }

    public boolean connectsTo(PipeType other) {
        return this == other;
    }

    public boolean carriesItems() {
        return this != STRUCTURE;
    }
}
