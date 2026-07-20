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
    SANDSTONE_FLUID("sandstone_fluid", "pipe_sandstone_fluid", "Sandstone Fluid Pipe"),
    IRON_FLUID("iron_fluid", "pipe_iron_fluid", "Iron Fluid Pipe"),
    CLAY_FLUID("clay_fluid", "pipe_clay_fluid", "Clay Fluid Pipe"),
    VOID_FLUID("void_fluid", "pipe_void_fluid", "Void Fluid Pipe"),
    DIAMOND_FLUID("diamond_fluid", "pipe_diamond_fluid", "Diamond Fluid Pipe"),
    DIAMOND_WOOD_FLUID("diamond_wood_fluid", "pipe_diamond_wood_fluid", "Diamond Wooden Fluid Pipe"),
    COBBLESTONE_POWER("cobblestone_power", "pipe_cobble_power", "Cobblestone Power Pipe"),
    STONE_POWER("stone_power", "pipe_stone_power", "Stone Power Pipe"),
    QUARTZ_POWER("quartz_power", "pipe_quartz_power", "Quartz Power Pipe"),
    WOOD_POWER("wood_power", "pipe_wood_power", "Wooden Power Pipe"),
    SANDSTONE_POWER("sandstone_power", "pipe_sandstone_power", "Sandstone Power Pipe"),
    IRON_POWER("iron_power", "pipe_iron_power", "Iron Power Pipe"),
    GOLD_POWER("gold_power", "pipe_gold_power", "Golden Power Pipe"),
    DIAMOND_POWER("diamond_power", "pipe_diamond_power", "Diamond Power Pipe"),
    DIAMOND_WOOD_POWER("diamond_wood_power", "pipe_diamond_wood_power", "Diamond Wooden Power Pipe"),
    COBBLESTONE_RF("cobblestone_rf", "pipe_cobble_rf", "Cobblestone RF Pipe"),
    STONE_RF("stone_rf", "pipe_stone_rf", "Stone RF Pipe"),
    QUARTZ_RF("quartz_rf", "pipe_quartz_rf", "Quartz RF Pipe"),
    WOOD_RF("wood_rf", "pipe_wood_rf", "Wooden RF Pipe"),
    SANDSTONE_RF("sandstone_rf", "pipe_sandstone_rf", "Sandstone RF Pipe"),
    IRON_RF("iron_rf", "pipe_iron_rf", "Iron RF Pipe"),
    GOLD_RF("gold_rf", "pipe_gold_rf", "Golden RF Pipe"),
    DIAMOND_RF("diamond_rf", "pipe_diamond_rf", "Diamond RF Pipe"),
    DIAMOND_WOOD_RF("diamond_wood_rf", "pipe_diamond_wood_rf", "Diamond Wooden RF Pipe");

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
        if (carriesRf() != other.carriesRf()) return false;
        if (carriesRf()) {
            if (isWoodenRfInput() && other.isWoodenRfInput()) return false;
            return this == other || isGeneralRfConnector() || other.isGeneralRfConnector();
        }
        if (carriesPower() != other.carriesPower()) return false;
        if (carriesPower()) {
            if (isWoodenPowerInput() && other.isWoodenPowerInput()) return false;
            return this == other || isGeneralPowerConnector() || other.isGeneralPowerConnector();
        }
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

    public boolean carriesItems() {
        return this != STRUCTURE && !carriesFluids() && !carriesPower() && !carriesRf();
    }
    public boolean carriesRf() {
        return this == COBBLESTONE_RF || this == STONE_RF || this == QUARTZ_RF || this == WOOD_RF
            || this == SANDSTONE_RF || this == IRON_RF || this == GOLD_RF || this == DIAMOND_RF
            || this == DIAMOND_WOOD_RF;
    }
    private boolean isGeneralRfConnector() {
        return this == WOOD_RF || this == SANDSTONE_RF || this == IRON_RF || this == GOLD_RF
            || this == DIAMOND_RF || this == DIAMOND_WOOD_RF;
    }
    public boolean isWoodenRfInput() { return this == WOOD_RF || this == DIAMOND_WOOD_RF; }
    public int rfTransferRate() {
        return switch (this) {
            case COBBLESTONE_RF -> 40;
            case STONE_RF -> 80;
            case WOOD_RF, SANDSTONE_RF -> 160;
            case QUARTZ_RF, IRON_RF -> 320;
            case GOLD_RF -> 1_280;
            case DIAMOND_RF, DIAMOND_WOOD_RF -> 2_560;
            default -> 0;
        };
    }
    public boolean connectsRfHandlers() { return carriesRf() && this != SANDSTONE_RF; }
    public boolean carriesPower() {
        return this == COBBLESTONE_POWER || this == STONE_POWER || this == QUARTZ_POWER || this == WOOD_POWER
            || this == SANDSTONE_POWER || this == IRON_POWER || this == GOLD_POWER
            || this == DIAMOND_POWER || this == DIAMOND_WOOD_POWER;
    }
    private boolean isGeneralPowerConnector() {
        return this == WOOD_POWER || this == SANDSTONE_POWER || this == IRON_POWER || this == GOLD_POWER
            || this == DIAMOND_POWER || this == DIAMOND_WOOD_POWER;
    }
    public boolean isWoodenPowerInput() { return this == WOOD_POWER || this == DIAMOND_WOOD_POWER; }
    public boolean isPowerLimiter() {
        return this == IRON_POWER || this == DIAMOND_POWER || this == IRON_RF || this == DIAMOND_RF;
    }
    public long powerTransferPerTick() {
        return switch (this) {
            case COBBLESTONE_POWER -> 4_000_000L;
            case STONE_POWER -> 8_000_000L;
            case QUARTZ_POWER -> 32_000_000L;
            case WOOD_POWER -> 16_000_000L;
            case SANDSTONE_POWER -> 16_000_000L;
            case IRON_POWER -> 32_000_000L;
            case GOLD_POWER -> 128_000_000L;
            case DIAMOND_POWER, DIAMOND_WOOD_POWER -> 256_000_000L;
            default -> 0;
        };
    }
    public long powerResistancePerTick() {
        return switch (this) {
            case COBBLESTONE_POWER -> 62_500L;
            case STONE_POWER, QUARTZ_POWER -> 31_250L;
            case WOOD_POWER -> 7_812L;
            case SANDSTONE_POWER, IRON_POWER, GOLD_POWER -> 31_250L;
            case DIAMOND_POWER, DIAMOND_WOOD_POWER -> 31_250L;
            default -> 0;
        };
    }
    public boolean connectsPowerHandlers() { return carriesPower() && this != SANDSTONE_POWER; }
    public boolean carriesFluids() {
        return this == COBBLESTONE_FLUID || this == STONE_FLUID || this == QUARTZ_FLUID
            || this == WOOD_FLUID || this == GOLD_FLUID || this == SANDSTONE_FLUID || this == IRON_FLUID
            || this == CLAY_FLUID || this == VOID_FLUID || this == DIAMOND_FLUID
            || this == DIAMOND_WOOD_FLUID;
    }
    public boolean isWoodenFluidExtraction() {
        return this == WOOD_FLUID || this == DIAMOND_WOOD_FLUID;
    }
    private boolean isGeneralFluidConnector() {
        return this == GOLD_FLUID || this == SANDSTONE_FLUID || this == IRON_FLUID
            || this == CLAY_FLUID || this == VOID_FLUID || this == DIAMOND_FLUID;
    }
    public boolean connectsFluidHandlers() { return carriesFluids() && this != SANDSTONE_FLUID; }
    public int fluidTransferRate() {
        return switch (this) {
            case COBBLESTONE_FLUID -> 10;
            case STONE_FLUID -> 20;
            case QUARTZ_FLUID -> 40;
            case WOOD_FLUID -> 10;
            case GOLD_FLUID -> 80;
            case SANDSTONE_FLUID -> 20;
            case IRON_FLUID -> 40;
            case CLAY_FLUID -> 40;
            case VOID_FLUID -> 80;
            case DIAMOND_FLUID -> 80;
            case DIAMOND_WOOD_FLUID -> 80;
            default -> 0;
        };
    }
    public boolean connectsInventories() { return carriesItems() && this != SANDSTONE_ITEM; }
}
