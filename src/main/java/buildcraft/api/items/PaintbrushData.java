package buildcraft.api.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.DyeColor;

/** Color and remaining applications for a loaded BuildCraft paintbrush. */
public record PaintbrushData(DyeColor color, int usesLeft) {
    public static final int MAX_USES = 64;
    public static final Codec<PaintbrushData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DyeColor.CODEC.fieldOf("color").forGetter(PaintbrushData::color),
        Codec.intRange(1, MAX_USES).fieldOf("uses_left").forGetter(PaintbrushData::usesLeft)
    ).apply(instance, PaintbrushData::new));

    public PaintbrushData useOnce() {
        return usesLeft <= 1 ? null : new PaintbrushData(color, usesLeft - 1);
    }
}
