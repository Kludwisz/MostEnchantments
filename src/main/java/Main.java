import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.loot.enchantment.Enchantments;
import enchantsources.EndCityChest;
import enchantsources.JungleTempleChest;
import enchantsources.StrongholdChests;

import java.util.List;

@SuppressWarnings("unused")
public class Main {
    /**
     * Here you can find all the methods that were used to generate the results,
     * as well as comments and references to the files where the results are stored.
     */
    public static void main(String[] args) {
        // EnchantFinder.findNEnchantSeeds(11);
        // EnchantFinder.findNEnchantSeeds(12);
        // results -> seeds_11.txt, seeds_12.txt

        // MaxSwordFinder.graphSharpnessFromEffectiveLevel();
        // results -> sharpness_levels.txt
        // MaxSwordFinder.printAERForLevel(45);
        // conclusion: can't get a maxed-out sharpness sword from any chest,
        // because we're always missing sweeping edge when level >= 45.

        // MaxSwordFinder.findMaxSwordSeeds();
        // results -> max_swords.txt

        // findEndCity();
        // results -> world_seeds.txt

        // findJungleTemple();
        // results -> world_seeds.txt

        // findStrongholdLootSeeds();
        // results -> stronghold_loot_seeds.txt

        // findStronghold();
        // results -> world_seeds.txt

        // jungleTempleDemo();
        // 60 -> bone, 85 -> golden horse armor, 46 -> bone
    }

    private static void findEndCity() {
        List.of(
                122582799509215L,   // bane of arthropods
                113671614640449L,   // smite
                31862010966927L     // sharpness
        )
        .forEach(lootSeed -> {
            EndCityChest endCityChest = new EndCityChest(lootSeed);
            endCityChest.findWorldSeeds();
        });
    }

    private static void findJungleTemple() {
        JungleTempleChest.getValidLootSeedsFromFile("seeds_11.txt")
        .forEach(lootSeed -> {
            JungleTempleChest jungleTempleChest = new JungleTempleChest(lootSeed);
            jungleTempleChest.findWorldSeeds();
        });
    }

    private static void findStrongholdLootSeeds() {
        List.of(
                StrongholdChests.LIBRARY_ONE_ITEM,
                StrongholdChests.CORRIDOR_ONE_ITEM,
                StrongholdChests.CROSSING_ONE_ITEM
        )
        .forEach(table -> {
            System.out.println("------------------------------------");
            StrongholdChests.getLootSeedsForLootTable("seeds_11.txt", table.get()).forEach(System.out::println);
        });
    }

    private static void findStronghold() {
        StrongholdChests.findCorridorChest(152519012265730L);
    }

    // ------------------------------------------------------------------------------------------

    private static void printEnchantmentOrders() {
        MCVersion[] versions = {
                MCVersion.v1_13,
                MCVersion.v1_14,
                MCVersion.v1_15,
                MCVersion.v1_16,
                MCVersion.v1_17,
                MCVersion.v1_18,
                MCVersion.v1_19,
                MCVersion.v1_20,
                MCVersion.v1_21
        };

        for (MCVersion version : versions) {
            System.out.println("------------------------------------");
            System.out.println(version);
            Enchantments.getFor(version).forEach(enchantment -> System.out.println(enchantment.getName()));
            System.out.println();
        }
    }

    private static void jungleTempleDemo() {
        long[] seeds12 = {
                256209145405483L,
                200022975719621L,
                230423166549507L
        };

        for (long seed : seeds12) {
            ChunkRand rand = new ChunkRand();
            rand.setSeed(seed, false);
            rand.advance(-1);
            System.out.println(seed + " " + rand.nextInt(88));
        }
    }
}
