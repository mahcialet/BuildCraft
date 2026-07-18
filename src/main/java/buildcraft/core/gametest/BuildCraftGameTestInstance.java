package buildcraft.core.gametest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/** A serializable GameTest instance that delegates to an in-code test body. */
public final class BuildCraftGameTestInstance extends GameTestInstance {
    public static final MapCodec<BuildCraftGameTestInstance> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            TestData.CODEC.fieldOf("data").forGetter(BuildCraftGameTestInstance::info),
            Identifier.CODEC.fieldOf("test").forGetter(test -> test.testId)
        ).apply(instance, BuildCraftGameTestInstance::new)
    );

    private static final Map<Identifier, Consumer<GameTestHelper>> TESTS = new HashMap<>();

    private final Identifier testId;

    public BuildCraftGameTestInstance(
        TestData<Holder<TestEnvironmentDefinition<?>>> data,
        Identifier testId
    ) {
        super(data);
        this.testId = testId;
    }

    public static BuildCraftGameTestInstance create(
        Identifier id,
        TestData<Holder<TestEnvironmentDefinition<?>>> data,
        Consumer<GameTestHelper> test
    ) {
        if (TESTS.putIfAbsent(id, test) != null) {
            throw new IllegalArgumentException("Duplicate BuildCraft GameTest " + id);
        }
        return new BuildCraftGameTestInstance(data, id);
    }

    @Override
    public void run(GameTestHelper helper) {
        Consumer<GameTestHelper> test = TESTS.get(testId);
        if (test == null) {
            throw new IllegalStateException("Missing BuildCraft GameTest body " + testId);
        }
        test.accept(helper);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("BuildCraft code test");
    }
}
