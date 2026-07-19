package buildcraft.transport;

import net.minecraft.util.ARGB;

/** The four independent signal networks that can be installed on a transport pipe. */
public enum PipeWireColor {
    RED(0xD03B3B),
    BLUE(0x3C44AA),
    GREEN(0x5E9C36),
    YELLOW(0xFED83D);

    private final int rgb;

    PipeWireColor(int rgb) {
        this.rgb = rgb;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public int argb(boolean powered) {
        int value = powered ? rgb : ARGB.scaleRGB(rgb, 0.35F);
        return ARGB.color(255, value);
    }
}
