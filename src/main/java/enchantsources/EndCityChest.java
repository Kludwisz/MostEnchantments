package enchantsources;

import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.util.pos.RPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.structure.EndCity;
import com.seedfinding.mcfeature.structure.generator.structure.EndCityGenerator;
import com.seedfinding.mcmath.util.Mth;
import com.seedfinding.mcterrain.TerrainGenerator;

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
        // -> reverse loot seed to decorator seeds, then to population seeds, then to world seeds
        // -> for each world seed, check if an end city generates a chest at the target chunk
        // -> repeat until a seed is found

        if (!SeedReverser.canGetLootSeed(lootSeed)) {
            System.err.println("EndCityChest.findWorldSeeds: Can't get loot seed from " + lootSeed);
            return;
        }

        SeedReverser reverser = new SeedReverser(new CPos(100, -40), new CPos(120, 40))
                .forVersion(VERSION).withFeatureSalt(FEATURE_SALT);

        reverser.reverseLootSeed(lootSeed).forEach(result -> {
            long seed = result.getFirst();
            CPos pos = result.getSecond();

            ChunkRand rand = new ChunkRand();
            rand.setDecoratorSeed(seed, pos.getX() << 4, pos.getZ() << 4, FEATURE_SALT, VERSION);
            long generatedLootSeed = rand.nextLong() & Mth.MASK_48;
            if (generatedLootSeed != lootSeed) {
                System.err.println("Reversal failed, check code!");
                return;
            }

            RPos rpos = pos.toRegionPos(END_CITY.getSpacing());
            CPos cityPos = END_CITY.getInRegion(seed, rpos.getX(), rpos.getZ(), rand);

            EndCityGenerator gen = new EndCityGenerator(VERSION);
            boolean generates = gen.generate(
                    TerrainGenerator.of(BiomeSource.of(Dimension.END, VERSION, seed)),
                    cityPos
            );

            if (!generates) return;

            var chests = gen.getChestsPos();
            var goodChest = chests
                    .stream().filter(pair -> pair.getSecond().toChunkPos().equals(pos))
                    .findFirst();

            goodChest.ifPresent(pair -> {
                BPos ch = pair.getSecond();
                System.out.println("LootSeed: " + lootSeed + "    Seed: " + seed + "  /tp " + ch.getX() + " " + ch.getY() + " " + ch.getZ());
            });
        });
    }
}
