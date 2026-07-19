package buildcraft.silicon.block.entity;

import buildcraft.api.mj.ILaserTarget;
import buildcraft.api.mj.MjAPI;
import buildcraft.silicon.BCSiliconBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class AdvancedCraftingTableBlockEntity extends BlockEntity implements ILaserTarget {
    public static final long POWER_REQUIRED = 500 * MjAPI.MJ;
    private final ItemStacksResourceHandler blueprint = new ItemStacksResourceHandler(9);
    private final ItemStacksResourceHandler materials = new ItemStacksResourceHandler(15);
    private final ItemStacksResourceHandler results = new ItemStacksResourceHandler(9);
    private final ResourceHandler<ItemResource> externalItems = new ExternalItems();
    private long storedLaserPower;

    public AdvancedCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(BCSiliconBlockEntities.ADVANCED_CRAFTING_TABLE.get(), pos, state);
    }
    public ItemStacksResourceHandler blueprint() { return blueprint; }
    public ItemStacksResourceHandler materials() { return materials; }
    public ItemStacksResourceHandler results() { return results; }
    public ResourceHandler<ItemResource> itemHandler() { return externalItems; }
    public long storedLaserPower() { return storedLaserPower; }

    public static void tick(Level level, BlockPos pos, BlockState state, AdvancedCraftingTableBlockEntity table) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        Craft craft = table.findCraft(serverLevel);
        if (craft != null && table.storedLaserPower >= POWER_REQUIRED && table.craft(craft)) {
            table.storedLaserPower -= POWER_REQUIRED;
        }
        table.setChanged();
    }
    @Override public long getRequiredLaserPower() {
        return level instanceof ServerLevel serverLevel && findCraft(serverLevel) != null
            ? Math.max(0, POWER_REQUIRED - storedLaserPower) : 0;
    }
    @Override public long receiveLaserPower(long microJoules) {
        long accepted = Math.min(Math.max(0, microJoules), getRequiredLaserPower());
        storedLaserPower += accepted;
        if (accepted > 0) setChanged();
        return microJoules - accepted;
    }

    private Craft findCraft(ServerLevel level) {
        List<ItemStack> pattern = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            ItemResource resource = blueprint.getResource(slot);
            pattern.add(resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(1));
        }
        CraftingInput input = CraftingInput.of(3, 3, pattern);
        RecipeHolder<CraftingRecipe> recipe = level.getServer().getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, input, level).orElse(null);
        if (recipe == null) return null;
        ItemStack output = recipe.value().assemble(input);
        if (output.isEmpty()) return null;
        int[] materialSlots = new int[9];
        java.util.Arrays.fill(materialSlots, -1);
        int[] reserved = new int[materials.size()];
        for (int patternSlot = 0; patternSlot < 9; patternSlot++) {
            ItemStack required = pattern.get(patternSlot);
            if (required.isEmpty()) continue;
            ItemResource wanted = ItemResource.of(required);
            for (int materialSlot = 0; materialSlot < materials.size(); materialSlot++) {
                if (materials.getResource(materialSlot).equals(wanted)
                    && materials.getAmountAsInt(materialSlot) > reserved[materialSlot]) {
                    materialSlots[patternSlot] = materialSlot;
                    reserved[materialSlot]++;
                    break;
                }
            }
            if (materialSlots[patternSlot] < 0) return null;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            if (results.insert(ItemResource.of(output), output.getCount(), transaction) != output.getCount()) return null;
        }
        return new Craft(recipe, input, output, materialSlots, pattern);
    }

    private boolean craft(Craft craft) {
        try (Transaction transaction = Transaction.openRoot()) {
            if (results.insert(ItemResource.of(craft.output), craft.output.getCount(), transaction)
                != craft.output.getCount()) return false;
            for (int patternSlot = 0; patternSlot < 9; patternSlot++) {
                int materialSlot = craft.materialSlots[patternSlot];
                if (materialSlot < 0) continue;
                ItemResource resource = ItemResource.of(craft.pattern.get(patternSlot));
                if (materials.extract(materialSlot, resource, 1, transaction) != 1) return false;
            }
            NonNullList<ItemStack> remainders = craft.recipe.value().getRemainingItems(craft.input);
            for (ItemStack remainder : remainders) {
                if (!remainder.isEmpty()
                    && materials.insert(ItemResource.of(remainder), remainder.getCount(), transaction)
                    != remainder.getCount()) return false;
            }
            transaction.commit();
            return true;
        }
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        blueprint.deserialize(input.childOrEmpty("blueprint"));
        materials.deserialize(input.childOrEmpty("materials"));
        results.deserialize(input.childOrEmpty("results"));
        storedLaserPower = Math.clamp(input.getLongOr("laser_power", 0), 0, POWER_REQUIRED);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        blueprint.serialize(output.child("blueprint"));
        materials.serialize(output.child("materials"));
        results.serialize(output.child("results"));
        if (storedLaserPower > 0) output.putLong("laser_power", storedLaserPower);
    }
    private record Craft(RecipeHolder<CraftingRecipe> recipe, CraftingInput input, ItemStack output,
                         int[] materialSlots, List<ItemStack> pattern) {}

    private final class ExternalItems implements ResourceHandler<ItemResource> {
        @Override public int size() { return materials.size() + results.size(); }
        @Override public ItemResource getResource(int index) {
            return index < materials.size() ? materials.getResource(index) : results.getResource(index - materials.size());
        }
        @Override public long getAmountAsLong(int index) {
            return index < materials.size() ? materials.getAmountAsLong(index) : results.getAmountAsLong(index - materials.size());
        }
        @Override public long getCapacityAsLong(int index, ItemResource resource) {
            return index < materials.size() ? materials.getCapacityAsLong(index, resource) : 0;
        }
        @Override public boolean isValid(int index, ItemResource resource) {
            if (index >= materials.size() || resource.isEmpty()) return false;
            for (int slot = 0; slot < blueprint.size(); slot++) if (blueprint.getResource(slot).equals(resource)) return true;
            return false;
        }
        @Override public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return isValid(index, resource) ? materials.insert(index, resource, amount, transaction) : 0;
        }
        @Override public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return index < materials.size() ? 0 : results.extract(index - materials.size(), resource, amount, transaction);
        }
    }
}
