package enchantsources;

import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.math.NextLongReverser;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mccore.version.UnsupportedVersion;
import com.seedfinding.mcmath.util.Mth;
import com.seedfinding.mcreversal.ChunkRandomReverser;
import com.seedfinding.mcseed.lcg.LCG;

import java.util.ArrayList;
import java.util.List;

public class SeedReverser {
    protected final CPos chunkMin;
    protected final CPos chunkMax;

    protected int featureSalt = 0;
    protected MCVersion version = MCVersion.v1_16_1;

    /**
     * Creates a chunk-area-bounded SeedReverser.
     * @param chunkMin the area's minimum chunk position, inclusive
     * @param chunkMax the area's maximum chunk position, inclusive
     */
    public SeedReverser(CPos chunkMin, CPos chunkMax) {
        this.chunkMin = chunkMin;
        this.chunkMax = chunkMax;
    }

    /**
     * Specifies the feature salt (also known as the decoration salt) to be used in the reversal process.
     */
    public SeedReverser withFeatureSalt(int featureSalt) {
        this.featureSalt = featureSalt;
        return this;
    }

    /**
     * Specifies the version to be used in the reversal process.
     */
    public SeedReverser forVersion(MCVersion version) {
        if (version == null)
            return this;
        if (version.isNewerOrEqualTo(MCVersion.v1_18) || version.isOlderThan(MCVersion.v1_13))
            throw new UnsupportedVersion(version, "SeedReverser's methods");

        this.version = version;
        return this;
    }

    /**
     * Reverses a loot seed to a list of world seeds and chunk positions where the loot seed will be
     * generated for the first chest in the given chunk, assuming no previous random calls happened.
     * The resulting chunks are always within the boundaries specified in the constructor.
     * @param lootSeed the loot seed to reverse
     * @return a list of pairs, where the first element is the world seed and the second element is the chunk position
     */
    public List<Pair<Long, CPos>> reverseLootSeed(long lootSeed) {
        List<Long> internalSeeds = NextLongReverser.getSeeds(lootSeed & Mth.MASK_48);
        if (internalSeeds.isEmpty())
            return List.of();

        return internalSeeds.stream()
                .map(internalSeed -> reverseDecoratorSeed(internalSeed ^ LCG.JAVA.multiplier))
                .flatMap(List::stream).toList();
    }

    /**
     * Reverses a decorator seed to a list of world seeds and positions of chunks that have the given decorator,
     * within the boundaries specified in the constructor.
     * @param decoratorSeed the decorator seed
     * @return a list of pairs, where the first element is the world seed and the second element is the chunk position
     */
    public List<Pair<Long, CPos>> reverseDecoratorSeed(long decoratorSeed) {
        long popseed = ChunkRandomReverser.reverseDecoratorSeed(decoratorSeed, featureSalt % 10000, featureSalt / 10000, version);

        ArrayList<Pair<Long, CPos>> results = new ArrayList<>();

        for (int cx = chunkMin.getX(); cx <= chunkMax.getX(); cx++) {
            for (int cz = chunkMin.getZ(); cz <= chunkMax.getZ(); cz++) {
                final CPos chunkPos = new CPos(cx, cz);
                final int bx = cx << 4;
                final int bz = cz << 4;
                ChunkRandomReverser.reversePopulationSeed(popseed, bx, bz, version)
                        .stream().map(seed -> new Pair<>(seed, chunkPos))
                        .forEach(results::add);
            }
        }

        return results;
    }

    /**
     * Returns whether it is possible or not for a chest with the given loot seed to generate.
     */
    public static boolean canGetLootSeed(long lootSeed) {
        return !NextLongReverser.getSeeds(lootSeed & Mth.MASK_48).isEmpty();
    }
}
