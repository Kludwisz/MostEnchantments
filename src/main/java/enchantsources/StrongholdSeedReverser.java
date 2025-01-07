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
                new BPos(-2500, 0, -2500).toChunkPos(),
                new BPos(2500, 0, 2500).toChunkPos()
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
        final int increment = Math.max(targetResultCount / 100, 1);
        int nextPercent = increment;
        int percent = 0;

        for (int cx = chunkMin.getX(); cx <= chunkMax.getX(); cx++) {
            for (int cz = chunkMin.getZ(); cz <= chunkMax.getZ(); cz++) {
                final CPos chunkPos = new CPos(cx, cz);
                if (!isInRing(chunkPos))
                    continue;

                final int bx = cx << 4;
                final int bz = cz << 4;
                ChunkRandomReverser.reversePopulationSeed(popseed, bx, bz, version)
                        .stream().parallel().map(seed -> new Pair<>(seed, chunkPos))
                        .forEach(pair -> {
                            Stronghold sh = new Stronghold(version);
                            BiomeSource obs = BiomeSource.of(Dimension.OVERWORLD, version, pair.getFirst());
                            CPos[] strongholds = sh.getStarts(obs, 1, new JRand(0));

                            for (CPos stronghold : strongholds) {
                                //System.out.println("check" + stronghold);
                                if (stronghold.distanceTo(chunkPos, DistanceMetric.CHEBYSHEV) <= 7) {
                                    synchronized (results) {
                                        results.add(pair);
                                    }
                                }
                            }
                        });

                if (results.size() >= targetResultCount)
                    return results;

                if (results.size() >= nextPercent) {
                    percent++;
                    nextPercent += increment;
                    System.out.println("results: " + results.size() + " / " + targetResultCount + ", " + percent + "% done");
                }
            }
        }

        return results;
    }

    private boolean isInRing(CPos pos) {
        long x = pos.getX();
        long z = pos.getZ();
        long dist = x*x + z*z;
        final long a = 1400 / 16;
        final long b = 2600 / 16;
        return a * a < dist && dist < b * b;
    }
}
