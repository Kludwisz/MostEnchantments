package enchantsources;

import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.math.NextLongReverser;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.util.pos.RPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.EndCity;
import com.seedfinding.mcfeature.structure.generator.structure.EndCityGenerator;
import com.seedfinding.mcmath.util.Mth;
import com.seedfinding.mcreversal.ChunkRandomReverser;
import com.seedfinding.mcseed.lcg.LCG;
import com.seedfinding.mcterrain.TerrainGenerator;

import java.util.List;

public class EndCityChest {
    private static final MCVersion VERSION = MCVersion.v1_16_1;
    private static final EndCity END_CITY = new EndCity(VERSION);
    private static final int FEATURE_SALT = END_CITY.getDecorationSalt();

    private final long lootSeed;

    public EndCityChest(long lootSeed) {
        this.lootSeed = lootSeed & Mth.MASK_48;
    }

    public void findWorldSeeds() {
        // the process goes like this:
        // -> reverse loot seed to decorator internalDecoratorSeeds, then to population internalDecoratorSeeds
        // -> reverse population internalDecoratorSeeds to world internalDecoratorSeeds
        // -> for each world seed, check if an end city generates a chest at the target chunk
        // -> repeat until a seed is found

        List<Long> internalDecoratorSeeds = NextLongReverser.getSeeds(lootSeed & Mth.MASK_48);
        if (internalDecoratorSeeds.isEmpty()) {
            System.err.println("EndCityChest.findWorldSeeds: No JRand state satisfies nextLong = " + lootSeed);
            return;
        }

        List<Long> decoratorSeeds = internalDecoratorSeeds.stream().map(ids -> ids ^ LCG.JAVA.multiplier).toList();

        List<Long> populationSeeds = decoratorSeeds.stream()
                .map(ds -> ChunkRandomReverser.reverseDecoratorSeed(ds, FEATURE_SALT % 10000, FEATURE_SALT / 10000, VERSION))
                .toList();

        for (long populationSeed : populationSeeds) {
            // reverse in some arbitrary region, not really important where that is
            for (int chunkX = 100; chunkX < 120; chunkX++) {
                for (int chunkZ = -40; chunkZ < 40; chunkZ++) {
                    final int bx = chunkX << 4;
                    final int bz = chunkZ << 4;
                    ChunkRandomReverser.reversePopulationSeed(populationSeed, bx, bz, VERSION)
                    .forEach(structureSeed -> {
                        ChunkRand rand = new ChunkRand();
                        rand.setDecoratorSeed(structureSeed, bx, bz, FEATURE_SALT, VERSION);
                        long generatedLootSeed = rand.nextLong() & Mth.MASK_48;
                        if (generatedLootSeed != lootSeed) {
                            System.err.println("Reversal failed, check code!");
                            return;
                        }

                        CPos chestChunkPos = new CPos(bx >> 4, bz >> 4);
                        RPos rpos = chestChunkPos.toRegionPos(END_CITY.getSpacing());
                        CPos cityPos = END_CITY.getInRegion(structureSeed, rpos.getX(), rpos.getZ(), rand);

                        EndCityGenerator gen = new EndCityGenerator(VERSION);
                        boolean generates = gen.generate(
                                TerrainGenerator.of(BiomeSource.of(Dimension.END, VERSION, structureSeed)),
                                cityPos
                        );

                        if (!generates) return;

                        var chests = gen.getChestsPos();
                        var goodChest = chests
                                .stream().filter(pair -> pair.getSecond().toChunkPos().equals(chestChunkPos))
                                .findFirst();

                        goodChest.ifPresent(pair -> {
                            BPos ch = pair.getSecond();
                            System.out.println("LootSeed: " + lootSeed + "    Seed: " + structureSeed + "  /tp " + ch.getX() + " " + ch.getY() + " " + ch.getZ());
                        });
                    });
                }
            }
        }
    }
}
