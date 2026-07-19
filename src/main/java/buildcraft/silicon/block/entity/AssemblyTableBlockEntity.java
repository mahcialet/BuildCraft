package buildcraft.silicon.block.entity;

import buildcraft.api.mj.ILaserTarget;
import buildcraft.silicon.BCSiliconBlockEntities;
import buildcraft.silicon.recipe.AssemblyRecipe;
import java.util.List;
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
    private static final List<AssemblyRecipe> RECIPES = AssemblyRecipe.chipsetRecipes();
    private final ItemStacksResourceHandler inventory = new ItemStacksResourceHandler(12);
    private long storedLaserPower;

    public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(BCSiliconBlockEntities.ASSEMBLY_TABLE.get(), pos, state);
    }
    public ItemStacksResourceHandler inventory() { return inventory; }
    public long storedLaserPower() { return storedLaserPower; }

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
        for (AssemblyRecipe recipe : RECIPES) if (findSlots(recipe) != null && canAccept(recipe.output())) return recipe;
        return null;
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
            if (inventory.insert(ItemResource.of(recipe.output()), recipe.output().getCount(), transaction)
                != recipe.output().getCount()) return false;
            transaction.commit();
            return true;
        }
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.deserialize(input.childOrEmpty("inventory"));
        storedLaserPower = Math.max(0, input.getLongOr("laser_power", 0));
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inventory.serialize(output.child("inventory"));
        if (storedLaserPower > 0) output.putLong("laser_power", storedLaserPower);
    }
}
