package enchantsources;

import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.math.DistanceMetric;
import com.seedfinding.mccore.util.math.NextLongReverser;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mcfeature.structure.Stronghold;
import com.seedfinding.mcmath.util.Mth;
import com.seedfinding.mcreversal.ChunkRandomReverser;
import com.seedfinding.mcseed.lcg.LCG;
import com.seedfinding.mcseed.rand.JRand;

import java.util.ArrayList;
import java.util.List;

public class StrongholdSeedReverser extends SeedReverser {
    private int targetResultCount = Integer.MAX_VALUE;

    private StrongholdSeedReverser(CPos chunkMin, CPos chunkMax) {
        super(chunkMin, chunkMax);
    }

    public StrongholdSeedReverser() {
        this(
                new BPos(-2800, 0, -2800).toChunkPos(),
                new BPos(2800, 0, 2800).toChunkPos()
        );
    }

    public StrongholdSeedReverser withTargetResultCount(int targetResultCount) {
        this.targetResultCount = targetResultCount;
        return this;
    }

    public StrongholdSeedReverser withFeatureSalt(int featureSalt) {
        this.featureSalt = featureSalt;
        return this;
    }

    public List<Pair<Long, CPos>> reverseCorridorLootSeed(long lootSeed) {
        if (!canGetLootSeed(lootSeed)) {
            return List.of();
        }

        // reverse loot seed to internal seed(s)
        List<Long> iseeds = NextLongReverser.getSeeds(lootSeed & Mth.MASK_48);

        // reverse each internal seed back to a decorator seed, using the most likely rng offset
        // of a stronghold corridor chest.

        List<Long> decoratorSeeds = iseeds.stream().map(iseed -> {
            ChunkRand rand = new ChunkRand();
            rand.setSeed(iseed, false);
            rand.advance(-130); // 130 is the most likely offset
            return rand.getSeed() ^ LCG.JAVA.multiplier; // undo scrambling
        }).toList();

        return decoratorSeeds.stream()
                .map(this::reverseDecoratorSeed)
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public List<Pair<Long, CPos>> reverseDecoratorSeed(long decoratorSeed) {
        long popseed = ChunkRandomReverser.reverseDecoratorSeed(decoratorSeed, featureSalt % 10000, featureSalt / 10000, version);

        ArrayList<Pair<Long, CPos>> results = new ArrayList<>();

        for (int cx = chunkMin.getX(); cx <= chunkMax.getX(); cx++) {
            for (int cz = chunkMin.getZ(); cz <= chunkMax.getZ(); cz++) {
                final CPos chunkPos = new CPos(cx, cz);
                if (!isInRing(chunkPos))
                    continue;

                final int bx = cx << 4;
                final int bz = cz << 4;
                ChunkRandomReverser.reversePopulationSeed(popseed, bx, bz, version)
                        .stream().map(seed -> new Pair<>(seed, chunkPos))
                        .forEach(pair -> {
                            Stronghold sh = new Stronghold(version);
                            BiomeSource obs = BiomeSource.of(Dimension.OVERWORLD, version, pair.getFirst());
                            CPos[] strongholds = sh.getStarts(obs, 3, new JRand(0));

                            for (CPos stronghold : strongholds) {
                                if (stronghold.distanceTo(chunkPos, DistanceMetric.CHEBYSHEV) <= 5) {
                                    results.add(pair);
                                    break;
                                }
                            }
                        });

                if (results.size() >= targetResultCount)
                    return results;
            }
        }

        return results;
    }

    private boolean isInRing(CPos pos) {
        long x = pos.getX();
        long z = pos.getZ();
        long dist = x*x + z*z;
        final long a = 1300 / 16;
        final long b = 2800 / 16;
        return a * a < dist && dist < b * b;
    }
}
