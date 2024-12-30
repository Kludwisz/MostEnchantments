package enchantsources;

import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mcbiome.source.OverworldBiomeSource;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.math.DistanceMetric;
import com.seedfinding.mccore.util.math.NextLongReverser;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.util.pos.RPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.loot.LootContext;
import com.seedfinding.mcfeature.loot.MCLootTables;
import com.seedfinding.mcfeature.loot.item.Items;
import com.seedfinding.mcfeature.misc.SpawnPoint;
import com.seedfinding.mcfeature.structure.JunglePyramid;
import com.seedfinding.mcmath.util.Mth;
import com.seedfinding.mcseed.lcg.LCG;
import com.seedfinding.mcseed.rand.JRand;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class JungleTempleChest {
    private static final MCVersion VERSION = MCVersion.v1_16_1;
    private static final JunglePyramid TEMPLE = new JunglePyramid(VERSION);
    private static final int FEATURE_SALT = 40002;

    private final long lootSeed;

    public JungleTempleChest(long lootSeed) {
        this.lootSeed = lootSeed & Mth.MASK_48;
    }

    public void findWorldSeeds() {
        SeedReverser reverser = new SeedReverser(new CPos(-32, -32), new CPos(23, 23)) // checking all 4 regions near spawn
                .forVersion(VERSION).withFeatureSalt(FEATURE_SALT);

        List<Long> decoratorSeeds = NextLongReverser.getSeeds(lootSeed & Mth.MASK_48)
                .stream().map(iseed -> {
                    JRand rand = JRand.ofInternalSeed(iseed);
                    rand.advance(-1515);
                    long s1 = rand.getSeed() ^ LCG.JAVA.multiplier;
                    rand.advance(-13); // 11 + 2 from nextLong
                    long s2 = rand.getSeed() ^ LCG.JAVA.multiplier;
                    return List.of(s1, s2);
                })
                .flatMap(List::stream).toList();

        if (decoratorSeeds.isEmpty()) {
            System.err.println("JungleTempleChest.findWorldSeeds: Can't get decorator seeds that yield " + lootSeed);
            return;
        }

        for (long decoratorSeed : decoratorSeeds) {
            reverser.reverseDecoratorSeed(decoratorSeed).forEach(result -> {
                long seed = result.getFirst();
                CPos pos = result.getSecond();
                RPos rpos = pos.toRegionPos(TEMPLE.getSpacing());

                ChunkRand rand = new ChunkRand();
                CPos templePos = TEMPLE.getInRegion(seed, rpos.getX(), rpos.getZ(), rand);
                if (templePos == null) return;

                if (templePos.equals(pos)) {
                    // find first sister seed that generates the jungle temple
                    for (long upper = 0; upper < 1024; upper++) {
                        long worldseed = (upper << 48) | seed;

                        BiomeSource obs = BiomeSource.of(Dimension.OVERWORLD, VERSION, worldseed);
                        if (!TEMPLE.canSpawn(templePos.getX(), templePos.getZ(), obs)) continue;
                        CPos spawnPos = SpawnPoint.getApproximateSpawn((OverworldBiomeSource) obs).toChunkPos();
                        if (spawnPos.distanceTo(templePos, DistanceMetric.EUCLIDEAN) >= 6)  continue;

                        System.out.println(worldseed + " " + pos.toBlockPos(80));
                        break;
                    }
                }
            });
        }

    }

    public static List<Long> getValidLootSeedsFromFile(String inputFilename) {
        // get the resource at inputFilename
        URL resource = JungleTempleChest.class.getClassLoader().getResource(inputFilename);
        if (resource == null) {
            System.err.println("JungleTempleChest.getValidLootSeedsFromFile: File not found");
            return List.of();
        }
        File file = new File(resource.getFile());

        try (Scanner fin = new Scanner(file)) {
            ArrayList<Long> lootSeeds = new ArrayList<>();
            LootContext ctx = new LootContext(0L);

            while (fin.hasNextLong()) {
                long iseed = fin.nextLong();
                fin.skip("  ->  ");
                fin.nextInt();

                // go back a couple of calls and check if the chest will generate the book
                // you could get a lot more loot seeds by repeating this in a loop over
                // different rng offsets, -2 just covers the case when it's the first generated item
                ctx.setSeed(iseed, false);
                ctx.advance(-2); // step back item choice and loot pool count choice
                long lootSeed = ctx.getSeed() ^ LCG.JAVA.multiplier;

                Items.ENCHANTED_BOOK.getEnchantments().clear(); // temporary fix for a bug in the library
                MCLootTables.JUNGLE_TEMPLE_CHEST.get().generate(ctx).forEach(is -> {
                    if (is.getItem().getEnchantments().size() >= 11)
                        lootSeeds.add(lootSeed);
                });
            }

            return lootSeeds;
        }
        catch (Exception ex) {
            System.err.println("JungleTempleChest.getValidLootSeedsFromFile: " + ex.getMessage());
        }
        return List.of();
    }
}
