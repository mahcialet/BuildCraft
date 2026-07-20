package buildcraft.energy.gen;

/** The public-domain 2D simplex implementation and fixed permutation used by BuildCraft 8 oil biomes. */
final class LegacySimplexNoise {
    private static final int[][] GRADIENTS = {
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1}, {1, 0}, {-1, 0},
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {0, 1}, {0, -1}
    };
    private static final short[] P = {
            151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,140,36,103,30,69,142,8,99,
            37,240,21,10,23,190,6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,
            57,177,33,88,237,149,56,87,174,20,125,136,171,168,68,175,74,165,71,134,139,48,27,
            166,77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
            102,143,54,65,25,63,161,1,216,80,73,209,76,132,187,208,89,18,169,200,196,135,130,
            116,188,159,86,164,100,109,198,173,186,3,64,52,217,226,250,124,123,5,202,38,147,
            118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,223,183,170,213,
            119,248,152,2,44,154,163,70,221,153,101,155,167,43,172,9,129,22,39,253,19,98,108,
            110,79,113,224,232,178,185,112,104,218,246,97,228,251,34,242,193,238,210,144,12,
            191,179,162,241,81,51,145,235,249,14,239,107,49,192,214,31,181,199,106,157,184,
            84,204,176,115,121,50,45,127,4,150,254,138,236,205,93,222,114,67,29,24,72,243,
            141,128,195,78,66,215,61,156,180
    };
    private static final short[] PERM = new short[512];
    private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
    private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;

    static {
        for (int index = 0; index < PERM.length; index++) PERM[index] = P[index & 255];
    }

    private LegacySimplexNoise() {
    }

    static double noise(double xInput, double zInput) {
        double skew = (xInput + zInput) * F2;
        int i = floor(xInput + skew), j = floor(zInput + skew);
        double unskew = (i + j) * G2;
        double x0 = xInput - (i - unskew), z0 = zInput - (j - unskew);
        int i1 = x0 > z0 ? 1 : 0, j1 = x0 > z0 ? 0 : 1;
        double x1 = x0 - i1 + G2, z1 = z0 - j1 + G2;
        double x2 = x0 - 1 + 2 * G2, z2 = z0 - 1 + 2 * G2;
        int ii = i & 255, jj = j & 255;
        int g0 = PERM[ii + PERM[jj]] % 12;
        int g1 = PERM[ii + i1 + PERM[jj + j1]] % 12;
        int g2 = PERM[ii + 1 + PERM[jj + 1]] % 12;
        return 70.0 * (contribution(g0, x0, z0) + contribution(g1, x1, z1)
                + contribution(g2, x2, z2));
    }

    private static double contribution(int gradient, double x, double z) {
        double attenuation = 0.5 - x * x - z * z;
        if (attenuation < 0) return 0;
        attenuation *= attenuation;
        return attenuation * attenuation * (GRADIENTS[gradient][0] * x + GRADIENTS[gradient][1] * z);
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }
}
