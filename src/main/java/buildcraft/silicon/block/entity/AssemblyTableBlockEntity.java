package buildcraft.silicon.block.entity;

import buildcraft.api.mj.ILaserTarget;
import buildcraft.silicon.BCSiliconBlockEntities;
import buildcraft.silicon.recipe.AssemblyRecipe;
import buildcraft.silicon.recipe.AssemblyRecipeInput;
import buildcraft.silicon.recipe.AssemblySelection;
import buildcraft.silicon.ChipsetType;
import buildcraft.silicon.BCSiliconRecipes;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class AssemblyTableBlockEntity extends BlockEntity implements ILaserTarget {
    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(12);
    private long storedLaserPower;
    private AssemblySelection selection = AssemblySelection.RED;

    public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(BCSiliconBlockEntities.ASSEMBLY_TABLE.get(), pos, state);
    }
    public ItemStacksResourceHandler inventory() { return inventory; }
    public long storedLaserPower() { return storedLaserPower; }
    public ChipsetType selectedType() { return selection.chipset().orElse(ChipsetType.RED); }
    public AssemblySelection selection() { return selection; }
    public void setSelectedType(ChipsetType type) {
        setSelection(AssemblySelection.chipset(type));
    }
    public void setSelection(AssemblySelection selection) {
        if (selection != this.selection) {
            this.selection = selection;
            storedLaserPower = 0;
            setChanged();
        }
    }
    public long selectedRequiredPower() {
        AssemblyRecipe recipe = activeRecipe();
        return recipe == null ? 0 : recipe.requiredPower();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AssemblyTableBlockEntity table) {
        if (level.isClientSide()) return;
        AssemblyRecipe recipe = table.activeRecipe();
        if (recipe == null) {
            table.storedLaserPower = 0;
        } else if (table.storedLaserPower >= recipe.requiredPower()) {
            if (table.craft(recipe)) table.storedLaserPower = 0;
        }
        table.setChanged();
    }

    @Override public long getRequiredLaserPower() {
        AssemblyRecipe recipe = activeRecipe();
        return recipe == null ? 0 : Math.max(0, recipe.requiredPower() - storedLaserPower);
    }

    @Override public long receiveLaserPower(long microJoules) {
        long accepted = Math.min(Math.max(0, microJoules), getRequiredLaserPower());
        storedLaserPower += accepted;
        if (accepted > 0) setChanged();
        return microJoules - accepted;
    }

    private AssemblyRecipe activeRecipe() {
        if (level == null || level.isClientSide()) return null;
        var stacks = new ArrayList<ItemStack>(inventory.size());
        for (int slot = 0; slot < inventory.size(); slot++) stacks.add(stack(slot));
        AssemblyRecipeInput input = new AssemblyRecipeInput(stacks, selection);
        return level.getServer().getRecipeManager().getRecipeFor(
            BCSiliconRecipes.ASSEMBLY_TYPE.get(), input, level).map(holder -> holder.value())
            .filter(recipe -> canAccept(recipe.result())).orElse(null);
    }

    private int[] findSlots(AssemblyRecipe recipe) {
        int[] slots = new int[recipe.ingredients().size()];
        java.util.Arrays.fill(slots, -1);
        boolean[] used = new boolean[inventory.size()];
        for (int ingredientIndex = 0; ingredientIndex < recipe.ingredients().size(); ingredientIndex++) {
            for (int slot = 0; slot < inventory.size(); slot++) {
                if (!used[slot] && recipe.ingredients().get(ingredientIndex).test(stack(slot))) {
                    slots[ingredientIndex] = slot;
                    used[slot] = true;
                    break;
                }
            }
            if (slots[ingredientIndex] < 0) return null;
        }
        return slots;
    }

    private ItemStack stack(int slot) {
        ItemResource resource = inventory.getResource(slot);
        return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(inventory.getAmountAsInt(slot));
    }

    private boolean canAccept(ItemStack output) {
        try (Transaction transaction = Transaction.openRoot()) {
            return inventory.insert(ItemResource.of(output), output.getCount(), transaction) == output.getCount();
        }
    }

    private boolean craft(AssemblyRecipe recipe) {
        int[] slots = findSlots(recipe);
        if (slots == null) return false;
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot : slots) {
                ItemResource resource = inventory.getResource(slot);
                if (inventory.extract(slot, resource, 1, transaction) != 1) return false;
            }
            if (inventory.insert(ItemResource.of(recipe.result()), recipe.result().getCount(), transaction)
                != recipe.result().getCount()) return false;
            transaction.commit();
            return true;
        }
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input.childOrEmpty("inventory"));
        storedLaserPower = Math.max(0, input.getLongOr("laser_power", 0));
        selection = input.read("selection", AssemblySelection.CODEC)
                .orElseGet(() -> AssemblySelection.chipset(
                        input.read("selected_type", ChipsetType.CODEC).orElse(ChipsetType.RED)));
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
        if (storedLaserPower > 0) output.putLong("laser_power", storedLaserPower);
        output.store("selection", AssemblySelection.CODEC, selection);
    }
}
