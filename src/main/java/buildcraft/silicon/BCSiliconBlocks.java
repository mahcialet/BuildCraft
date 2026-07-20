package buildcraft.silicon;

import buildcraft.silicon.block.AssemblyTableBlock;
import buildcraft.silicon.block.LaserBlock;
import buildcraft.silicon.block.AdvancedCraftingTableBlock;
import buildcraft.silicon.block.IntegrationTableBlock;
import buildcraft.silicon.block.ChargingTableBlock;
import buildcraft.silicon.block.ProgrammingTableBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BCSiliconBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BCSilicon.MOD_ID);
    public static final DeferredBlock<LaserBlock> LASER = BLOCKS.registerBlock("laser", LaserBlock::new);
    public static final DeferredBlock<AssemblyTableBlock> ASSEMBLY_TABLE =
        BLOCKS.registerBlock("assembly_table", AssemblyTableBlock::new);
    public static final DeferredBlock<AdvancedCraftingTableBlock> ADVANCED_CRAFTING_TABLE =
        BLOCKS.registerBlock("advanced_crafting_table", AdvancedCraftingTableBlock::new);
    public static final DeferredBlock<IntegrationTableBlock> INTEGRATION_TABLE =
        BLOCKS.registerBlock("integration_table", IntegrationTableBlock::new);
    public static final DeferredBlock<ChargingTableBlock> CHARGING_TABLE =
        BLOCKS.registerBlock("charging_table", ChargingTableBlock::new);
    public static final DeferredBlock<ProgrammingTableBlock> PROGRAMMING_TABLE =
        BLOCKS.registerBlock("programming_table", ProgrammingTableBlock::new);
    private BCSiliconBlocks() {}
    public static void register(IEventBus bus) { BLOCKS.register(bus); }
}
