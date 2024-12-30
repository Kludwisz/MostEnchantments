package enchantsources;

import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.math.NextLongReverser;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcmath.util.Mth;
import com.seedfinding.mcreversal.ChunkRandomReverser;
import com.seedfinding.mcseed.lcg.LCG;

import java.util.ArrayList;
import java.util.List;

public class SeedReverser {
    private final CPos chunkMin;
    private final CPos chunkMax;

    private int featureSalt = 0;
    private MCVersion version = MCVersion.v1_16_1;

    public SeedReverser(CPos chunkMin, CPos chunkMax) {
        this.chunkMin = chunkMin;
        this.chunkMax = chunkMax;
    }

    public SeedReverser withFeatureSalt(int featureSalt) {
        this.featureSalt = featureSalt;
        return this;
    }

    public SeedReverser forVersion(MCVersion version) {
        this.version = version;
        return this;
    }

    public List<Pair<Long, CPos>> reverseLootSeed(long lootSeed) {
        List<Long> internalSeeds = NextLongReverser.getSeeds(lootSeed & Mth.MASK_48);
        if (internalSeeds.isEmpty())
            return List.of();

        return internalSeeds.stream()
                .map(internalSeed -> reverseDecoratorSeed(internalSeed ^ LCG.JAVA.multiplier))
                .flatMap(List::stream).toList();
    }

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

    public static boolean canGetLootSeed(long lootSeed) {
        return !NextLongReverser.getSeeds(lootSeed & Mth.MASK_48).isEmpty();
    }
}
