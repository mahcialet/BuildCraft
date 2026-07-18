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
    LAPIS_ITEM("lapis_item", "pipe_lapis_item", "Lapis Transport Pipe"),
    DAIZULI_ITEM("daizuli_item", "pipe_daizuli_item", "Daizuli Transport Pipe"),
    DIAMOND_WOOD_ITEM("diamond_wood_item", "pipe_diamond_wood_item", "Diamond Wooden Transport Pipe"),
    EMZULI_ITEM("emzuli_item", "pipe_emzuli_item", "Emzuli Transport Pipe"),
    DIAMOND_ITEM("diamond_item", "pipe_diamond_item", "Diamond Transport Pipe"),
    STRIPES_ITEM("stripes_item", "pipe_stripes_item", "Stripes Transport Pipe"),
    COBBLESTONE_FLUID("cobblestone_fluid", "pipe_cobble_fluid", "Cobblestone Fluid Pipe"),
    STONE_FLUID("stone_fluid", "pipe_stone_fluid", "Stone Fluid Pipe"),
    QUARTZ_FLUID("quartz_fluid", "pipe_quartz_fluid", "Quartz Fluid Pipe"),
    WOOD_FLUID("wood_fluid", "pipe_wood_fluid", "Wooden Fluid Pipe"),
    GOLD_FLUID("gold_fluid", "pipe_gold_fluid", "Golden Fluid Pipe"),
    SANDSTONE_FLUID("sandstone_fluid", "pipe_sandstone_fluid", "Sandstone Fluid Pipe");

    public static final PipeType[] VALUES = values();
    private final String serializedName;
    private final String itemId;
    private final String englishName;

    PipeType(String serializedName, String itemId, String englishName) {
        this.serializedName = serializedName;
        this.itemId = itemId;
        this.englishName = englishName;
    }

    @Override public String getSerializedName() { return serializedName; }
    public String itemId() { return itemId; }
    public String englishName() { return englishName; }

    public boolean connectsTo(PipeType other) {
        if (carriesFluids() != other.carriesFluids()) return false;
        if (carriesFluids()) {
            if (isWoodenFluidExtraction() && other.isWoodenFluidExtraction()) return false;
            return this == other || isWoodenFluidExtraction() || other.isWoodenFluidExtraction()
                || isGeneralFluidConnector() || other.isGeneralFluidConnector();
        }
        if (this == STRUCTURE || other == STRUCTURE) return this == other;
        if (isWoodenExtraction() && other.isWoodenExtraction()) return false;
        if (isWoodenExtraction() || other.isWoodenExtraction()) return true;
        if (this == OBSIDIAN_ITEM && other == OBSIDIAN_ITEM) return false;
        if (this == STRIPES_ITEM && other == STRIPES_ITEM) return false;
        return this == other || isGeneralConnector() || other.isGeneralConnector();
    }

    private boolean isGeneralConnector() {
        return this == WOOD_ITEM || this == GOLD_ITEM || this == IRON_ITEM || this == CLAY_ITEM
            || this == SANDSTONE_ITEM || this == VOID_ITEM || this == OBSIDIAN_ITEM || this == LAPIS_ITEM
            || this == DAIZULI_ITEM || this == DIAMOND_WOOD_ITEM || this == EMZULI_ITEM
            || this == DIAMOND_ITEM || this == STRIPES_ITEM;
    }

    private boolean isWoodenExtraction() {
        return this == WOOD_ITEM || this == DIAMOND_WOOD_ITEM || this == EMZULI_ITEM;
    }

    public boolean carriesItems() { return this != STRUCTURE && !carriesFluids(); }
    public boolean carriesFluids() {
        return this == COBBLESTONE_FLUID || this == STONE_FLUID || this == QUARTZ_FLUID
            || this == WOOD_FLUID || this == GOLD_FLUID || this == SANDSTONE_FLUID;
    }
    public boolean isWoodenFluidExtraction() { return this == WOOD_FLUID; }
    private boolean isGeneralFluidConnector() { return this == GOLD_FLUID || this == SANDSTONE_FLUID; }
    public boolean connectsFluidHandlers() { return carriesFluids() && this != SANDSTONE_FLUID; }
    public int fluidTransferRate() {
        return switch (this) {
            case COBBLESTONE_FLUID -> 10;
            case STONE_FLUID -> 20;
            case QUARTZ_FLUID -> 40;
            case WOOD_FLUID -> 10;
            case GOLD_FLUID -> 80;
            case SANDSTONE_FLUID -> 20;
            default -> 0;
        };
    }
    public boolean connectsInventories() { return carriesItems() && this != SANDSTONE_ITEM; }
}
