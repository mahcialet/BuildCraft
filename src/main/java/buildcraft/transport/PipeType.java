package buildcraft.transport;

import net.minecraft.util.StringRepresentable;

public enum PipeType implements StringRepresentable {
    STRUCTURE("structure", "pipe_structure", "Structure Pipe"),
    COBBLESTONE_ITEM("cobblestone_item", "pipe_cobble_item", "Cobblestone Transport Pipe"),
    STONE_ITEM("stone_item", "pipe_stone_item", "Stone Transport Pipe"),
    QUARTZ_ITEM("quartz_item", "pipe_quartz_item", "Quartz Transport Pipe"),
    WOOD_ITEM("wood_item", "pipe_wood_item", "Wooden Transport Pipe"),
    GOLD_ITEM("gold_item", "pipe_gold_item", "Golden Transport Pipe"),
    IRON_ITEM("iron_item", "pipe_iron_item", "Iron Transport Pipe"),
    CLAY_ITEM("clay_item", "pipe_clay_item", "Clay Transport Pipe"),
    SANDSTONE_ITEM("sandstone_item", "pipe_sandstone_item", "Sandstone Transport Pipe"),
    VOID_ITEM("void_item", "pipe_void_item", "Void Transport Pipe"),
    OBSIDIAN_ITEM("obsidian_item", "pipe_obsidian_item", "Obsidian Transport Pipe"),
    LAPIS_ITEM("lapis_item", "pipe_lapis_item", "Lapis Transport Pipe");

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
        if (this == STRUCTURE || other == STRUCTURE) return this == other;
        if (this == WOOD_ITEM && other == WOOD_ITEM) return false;
        if (this == OBSIDIAN_ITEM && other == OBSIDIAN_ITEM) return false;
        if (isGeneralConnector() || other.isGeneralConnector()) return true;
        return this == other;
    }

    private boolean isGeneralConnector() {
        return this == WOOD_ITEM || this == GOLD_ITEM || this == IRON_ITEM || this == CLAY_ITEM
            || this == SANDSTONE_ITEM || this == VOID_ITEM || this == OBSIDIAN_ITEM || this == LAPIS_ITEM;
    }

    public boolean carriesItems() {
        return this != STRUCTURE;
    }

    public boolean connectsInventories() {
        return carriesItems() && this != SANDSTONE_ITEM;
    }
}
